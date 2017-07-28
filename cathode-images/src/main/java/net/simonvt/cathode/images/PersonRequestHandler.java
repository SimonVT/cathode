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
import android.text.format.DateUtils;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.entities.Person;
import com.uwetrottmann.tmdb2.entities.TaggedImage;
import com.uwetrottmann.tmdb2.entities.TaggedImagesResultsPage;
import com.uwetrottmann.tmdb2.services.PeopleService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.common.util.Closeables;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.PersonDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.schematic.Cursors;
import retrofit2.Response;

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
          PersonColumns.IMAGES_LAST_UPDATE, PersonColumns.HEADSHOT, PersonColumns.SCREENSHOT,
      }, null, null, null);
      c.moveToFirst();

      final long lastUpdate = Cursors.getLong(c, PersonColumns.IMAGES_LAST_UPDATE);
      final boolean needsUpdate =
          lastUpdate + DateUtils.WEEK_IN_MILLIS < System.currentTimeMillis();

      if (imageType == ImageType.PROFILE) {
        String headshotPath = Cursors.getString(c, PersonColumns.HEADSHOT);
        if (!needsUpdate && !TextUtils.isEmpty(headshotPath)) {
          return headshotPath;
        }
      } else if (imageType == ImageType.STILL) {
        String screenshotPath = Cursors.getString(c, PersonColumns.SCREENSHOT);
        if (!needsUpdate && !TextUtils.isEmpty(screenshotPath)) {
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
    values.put(PersonColumns.IMAGES_LAST_UPDATE, 0L);
    values.putNull(PersonColumns.HEADSHOT);
    values.putNull(PersonColumns.SCREENSHOT);
    context.getContentResolver().update(People.withId(id), values, null, null);
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    String path = null;

    TmdbRateLimiter.acquire();
    Response<Person> personResponse = peopleService.summary(tmdbId).execute();
    TmdbRateLimiter.acquire();
    Response<TaggedImagesResultsPage> stillResponse =
        peopleService.taggedImages(tmdbId, 1, "en").execute();

    if (!personResponse.isSuccessful() || !stillResponse.isSuccessful()) {
      return null;
    }

    Person person = personResponse.body();

    ContentValues values = new ContentValues();
    values.put(PersonColumns.IMAGES_LAST_UPDATE, System.currentTimeMillis());

    if (person.profile_path != null) {
      final String profilePath = ImageUri.create(ImageType.PROFILE, person.profile_path);
      values.put(PersonColumns.HEADSHOT, profilePath);

      if (imageType == ImageType.PROFILE) {
        path = profilePath;
      }
    } else {
      values.putNull(PersonColumns.HEADSHOT);
    }

    TaggedImagesResultsPage images = stillResponse.body();
    List<Image> imageList = new ArrayList<>();
    for (TaggedImage image : images.results) {
      imageList.add(image);
    }
    Image screenshot = selectBest(imageList);

    if (screenshot != null) {
      final String screenshotPath = ImageUri.create(ImageType.STILL, screenshot.file_path);
      values.put(PersonColumns.SCREENSHOT, screenshotPath);

      if (imageType == ImageType.STILL) {
        path = screenshotPath;
      }
    } else {
      values.putNull(PersonColumns.SCREENSHOT);
    }

    context.getContentResolver().update(People.withId(id), values, null, null);

    return path;
  }
}
