/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.entity;

public class Images {

  public static class ImageType {

    String full;

    String medium;

    String thumb;

    public String getFull() {
      return full;
    }

    public String getMedium() {
      return medium;
    }

    public String getThumb() {
      return thumb;
    }
  }

  ImageType fanart;

  ImageType poster;

  ImageType logo;

  ImageType clearart;

  ImageType banner;

  ImageType thumb;

  ImageType avatar;

  public ImageType getFanart() {
    return fanart;
  }

  public ImageType getPoster() {
    return poster;
  }

  public ImageType getLogo() {
    return logo;
  }

  public ImageType getClearart() {
    return clearart;
  }

  public ImageType getBanner() {
    return banner;
  }

  public ImageType getThumb() {
    return thumb;
  }

  public ImageType getAvatar() {
    return avatar;
  }
}
