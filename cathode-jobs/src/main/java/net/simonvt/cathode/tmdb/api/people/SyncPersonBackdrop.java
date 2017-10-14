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

package net.simonvt.cathode.tmdb.api.people;

import android.content.ContentValues;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.TaggedImagesResultsPage;
import com.uwetrottmann.tmdb2.services.PeopleService;
import javax.inject.Inject;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.tmdb.api.TmdbCallJob;
import retrofit2.Call;
import timber.log.Timber;

public class SyncPersonBackdrop extends TmdbCallJob<TaggedImagesResultsPage> {

  @Inject transient PeopleService peopleService;

  @Inject transient PersonDatabaseHelper personHelper;

  private int tmdbId;

  public SyncPersonBackdrop(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override public String key() {
    return "SyncPersonBackdrop" + "&tmdbId=" + tmdbId;
  }

  @Override public int getPriority() {
    return JobPriority.IMAGES;
  }

  @Override public Call<TaggedImagesResultsPage> getCall() {
    return peopleService.taggedImages(tmdbId, 1, "en");
  }

  @Override public boolean handleResponse(TaggedImagesResultsPage images) {
    final long personId = personHelper.getIdFromTmdb(tmdbId);

    ContentValues values = new ContentValues();

    if (images.results.size() > 0) {
      Image taggedImage = images.results.get(0);
      final String path = ImageUri.create(ImageType.STILL, taggedImage.file_path);
      Timber.d("Backdrop: %s", path);

      values.put(PersonColumns.SCREENSHOT, path);
    } else {
      values.putNull(PersonColumns.SCREENSHOT);
    }

    getContentResolver().update(People.withId(personId), values, null, null);

    return true;
  }
}
