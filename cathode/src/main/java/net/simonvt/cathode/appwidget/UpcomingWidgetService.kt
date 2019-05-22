/*
 * Copyright (C) 2016 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.appwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.squareup.picasso.Picasso
import dagger.android.AndroidInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.util.SqlColumn
import net.simonvt.cathode.settings.UpcomingTimePreference
import net.simonvt.cathode.ui.EpisodeDetailsActivity
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class UpcomingWidgetService : RemoteViewsService() {

  @Inject
  lateinit var picasso: Picasso

  override fun onCreate() {
    super.onCreate()
    AndroidInjection.inject(this)
  }

  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
    return UpcomingRemoteViewsFactory(this.applicationContext, picasso, intent)
  }

  class UpcomingRemoteViewsFactory(
    private val context: Context,
    private val picasso: Picasso,
    intent: Intent
  ) : RemoteViewsFactory {

    private val widgetManager = AppWidgetManager.getInstance(this.context)
    private val appWidgetId = intent.getIntExtra(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var upcomingEpisodes: LiveData<List<ShowWithEpisode>>? = null

    private var items: ItemModel? = null

    private val upcomingObserver = Observer<List<ShowWithEpisode>> { showsWithEpisode ->
      val nextUpdateTime: Long
      if (showsWithEpisode != null) {
        Timber.d("Load completed: %d", showsWithEpisode.size)
        items = ItemModel.fromItems(context, showsWithEpisode)
        widgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list)
        nextUpdateTime = items!!.nextUpdateTime
      } else {
        nextUpdateTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS
      }

      val df = SimpleDateFormat.getDateTimeInstance()
      Timber.d("Next update: %s", df.format(Date(nextUpdateTime)))

      val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

      val updateIntent = Intent(context, UpcomingWidgetProvider::class.java)
      updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      val ids = intArrayOf(appWidgetId)
      updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

      val pi = PendingIntent.getBroadcast(context, 0, updateIntent, 0)
      am.cancel(pi)
      am.set(AlarmManager.RTC, nextUpdateTime, pi)
    }

    override fun onCreate() {
      val currentTime = System.currentTimeMillis()
      val upcomingTime = currentTime + UpcomingTimePreference.getInstance().get().cacheTime

      upcomingEpisodes = MappedCursorLiveData<List<ShowWithEpisode>>(
        context,
        Episodes.EPISODES_WITH_SHOW,
        ShowWithEpisodeMapper.projection,
        "(" + SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT) + ">0 OR " +
            SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST) + "=1) AND " +
            SqlColumn.table(Tables.SHOWS).column(ShowColumns.HIDDEN_CALENDAR) + "=0 AND (" +
            SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED) + ">? AND " +
            SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED) + "<?)",
        arrayOf((currentTime - DateUtils.HOUR_IN_MILLIS).toString(), upcomingTime.toString()),
        Shows.SORT_NEXT_EPISODE,
        ShowWithEpisodeListMapper
      )

      upcomingEpisodes!!.observeForever(upcomingObserver)
    }

    override fun onDataSetChanged() {}

    override fun onDestroy() {
      MainHandler.post { upcomingEpisodes!!.removeObserver(upcomingObserver) }
    }

    override fun getCount(): Int {
      if (items == null) {
        return 1
      }

      val itemCount = items!!.itemCount
      return if (itemCount == 0) 1 else itemCount
    }

    override fun getViewAt(position: Int): RemoteViews {
      val rv: RemoteViews

      if (items == null) {
        rv = RemoteViews(context.packageName, R.layout.appwidget_loading)
      } else if (items!!.itemCount == 0) {
        rv = RemoteViews(context.packageName, R.layout.appwidget_upcoming_row_empty)
      } else if (items!!.getItemType(position) == WidgetItem.TYPE_DAY) {
        val dayInfo = items!!.getDay(position)
        rv = RemoteViews(context.packageName, R.layout.appwidget_upcoming_row_header)
        rv.setTextViewText(R.id.title, dayInfo.getDayLabel())
      } else {
        val itemInfo = items!!.getItem(position)
        val showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, itemInfo.showId)

        rv = RemoteViews(context.packageName, R.layout.appwidget_upcoming_row)

        rv.setTextViewText(R.id.title, itemInfo.getShowTitle())
        rv.setTextViewText(R.id.nextEpisode, itemInfo.getEpisodeTitle())
        rv.setTextViewText(R.id.firstAired, itemInfo.getAirTime())

        var poster: Bitmap? = null
        try {
          poster = picasso.load(showPosterUri)
            .resizeDimen(R.dimen.appWidgetPosterWidth, R.dimen.appWidgetPosterHeight)
            .centerCrop()
            .get()
        } catch (t: Throwable) {
          // Ignore
        }

        rv.setImageViewBitmap(R.id.poster, poster)

        val extras = Bundle()
        extras.putLong(EpisodeDetailsActivity.EXTRA_ID, itemInfo.getEpisodeId())

        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        rv.setOnClickFillInIntent(R.id.row, fillInIntent)
      }

      return rv
    }

    override fun getLoadingView(): RemoteViews? {
      return null
    }

    override fun getViewTypeCount(): Int {
      return 4
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    override fun hasStableIds(): Boolean {
      return true
    }
  }
}
