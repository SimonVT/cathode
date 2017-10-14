/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.show;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.database.BaseAsyncLoader;
import net.simonvt.cathode.provider.database.DatabaseUtils;
import net.simonvt.cathode.provider.database.SimpleMergeCursor;
import net.simonvt.schematic.Cursors;

public class WatchedLoader extends BaseAsyncLoader<SimpleMergeCursor> {

  private long showId;

  private String[] projection;

  public WatchedLoader(Context context, long showId, String[] projection) {
    super(context);
    this.showId = showId;
    this.projection = projection;
  }

  @Override public SimpleMergeCursor loadInBackground() {
    clearNotificationUris();

    Cursor show = getContext().getContentResolver().query(Shows.withId(showId), new String[] {
        ShowColumns.WATCHING,
    }, null, null, null);
    show.moveToFirst();
    final boolean watching = Cursors.getBoolean(show, ShowColumns.WATCHING);
    show.close();

    Cursor lastWatched = null;
    if (!watching) {
      lastWatched = getContext().getContentResolver()
          .query(Episodes.fromShow(showId), projection, EpisodeColumns.WATCHED + "=1", null,
              EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1");
    }

    Cursor toWatch;
    if (watching) {
      toWatch = getContext().getContentResolver()
          .query(Episodes.fromShow(showId), projection,
              EpisodeColumns.WATCHING + "=1 OR " + EpisodeColumns.CHECKED_IN + "=1", null, null);
    } else {
      long lastWatchedSeason = 0;
      long lastWatchedEpisode = -1;
      if (lastWatched.moveToFirst()) {
        lastWatchedSeason = Cursors.getInt(lastWatched, EpisodeColumns.SEASON);
        lastWatchedEpisode = Cursors.getInt(lastWatched, EpisodeColumns.EPISODE);
      }

      toWatch = getContext().getContentResolver()
          .query(Episodes.fromShow(showId), projection, EpisodeColumns.WATCHED
                  + "=0 AND "
                  + EpisodeColumns.FIRST_AIRED
                  + " IS NOT NULL"
                  + " AND "
                  + EpisodeColumns.SEASON
                  + ">0"
                  + " AND ("
                  + EpisodeColumns.SEASON
                  + ">"
                  + lastWatchedSeason
                  + " OR ("
                  + EpisodeColumns.SEASON
                  + "="
                  + lastWatchedSeason
                  + " AND "
                  + EpisodeColumns.EPISODE
                  + ">"
                  + lastWatchedEpisode
                  + "))", null,
              EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1");
    }

    Uri notiUri = DatabaseUtils.getNotificationUri(toWatch);
    addNotificationUri(notiUri);

    if (toWatch.getCount() == 0 || watching) {
      if (!watching) {
        lastWatched.close();
      }
      return new SimpleMergeCursor(toWatch);
    }

    notiUri = DatabaseUtils.getNotificationUri(lastWatched);
    addNotificationUri(notiUri);

    return new SimpleMergeCursor(toWatch, lastWatched);
  }
}
