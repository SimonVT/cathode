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

package net.simonvt.cathode.sync.tmdb.api.show;

import com.uwetrottmann.tmdb2.entities.Images;
import com.uwetrottmann.tmdb2.services.TvEpisodesService;
import javax.inject.Inject;
import net.simonvt.cathode.images.EpisodeRequestHandler;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.sync.tmdb.api.TmdbCallJob;
import retrofit2.Call;

public class SyncEpisodeImages extends TmdbCallJob<Images> {

  @Inject transient TvEpisodesService tvEpisodesService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  private int tmdbId;

  private int season;

  private int episode;

  public SyncEpisodeImages(int tmdbId, int season, int episode) {
    this.tmdbId = tmdbId;
    this.season = season;
    this.episode = episode;
  }

  @Override public String key() {
    return "SyncEpisodeImages" + "&tmdbId=" + tmdbId + "?season=" + season + "?episode=" + episode;
  }

  @Override public int getPriority() {
    return JobPriority.IMAGES;
  }

  @Override public Call<Images> getCall() {
    return tvEpisodesService.images(tmdbId, season, episode);
  }

  @Override public boolean handleResponse(Images images) {
    final long showId = showHelper.getIdFromTmdb(tmdbId);
    final long episodeId = episodeHelper.getId(showId, season, episode);

    if (episodeId == -1L) {
      return true;
    }

    EpisodeRequestHandler.retainImages(getContext(), episodeId, images);

    return true;
  }
}
