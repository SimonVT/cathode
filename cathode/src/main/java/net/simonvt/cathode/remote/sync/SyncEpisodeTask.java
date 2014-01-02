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
package net.simonvt.cathode.remote.sync;

import android.content.ContentResolver;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;
import timber.log.Timber;

public class SyncEpisodeTask extends TraktTask {

  private static final String TAG = "SyncEpisodeTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  private final int episode;

  public SyncEpisodeTask(int tvdbId, int season, int episode) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
  }

  @Override protected void doTask() {
    try {
      Timber.d("Syncing episode: %d - %d - %d", tvdbId, season, episode);

      Episode episode = showService.episodeSummary(tvdbId, season, this.episode).getEpisode();

      final ContentResolver resolver = service.getContentResolver();
      final long showId = ShowWrapper.getShowId(resolver, tvdbId);
      final long seasonId = ShowWrapper.getSeasonId(resolver, showId, season);

      EpisodeWrapper.updateOrInsertEpisode(service.getContentResolver(), episode, showId, seasonId);

      postOnSuccess();
    } catch (RetrofitError e) {
      retrofit.client.Response r = e.getResponse();
      if (r != null) {
        final int statusCode = r.getStatus();
        if (statusCode == 400) {
          Timber.tag(TAG).e(e, "URL: %s", e.getUrl());

          ResponseParser parser = new ResponseParser();
          CathodeApp.inject(service, parser);
          Response response = parser.tryParse(e);
          if (response != null && "episode not found".equals(response.getError())) {
            postOnSuccess();
            return;
          }
        }
      }
      postOnFailure();
    }
  }
}
