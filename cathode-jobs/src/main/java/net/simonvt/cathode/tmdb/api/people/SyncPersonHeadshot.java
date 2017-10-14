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
import com.uwetrottmann.tmdb2.entities.PersonImages;
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

public class SyncPersonHeadshot extends TmdbCallJob<PersonImages> {

  @Inject transient PeopleService peopleService;

  @Inject transient PersonDatabaseHelper personHelper;

  private int tmdbId;

  public SyncPersonHeadshot(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override public String key() {
    return "SyncPersonHeadshot" + "&tmdbId=" + tmdbId;
  }

  @Override public int getPriority() {
    return JobPriority.IMAGES;
  }

  @Override public Call<PersonImages> getCall() {
    return peopleService.images(tmdbId);
  }

  @Override public boolean handleResponse(PersonImages images) {
    final long personId = personHelper.getIdFromTmdb(tmdbId);

    ContentValues values = new ContentValues();

    if (images.profiles.size() > 0) {
      Image still = images.profiles.get(0);
      final String path = ImageUri.create(ImageType.STILL, still.file_path);
      Timber.d("Profile: %s", path);

      values.put(PersonColumns.HEADSHOT, path);
    } else {
      values.putNull(PersonColumns.HEADSHOT);
    }

    getContentResolver().update(People.withId(personId), values, null, null);

    return true;
  }
}
