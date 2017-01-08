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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.squareup.picasso.Picasso;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class UpcomingWidgetService extends RemoteViewsService {

  private static final int LOADER_UPCOMING = 1;

  @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return new UpcomingRemoteViewsFactory(this.getApplicationContext(), intent);
  }

  public static class UpcomingRemoteViewsFactory
      implements RemoteViewsFactory, Loader.OnLoadCompleteListener<SimpleCursor> {

    private static final String COLUMN_EPISODE_ID = "episodeId";

    @Inject Picasso picasso;

    private Context context;

    private AppWidgetManager widgetManager;
    private int appWidgetId;

    private SimpleCursorLoader cursorLoader;
    private Cursor cursor;

    public static final String[] PROJECTION = new String[] {
        SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID),
        SqlColumn.table(Tables.SHOWS).column(ShowColumns.RUNTIME),
        SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
        SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.ID) + " AS episodeId",
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
        SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.NOTIFICATION_DISMISSED),
    };

    public UpcomingRemoteViewsFactory(Context context, Intent intent) {
      this.context = context;

      CathodeApp.inject(context, this);

      appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
          AppWidgetManager.INVALID_APPWIDGET_ID);

      widgetManager = AppWidgetManager.getInstance(this.context);
    }

    @Override public void onCreate() {
      final long currentTime = System.currentTimeMillis();
      final long upcomingTime =
          currentTime + UpcomingTimePreference.getInstance().get().getCacheTime();

      cursorLoader = new SimpleCursorLoader(context, Episodes.EPISODES_WITH_SHOW, PROJECTION, "("
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT)
          + ">0 OR "
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST)
          + "=1) AND "
          + SqlColumn.table(Tables.SHOWS).column(ShowColumns.HIDDEN_CALENDAR)
          + "=0 AND ("
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
          + ">? AND "
          + SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED)
          + "<?)",
          new String[] {
          String.valueOf(currentTime - DateUtils.HOUR_IN_MILLIS), String.valueOf(upcomingTime)
      }, ProviderSchematic.Shows.SORT_NEXT_EPISODE);

      cursorLoader.registerListener(LOADER_UPCOMING, this);
      cursorLoader.startLoading();
    }

    @Override public void onLoadComplete(Loader loader, SimpleCursor data) {
      Timber.d("Load completed: %d", data.getCount());
      cursor = data;
      widgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
    }

    @Override public void onDataSetChanged() {
    }

    @Override public void onDestroy() {
      if (cursor != null) cursor.close();
      cursorLoader.stopLoading();
    }

    @Override public int getCount() {
      return cursor != null ? cursor.getCount() : 0;
    }

    @Override public RemoteViews getViewAt(int position) {
      cursor.moveToPosition(position);

      final long showId = Cursors.getLong(cursor, ShowColumns.ID);
      final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
      final String showOverview = Cursors.getString(cursor, ShowColumns.OVERVIEW);

      final long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
      final int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
      final long firstAired = DataHelper.getFirstAired(cursor);

      final String episodeTitle = DataHelper.getEpisodeTitle(context, cursor, season, episode);
      final String timeStamp = DateUtils.millisToString(context, firstAired, false);

      final String showPosterUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, showId);

      RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_upcoming_row);

      rv.setTextViewText(R.id.title, showTitle);
      rv.setTextViewText(R.id.nextEpisode, episodeTitle);
      rv.setTextViewText(R.id.firstAired, timeStamp);

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
      extras.putLong(EpisodeDetailsActivity.EXTRA_ID, episodeId);
      extras.putLong(EpisodeDetailsActivity.EXTRA_SHOW_ID, showId);
      extras.putString(EpisodeDetailsActivity.EXTRA_SHOW_TITLE, showTitle);
      extras.putString(EpisodeDetailsActivity.EXTRA_SHOW_OVERVIEW, showOverview);

      Intent fillInIntent = new Intent();
      fillInIntent.putExtras(extras);
      rv.setOnClickFillInIntent(R.id.row, fillInIntent);

      return rv;
    }

    @Override public RemoteViews getLoadingView() {
      return null;
    }

    @Override public int getViewTypeCount() {
      return 1;
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean hasStableIds() {
      return true;
    }
  }
}
