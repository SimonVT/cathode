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

  int LOADER_SHOWS_UPCOMING = 100;
  int LOADER_SHOWS_WATCHED = 101;
  int LOADER_SHOWS_COLLECTION = 102;
  int LOADER_SHOWS_WATCHLIST = 103;
  int LOADER_EPISODES_WATCHLIST = 104;
  int LOADER_SHOWS_TRENDING = 105;
  int LOADER_SHOWS_RECOMMENDATIONS = 106;
  int LOADER_MOVIES_WATCHED = 200;
  int LOADER_MOVIES_COLLECTION = 201;
  int LOADER_MOVIES_WATCHLIST = 202;
  int LOADER_MOVIES_TRENDING = 203;
  int LOADER_MOVIES_RECOMMENDATIONS = 204;
  int LOADER_SHOW = 300;
  int LOADER_SHOW_WATCH = 301;
  int LOADER_SHOW_COLLECT = 302;
  int LOADER_SHOW_GENRES = 303;
  int LOADER_SHOW_SEASONS = 304;
  int LOADER_SHOW_ACTORS = 305;
  int LOADER_MOVIE = 400;
  int LOADER_MOVIE_ACTORS = 401;
  int LOADER_SEASON = 500;
  int LOADER_SEARCH_SHOWS = 600;
  int LOADER_SEARCH_MOVIES = 700;
  int LOADER_EPISODE = 800;
  int LOADER_SHOW_WATCHING = 900;
  int LOADER_MOVIE_WATCHING = 901;
}
