/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Activity;
import net.simonvt.cathode.api.entity.ActivityItem;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.ServerTime;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.ActivityAction;
import net.simonvt.cathode.api.enumeration.ActivityType;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.ActivityService;
import net.simonvt.cathode.api.service.ServerService;
import net.simonvt.cathode.api.util.Joiner;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncActivityStreamTask extends TraktTask {

  @Inject transient ActivityService activityService;

  @Inject transient ServerService serverService;

  @Override protected void doTask() {
    final long lastSync = TraktTimestamps.lastActivityStreamSync(getContext());

    if (lastSync == -1L) {
      ServerTime time = serverService.time();
      TraktTimestamps.updateLastActivityStreamSync(getContext(), time.getTimestamp());
      queueTask(new SyncUserActivityTask());
    } else {
      // ActivityAction#ALL is broken, trakt only shows watchlist actions
      final String actions = Joiner.on(",")
          .join(ActivityAction.CHECKIN, ActivityAction.WATCHING, ActivityAction.SEEN,
              ActivityAction.COLLECTION, ActivityAction.WATCHLIST, ActivityAction.RATING);
      Activity activity =
          activityService.user(ActivityType.ALL, actions, lastSync, DetailLevel.ACTIVITY_MIN);

      if (activity.getActivity() != null) {
        List<ActivityItem> items = activity.getActivity();

        if (items.size() >= 100) {
          queueTask(new SyncUserActivityTask());
        } else {
          for (ActivityItem item : items) {
            ActivityType type = item.getType();

            // Don't trust the activity API, it doesn't show 'un' events. If something happened,
            // sync all the things.
            switch (type) {
              case SHOW:
                if (item.getShow() != null) {
                  queueTask(new SyncShowTask(item.getShow().getTvdbId()));
                }
                break;

              case EPISODE:
                TvShow show = item.getShow();
                if (!ShowWrapper.exists(getContentResolver(), show.getTvdbId())) {
                  queueTask(new SyncShowTask(show.getTvdbId()));
                } else {
                  if (item.getEpisode() != null) {
                    Episode episode = item.getEpisode();
                    queueTask(new SyncEpisodeTask(show.getTvdbId(), episode.getSeason(), episode.getNumber()));
                  } else if (item.getEpisodes() != null) {
                    List<Episode> episodes = item.getEpisodes();
                    for (Episode episode : episodes) {
                      queueTask(new SyncEpisodeTask(show.getTvdbId(), episode.getSeason(), episode.getNumber()));
                    }
                  }
                }
                break;

              case MOVIE:
                if (item.getMovie() != null) {
                  queueTask(new SyncMovieTask(item.getMovie().getTmdbId()));
                }
                break;
            }

            switch (item.getAction()) {
              case CHECKIN:
              case WATCHING:
                queueTask(new SyncWatchingTask());
                break;
            }
          }
        }
      }

      TraktTimestamps.updateLastActivityStreamSync(getContext(),
          activity.getTimestamps().getCurrent());
    }

    queueTask(new SyncWatchingTask());
    postOnSuccess();
  }
}

