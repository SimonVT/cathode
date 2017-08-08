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
import com.uwetrottmann.tmdb2.entities.Configuration;
import java.util.Set;
import net.simonvt.cathode.common.util.Lists;

public final class ImageSettings {

  private static final String SETTINGS_FILE = "cathode_images";

  // themoviedb image configuration
  private static final String TMDB_IMAGES_BASE_URL = "tmdbImagesBaseUrl";
  private static final String TMDB_IMAGES_SECURE_BASE_URL = "tmdbImagesSecureBaseUrl";
  private static final String TMDB_IMAGES_BACKDROP_SIZES = "tmdbImagesBackdropSizes";
  private static final String TMDB_IMAGES_LOGO_SIZES = "tmdbImagesLogoSizes";
  private static final String TMDB_IMAGES_STILL_SIZES = "tmdbImagesStillSizes";
  private static final String TMDB_IMAGES_POSTER_SIZES = "tmdbImagesPosterSizes";
  private static final String TMDB_IMAGES_PROFILE_SIZES = "tmdbImagesProfileSizes";

  private ImageSettings() {
  }

  public static SharedPreferences get(Context context) {
    return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
  }

  public static String getSecureBaseUrl(Context context) {
    return get(context).getString(TMDB_IMAGES_SECURE_BASE_URL, null);
  }

  public static Set<String> getBackdropSizes(Context context) {
    return get(context).getStringSet(TMDB_IMAGES_BACKDROP_SIZES, null);
  }

  public static Set<String> getLogoSizes(Context context) {
    return get(context).getStringSet(TMDB_IMAGES_LOGO_SIZES, null);
  }

  public static Set<String> getStillSizes(Context context) {
    return get(context).getStringSet(TMDB_IMAGES_STILL_SIZES, null);
  }

  public static Set<String> getPosterSizes(Context context) {
    return get(context).getStringSet(TMDB_IMAGES_POSTER_SIZES, null);
  }

  public static Set<String> getProfileSizes(Context context) {
    return get(context).getStringSet(TMDB_IMAGES_PROFILE_SIZES, null);
  }

  public static void updateTmdbConfiguration(Context context, Configuration configuration) {
    SharedPreferences.Editor editor = get(context).edit();

    Configuration.ImagesConfiguration images = configuration.images;

    editor.putString(TMDB_IMAGES_BASE_URL, images.base_url);
    editor.putString(TMDB_IMAGES_SECURE_BASE_URL, images.secure_base_url);

    Set<String> backdropSizes = Lists.asSet(images.backdrop_sizes);
    Set<String> logoSizes = Lists.asSet(images.logo_sizes);
    Set<String> stillSizes = Lists.asSet(images.still_sizes);
    Set<String> posterSizes = Lists.asSet(images.poster_sizes);
    Set<String> profileSizes = Lists.asSet(images.profile_sizes);

    editor.putStringSet(TMDB_IMAGES_BACKDROP_SIZES, backdropSizes);
    editor.putStringSet(TMDB_IMAGES_LOGO_SIZES, logoSizes);
    editor.putStringSet(TMDB_IMAGES_STILL_SIZES, stillSizes);
    editor.putStringSet(TMDB_IMAGES_POSTER_SIZES, posterSizes);
    editor.putStringSet(TMDB_IMAGES_PROFILE_SIZES, profileSizes);

    editor.apply();

    ImageSizeSelector.getInstance(context).updateSizes();
  }
}
