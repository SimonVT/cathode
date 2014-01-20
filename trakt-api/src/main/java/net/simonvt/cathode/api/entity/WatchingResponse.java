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

public class WatchingResponse extends Response {

  private Movie movie;

  private Boolean facebook;

  private Boolean twitter;

  private Boolean tumblr;

  private Boolean path;

  public Movie getMovie() {
    return movie;
  }

  public Boolean getFacebook() {
    return facebook;
  }

  public Boolean getTwitter() {
    return twitter;
  }

  public Boolean getTumblr() {
    return tumblr;
  }

  public Boolean getPath() {
    return path;
  }
}
