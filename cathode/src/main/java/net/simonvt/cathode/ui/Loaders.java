/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui;

public interface Loaders {

  int SHOWS_UPCOMING = 100;
  int SHOWS_WATCHED = 101;
  int SHOWS_COLLECTION = 102;
  int SHOWS_WATCHLIST = 103;
  int SHOWS_TRENDING = 104;
  int SHOWS_RECOMMENDATIONS = 105;

  int EPISODES_WATCHLIST = 106;

  int MOVIES_WATCHED = 200;
  int MOVIES_COLLECTION = 201;
  int MOVIES_WATCHLIST = 202;
  int MOVIES_TRENDING = 203;
  int MOVIES_RECOMMENDATIONS = 204;

  int SHOW = 300;
  int SHOW_WATCH = 301;
  int SHOW_COLLECT = 302;
  int SHOW_GENRES = 303;
  int SHOW_SEASONS = 304;
  int SHOW_ACTORS = 305;
  int SHOW_USER_COMMENTS = 306;
  int SHOW_COMMENTS = 307;

  int SEASON = 310;
  int EPISODE = 320;
  int EPISODE_USER_COMMENTS = 321;
  int EPISODE_COMMENTS = 322;

  int MOVIE = 400;
  int MOVIE_ACTORS = 401;
  int MOVIE_USER_COMMENTS = 402;
  int MOVIE_COMMENTS = 403;

  int SEARCH_SHOWS = 500;
  int SEARCH_MOVIES = 501;

  int SHOW_WATCHING = 600;
  int MOVIE_WATCHING = 601;

  int ACTORS = 702;

  int LISTS = 800;
  int LIST = 801;

  int DIALOG_LISTS = 1002;
  int DIALOG_LISTS_STATUS = 1003;

  int COMMENTS = 1100;
  int COMMENT = 1101;
}
