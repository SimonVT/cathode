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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ImageType {
  POSTER("poster", 1.5f),
  BACKDROP("backdrop", 0.5617977528f),
  PROFILE("profile", 1.5f),
  STILL("still", 0.5617977528f);

  private final String value;

  private final float ratio;

  ImageType(String value, float ratio) {
    this.value = value;
    this.ratio = ratio;
  }

  @Override public String toString() {
    return value;
  }

  public float getRatio() {
    return ratio;
  }

  private static final Map<String, ImageType> STRING_MAPPING = new HashMap<>();

  static {
    for (ImageType via : ImageType.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
    }
  }

  public static ImageType fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
