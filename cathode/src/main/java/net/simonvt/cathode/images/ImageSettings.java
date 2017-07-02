/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.uwetrottmann.tmdb2.entities.Configuration;
import java.util.Set;
import net.simonvt.cathode.common.util.Lists;
import net.simonvt.cathode.settings.Settings;

public final class ImageSettings {

  private ImageSettings() {
  }

  public static void updateTmdbConfiguration(Context context, Configuration configuration) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    Configuration.ImagesConfiguration images = configuration.images;

    editor.putString(Settings.TMDB_IMAGES_BASE_URL, images.base_url);
    editor.putString(Settings.TMDB_IMAGES_SECURE_BASE_URL, images.secure_base_url);

    Set<String> backdropSizes = Lists.asSet(images.backdrop_sizes);
    Set<String> logoSizes = Lists.asSet(images.logo_sizes);
    Set<String> stillSizes = Lists.asSet(images.still_sizes);
    Set<String> posterSizes = Lists.asSet(images.poster_sizes);
    Set<String> profileSizes = Lists.asSet(images.profile_sizes);

    editor.putStringSet(Settings.TMDB_IMAGES_BACKDROP_SIZES, backdropSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_LOGO_SIZES, logoSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_STILL_SIZES, stillSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_POSTER_SIZES, posterSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_PROFILE_SIZES, profileSizes);

    editor.apply();

    ImageSizeSelector.getInstance(context).updateSizes();
  }
}
