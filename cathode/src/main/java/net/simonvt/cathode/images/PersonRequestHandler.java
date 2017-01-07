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

package net.simonvt.cathode.images;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.PersonImages;
import com.uwetrottmann.tmdb2.entities.TaggedImagesResultsPage;
import com.uwetrottmann.tmdb2.services.PeopleService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.PersonDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.cathode.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.util.Closeables;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;
import timber.log.Timber;

public class PersonRequestHandler extends ItemRequestHandler {

  @Inject PeopleService peopleService;

  @Inject transient PersonDatabaseHelper personHelper;

  public PersonRequestHandler(Context context, ImageDownloader downloader) {
    super(context, downloader);
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_PERSON.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    return personHelper.getTmdbId(id);
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    Cursor c = null;

    try {
      c = context.getContentResolver().query(People.withId(id), new String[] {
          PersonColumns.HEADSHOT, PersonColumns.SCREENSHOT,
      }, null, null, null);
      c.moveToFirst();

      if (imageType == ImageType.PROFILE) {
        String headshotPath = Cursors.getString(c, PersonColumns.HEADSHOT);
        if (!TextUtils.isEmpty(headshotPath)) {
          return headshotPath;
        }
      } else if (imageType == ImageType.STILL) {
        String screenshotPath = Cursors.getString(c, PersonColumns.SCREENSHOT);
        if (!TextUtils.isEmpty(screenshotPath)) {
          return screenshotPath;
        }
      } else {
        throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
      }
    } finally {
      Closeables.closeQuietly(c);
    }

    return null;
  }

  protected void clearCachedPaths(long id) {
    ContentValues values = new ContentValues();
    values.putNull(PersonColumns.HEADSHOT);
    values.putNull(PersonColumns.SCREENSHOT);
    context.getContentResolver().update(People.withId(id), values, null, null);
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    String path = null;

    TmdbRateLimiter.acquire();
    if (imageType == ImageType.PROFILE) {
      Response<PersonImages> response = peopleService.images(tmdbId).execute();

      if (response.isSuccessful()) {
        PersonImages images = response.body();

        ContentValues values = new ContentValues();

        if (images.profiles.size() > 0) {
          Image profile = images.profiles.get(0);
          final String profilePath = ImageUri.create(ImageType.PROFILE, profile.file_path);

          values.put(PersonColumns.HEADSHOT, profilePath);
          path = profilePath;
        } else {
          values.putNull(PersonColumns.HEADSHOT);
        }

        context.getContentResolver().update(People.withId(id), values, null, null);
      }
    }
    if (imageType == ImageType.STILL) {
      Response<TaggedImagesResultsPage> response =
          peopleService.taggedImages(tmdbId, 1, "en").execute();

      if (response.isSuccessful()) {
        TaggedImagesResultsPage images = response.body();

        ContentValues values = new ContentValues();

        if (images.results.size() > 0) {
          Image screenshot = images.results.get(0);
          final String screenshotPath =
              ImageUri.create(ImageType.STILL, screenshot.file_path);

          values.put(PersonColumns.SCREENSHOT, screenshotPath);
          path = screenshotPath;
        } else {
          Timber.d("No screenshots");
          values.putNull(PersonColumns.SCREENSHOT);
        }

        context.getContentResolver().update(People.withId(id), values, null, null);
      }
    }

    return path;
  }
}
