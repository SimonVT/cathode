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

import android.net.Uri;

public final class ImageUri {

  public static final String ITEM_SHOW = "show";
  public static final String ITEM_EPISODE = "episode";
  public static final String ITEM_MOVIE = "movie";
  public static final String ITEM_PERSON = "person";

  public static final String ITEM_IMAGE = "image";

  private ImageUri() {
  }

  public static String create(String itemType, ImageType imageType, long id) {
    return new Uri.Builder().scheme(itemType)
        .authority(imageType.toString())
        .path(String.valueOf(id))
        .build()
        .toString();
  }

  public static String create(ImageType imageType, String path) {
    return new Uri.Builder().scheme(ITEM_IMAGE)
        .authority(imageType.toString())
        .path(path)
        .build()
        .toString();
  }
}
