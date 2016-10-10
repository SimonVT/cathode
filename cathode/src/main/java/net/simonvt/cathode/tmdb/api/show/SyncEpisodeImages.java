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

package net.simonvt.cathode.tmdb.api.show;

import android.content.ContentValues;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.Images;
import com.uwetrottmann.tmdb2.services.TvEpisodesService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import retrofit2.Call;
import timber.log.Timber;

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
    return PRIORITY_IMAGES;
  }

  @Override public Call<Images> getCall() {
    return tvEpisodesService.images(tmdbId, season, episode);
  }

  @Override public void handleResponse(Images images) {
    final long showId = showHelper.getIdFromTmdb(tmdbId);
    final long episodeId = episodeHelper.getId(showId, season, episode);

    ContentValues values = new ContentValues();

    if (images.stills.size() > 0) {
      Image still = images.stills.get(0);
      final String path = ImageUri.create(ImageType.STILL, still.file_path);
      Timber.d("Still: %s", path);

      values.put(EpisodeColumns.SCREENSHOT, path);
    } else {
      values.putNull(EpisodeColumns.SCREENSHOT);
    }

    getContentResolver().update(Episodes.withId(episodeId), values, null, null);
  }
}
