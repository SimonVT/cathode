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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.simonvt.cathode.settings.Settings;

public class ImageSizeSelector {

  private static ImageSizeSelector instance;

  public static ImageSizeSelector getInstance(Context context) {
    if (instance == null) {
      synchronized (ImageSizeSelector.class) {
        if (instance == null) {
          instance = new ImageSizeSelector(context.getApplicationContext());
        }
      }
    }

    return instance;
  }

  private static class ImageSize {

    String size;

    int width;

    int height;

    ImageSize(String size, int width, int height) {
      this.size = size;
      this.width = width;
      this.height = height;
    }

    @Override public String toString() {
      return "ImageSize{"
          + "size='"
          + size
          + '\''
          + ", width="
          + width
          + ", height="
          + height
          + '}';
    }
  }

  private Context context;

  private List<ImageSize> posterImageSizes = new ArrayList<>();
  private List<ImageSize> backdropImageSizes = new ArrayList<>();
  private List<ImageSize> profileImageSizes = new ArrayList<>();
  private List<ImageSize> stillImageSizes = new ArrayList<>();

  private ImageSizeSelector(Context context) {
    this.context = context;
    updateSizes();
  }

  public void updateSizes() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    Set<String> posterSizes = settings.getStringSet(Settings.TMDB_IMAGES_POSTER_SIZES, null);
    parseImageSizes(posterSizes, posterImageSizes);
    Set<String> backdropSizes = settings.getStringSet(Settings.TMDB_IMAGES_BACKDROP_SIZES, null);
    parseImageSizes(backdropSizes, backdropImageSizes);
    Set<String> profileSizes = settings.getStringSet(Settings.TMDB_IMAGES_PROFILE_SIZES, null);
    parseImageSizes(profileSizes, profileImageSizes);
    Set<String> stillSizes = settings.getStringSet(Settings.TMDB_IMAGES_STILL_SIZES, null);
    parseImageSizes(stillSizes, stillImageSizes);

    sort(posterImageSizes);
    sort(backdropImageSizes);
    sort(profileImageSizes);
    sort(stillImageSizes);
  }

  private void parseImageSizes(Set<String> sizeSet, List<ImageSize> imageSizes) {
    if (sizeSet == null) {
      return;
    }

    for (String s : sizeSet) {
      if (s.startsWith("w")) {
        String number = s.replace("w", "");
        final int width = Integer.valueOf(number);
        final int height = (int) (width * 1.5);

        ImageSize imageSize = new ImageSize(s, width, height);
        imageSizes.add(imageSize);
      } else if (s.startsWith("h")) {
        String number = s.replace("h", "");
        final int height = Integer.valueOf(number);
        final int width = (int) (height / 1.5);

        ImageSize imageSize = new ImageSize(s, width, height);
        imageSizes.add(imageSize);
      }
    }
  }

  private void sort(List<ImageSize> imageSizes) {
    Collections.sort(imageSizes, new Comparator<ImageSize>() {
      @Override public int compare(ImageSize o1, ImageSize o2) {
        return o1.width - o2.width;
      }
    });
  }

  private String getSize(List<ImageSize> imageSizes, int width, int height) {
    for (ImageSize imageSize : imageSizes) {
      if (width < imageSize.width && height < imageSize.height) {
        return imageSize.size;
      }
    }

    return "original";
  }

  public String getSize(ImageType imageType, int width, int height) {
    switch (imageType) {
      case POSTER:
        return getSize(posterImageSizes, width, height);

      case BACKDROP:
        return getSize(backdropImageSizes, width, height);

      case PROFILE:
        return getSize(profileImageSizes, width, height);

      case STILL:
        return getSize(stillImageSizes, width, height);
    }

    return "original";
  }
}
