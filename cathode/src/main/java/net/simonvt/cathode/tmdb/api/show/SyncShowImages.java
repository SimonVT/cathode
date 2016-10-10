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
import com.uwetrottmann.tmdb2.services.TvService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import retrofit2.Call;
import timber.log.Timber;

public class SyncShowImages extends TmdbCallJob<Images> {

  @Inject transient TvService tvService;

  @Inject transient ShowDatabaseHelper showHelper;

  private int tmdbId;

  public SyncShowImages(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override public String key() {
    return "SyncShowImages" + "&tmdbId=" + tmdbId;
  }

  @Override public int getPriority() {
    return PRIORITY_IMAGES;
  }

  @Override public Call<Images> getCall() {
    return tvService.images(tmdbId, "en");
  }

  @Override public void handleResponse(Images images) {
    final long showId = showHelper.getIdFromTmdb(tmdbId);

    ContentValues values = new ContentValues();

    if (images.backdrops.size() > 0) {
      Image backdrop = images.backdrops.get(0);
      final String path = ImageUri.create(ImageType.BACKDROP, backdrop.file_path);
      Timber.d("Backdrop: %s", path);

      values.put(ShowColumns.BACKDROP, path);
    } else {
      values.putNull(ShowColumns.BACKDROP);
    }

    if (images.posters.size() > 0) {
      Image poster = images.posters.get(0);
      final String path = ImageUri.create(ImageType.POSTER, poster.file_path);
      Timber.d("Poster: %s", path);

      values.put(ShowColumns.POSTER, path);
    } else {
      values.putNull(ShowColumns.POSTER);
    }

    getContentResolver().update(Shows.withId(showId), values, null, null);
  }
}
