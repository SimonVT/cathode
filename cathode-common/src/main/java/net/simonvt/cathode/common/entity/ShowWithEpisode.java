/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.entity;

public class ShowWithEpisode {

  private Show show;

  private Episode episode;

  public ShowWithEpisode(Show show, Episode episode) {
    this.show = show;
    this.episode = episode;
  }

  public Show getShow() {
    return show;
  }

  public Episode getEpisode() {
    return episode;
  }
}
