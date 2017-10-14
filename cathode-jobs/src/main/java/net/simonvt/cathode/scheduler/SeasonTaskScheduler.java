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
package net.simonvt.cathode.scheduler;

import android.content.Context;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.action.shows.AddSeasonToHistory;
import net.simonvt.cathode.remote.action.shows.CollectSeason;
import net.simonvt.cathode.remote.action.shows.RemoveSeasonFromHistory;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;

public class SeasonTaskScheduler extends BaseTaskScheduler {

  @Inject ShowDatabaseHelper showHelper;
  @Inject SeasonDatabaseHelper seasonHelper;

  public SeasonTaskScheduler(Context context) {
    super(context);
  }

  public void addToHistoryNow(final long seasonId) {
    addToHistory(seasonId, System.currentTimeMillis());
  }

  public void addToHistoryOnRelease(final long seasonId) {
    addToHistory(seasonId, SyncItems.TIME_RELEASED);
  }

  public void addToHistory(final long seasonId, final long watchedAt) {
    final String isoWhen = TimeUtils.getIsoTime(watchedAt);
    addToHistory(seasonId, isoWhen);
  }

  public void addToHistory(final long seasonId, final int year, final int month, final int day,
      final int hour, final int minute) {
    addToHistory(seasonId, TimeUtils.getMillis(year, month, day, hour, minute));
  }

  public void addToHistory(final long seasonId, final String watchedAt) {
    execute(new Runnable() {
      @Override public void run() {
        final long showId = seasonHelper.getShowId(seasonId);
        final long traktId = showHelper.getTraktId(showId);
        final int seasonNumber = seasonHelper.getNumber(seasonId);

        long watched = SeasonDatabaseHelper.WATCHED_RELEASE;
        if (!SyncItems.TIME_RELEASED.equals(watchedAt)) {
          watched = TimeUtils.getMillis(watchedAt);
        }

        seasonHelper.addToHistory(seasonId, watched);
        queue(new AddSeasonToHistory(traktId, seasonNumber, watchedAt));
        // No documentation on how exactly the trakt endpoint is implemented, so sync after.
        queue(new SyncWatchedShows());
      }
    });
  }

  public void removeFromHistory(final long seasonId) {
    execute(new Runnable() {
      @Override public void run() {
        seasonHelper.removeFromHistory(seasonId);

        final long showId = seasonHelper.getShowId(seasonId);
        final long traktId = showHelper.getTraktId(showId);
        final int seasonNumber = seasonHelper.getNumber(seasonId);

        queue(new RemoveSeasonFromHistory(traktId, seasonNumber));
      }
    });
  }

  public void setInCollection(final long seasonId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        String collectedAt = null;
        long collectedAtMillis = 0L;
        if (inCollection) {
          collectedAt = TimeUtils.getIsoTime();
          collectedAtMillis = TimeUtils.getMillis(collectedAt);
        }

        final long showId = seasonHelper.getShowId(seasonId);
        final long traktId = showHelper.getTraktId(showId);
        final int seasonNumber = seasonHelper.getNumber(seasonId);

        queue(new CollectSeason(traktId, seasonNumber, inCollection, collectedAt));
        seasonHelper.setIsInCollection(seasonId, inCollection, collectedAtMillis);
      }
    });
  }
}
