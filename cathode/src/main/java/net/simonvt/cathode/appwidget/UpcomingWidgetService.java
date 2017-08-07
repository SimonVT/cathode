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
package net.simonvt.cathode.appwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.squareup.picasso.Picasso;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.util.SqlColumn;
import timber.log.Timber;

public class UpcomingWidgetService extends RemoteViewsService {

  private static final int LOADER_UPCOMING = 1;

  @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new UpcomingRemoteViewsFactory(this.getApplicationContext(), intent);
  }

  public static class UpcomingRemoteViewsFactory
      implements RemoteViewsFactory, Loader.OnLoadCompleteListener<SimpleCursor> {

    @Inject Picasso picasso;

    private Context context;

    private AppWidgetManager widgetManager;
    private int appWidgetId;

    private SimpleCursorLoader cursorLoader;

    private ItemModel items;

    public UpcomingRemoteViewsFactory(Context context, Intent intent) {
      this.context = context;

      Injector.inject(this);

      appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
          AppWidgetManager.INVALID_APPWIDGET_ID);

      widgetManager = AppWidgetManager.getInstance(this.context);
    }

    @Override public void onCreate() {
      final long currentTime = System.currentTimeMillis();
      final long upcomingTime =
          currentTime + UpcomingTimePreference.getInstance().get().getCacheTime();

      cursorLoader =
          new SimpleCursorLoader(context, Episodes.EPISODES_WITH_SHOW, ItemModel.PROJECTION, "("
              + SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT)
              + ">0 OR "
              + SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST)
              + "=1) AND "
              + SqlColumn.table(Tables.SHOWS).column(ShowColumns.HIDDEN_CALENDAR)
              + "=0 AND ("
              + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
              + ">? AND "
              + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
              + "<?)", new String[] {
              String.valueOf(currentTime - DateUtils.HOUR_IN_MILLIS), String.valueOf(upcomingTime)
          }, Shows.SORT_NEXT_EPISODE);

      cursorLoader.registerListener(LOADER_UPCOMING, this);
      cursorLoader.startLoading();
    }

    @Override public void onLoadComplete(Loader loader, SimpleCursor data) {
      if (data == null) {
        return;
      }

      Timber.d("Load completed: %d", data.getCount());
      items = ItemModel.fromCursor(context, data);
      widgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);

      final long nextUpdateTime = items.getNextUpdateTime();

      DateFormat df = SimpleDateFormat.getDateTimeInstance();
      Timber.d("Next update: %s", df.format(new Date(nextUpdateTime)));

      final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

      Intent intent = new Intent(context, UpcomingWidgetProvider.class);
      intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      int[] ids = {
          appWidgetId,
      };
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

      PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
      am.cancel(pi);
      am.set(AlarmManager.RTC, nextUpdateTime, pi);
    }

    @Override public void onDataSetChanged() {
    }

    @Override public void onDestroy() {
      cursorLoader.stopLoading();
    }

    @Override public int getCount() {
      if (items == null) {
        return 1;
      }

      final int itemCount = items.getItemCount();
      if (itemCount == 0) {
        return 1;
      }

      return itemCount;
    }

    @Override public RemoteViews getViewAt(int position) {
      RemoteViews rv;

      if (items == null) {
        rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_loading);
      } else if (items.getItemCount() == 0) {
        rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_upcoming_row_empty);
      } else if (items.getItemType(position) == WidgetItem.TYPE_DAY) {
        DayInfo dayInfo = items.getDay(position);
        rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_upcoming_row_header);
        rv.setTextViewText(R.id.title, dayInfo.getDayLabel());
      } else {
        ItemInfo itemInfo = items.getItem(position);
        final String showPosterUri =
            ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, itemInfo.showId);

        rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_upcoming_row);

        rv.setTextViewText(R.id.title, itemInfo.getShowTitle());
        rv.setTextViewText(R.id.nextEpisode, itemInfo.getEpisodeTitle());
        rv.setTextViewText(R.id.firstAired, itemInfo.getAirTime());

        Bitmap poster = null;
        try {
          poster = picasso.load(showPosterUri)
              .resizeDimen(R.dimen.appWidgetPosterWidth, R.dimen.appWidgetPosterHeight)
              .centerCrop()
              .get();
        } catch (Throwable t) {
          // Ignore
        }

        rv.setImageViewBitmap(R.id.poster, poster);

        Bundle extras = new Bundle();
        extras.putLong(EpisodeDetailsActivity.EXTRA_ID, itemInfo.getEpisodeId());
        extras.putLong(EpisodeDetailsActivity.EXTRA_SHOW_ID, itemInfo.getShowId());
        extras.putString(EpisodeDetailsActivity.EXTRA_SHOW_TITLE, itemInfo.getShowTitle());
        extras.putString(EpisodeDetailsActivity.EXTRA_SHOW_OVERVIEW, itemInfo.getShowOverview());

        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.row, fillInIntent);
      }

      return rv;
    }

    @Override public RemoteViews getLoadingView() {
      return null;
    }

    @Override public int getViewTypeCount() {
      return 4;
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean hasStableIds() {
      return true;
    }
  }
}
