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
package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentResolver;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.EpisodeService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class SyncEpisodeTask extends TraktTask {

  @Inject transient EpisodeService episodeService;

  private long traktId;

  private int season;

  private int episode;

  public SyncEpisodeTask(long traktId, int season, int episode) {
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
  }

  @Override protected void doTask() {
    Timber.d("Syncing episode: %d - %d - %d", traktId, season, episode);

    try {
      Episode summary = episodeService.getSummary(traktId, season, episode, Extended.FULL_IMAGES);

      final ContentResolver resolver = getContentResolver();
      final long showId = ShowWrapper.getShowId(resolver, traktId);
      final long seasonId = ShowWrapper.getSeasonId(resolver, showId, season);

      if (showId == -1L || seasonId == -1L) {
        queueTask(new SyncShowTask(traktId));
        postOnSuccess();
        return;
      }

      EpisodeWrapper.updateOrInsertEpisode(getContentResolver(), summary, showId, seasonId);

      postOnSuccess();
    } catch (RetrofitError e) {
      Response response = e.getResponse();
      if (response != null && response.getStatus() == 404) {
        // Episode no longer exists
        postOnSuccess();
      } else {
        postOnFailure();
      }
    }
  }
}
