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
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.shows.CollectSeason;
import net.simonvt.cathode.remote.action.shows.WatchedSeason;

public class SeasonTaskScheduler extends BaseTaskScheduler {

  public SeasonTaskScheduler(Context context) {
    super(context);
  }

  public void setWatched(final long seasonId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        String watchedAt = null;
        long watchedAtMillis = 0L;
        if (watched) {
          watchedAt = TimeUtils.getIsoTime();
          watchedAtMillis = TimeUtils.getMillis(watchedAt);
        }

        final long showId = SeasonWrapper.getShowId(context.getContentResolver(), seasonId);
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int seasonNumber =
            SeasonWrapper.getSeasonNumber(context.getContentResolver(), seasonId);

        queue(new WatchedSeason(traktId, seasonNumber, watched, watchedAt));
        SeasonWrapper.setWatched(context.getContentResolver(), showId, seasonId, watched,
            watchedAtMillis);
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

        final long showId = SeasonWrapper.getShowId(context.getContentResolver(), seasonId);
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int seasonNumber =
            SeasonWrapper.getSeasonNumber(context.getContentResolver(), seasonId);

        queue(new CollectSeason(traktId, seasonNumber, inCollection, collectedAt));
        SeasonWrapper.setIsInCollection(context.getContentResolver(), seasonId, inCollection,
            collectedAtMillis);
      }
    });
  }
}
