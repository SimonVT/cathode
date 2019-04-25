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
package net.simonvt.cathode.provider;

import net.simonvt.cathode.provider.entity.ItemTypeString;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;

public interface Joins {

  String SHOWS_UNWATCHED =
      "LEFT OUTER JOIN "
          + Tables.EPISODES
          + " ON "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + "=(SELECT "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES + "." + EpisodeColumns.WATCHED
          + "=0 AND "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + "<>0"
          + " ORDER BY "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + " ASC, "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + " ASC LIMIT 1)";

  String SHOWS_UPCOMING =
      "LEFT OUTER JOIN "
          + Tables.EPISODES
          + " ON "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + "=(SELECT "
          + EpisodeColumns.ID
          + " FROM "
          + Tables.EPISODES
          + " JOIN (SELECT "
          + EpisodeColumns.SEASON
          + ","
          + EpisodeColumns.EPISODE
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + EpisodeColumns.WATCHED
          + "=1 AND "
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " ORDER BY "
          + EpisodeColumns.SEASON
          + " DESC, "
          + EpisodeColumns.EPISODE
          + " DESC LIMIT 1"
          + ") AS ep2 "
          + "WHERE "
          + Tables.EPISODES + "." + EpisodeColumns.WATCHED
          + "=0 AND "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " AND ("
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + ">"
          + "ep2" + "." + EpisodeColumns.SEASON
          + " OR ("
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + "="
          + "ep2" + "." + EpisodeColumns.SEASON
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + ">"
          + "ep2" + "." + EpisodeColumns.EPISODE
          + ")) "
          + "ORDER BY "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + " ASC, "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + " ASC LIMIT 1" + ")";

  String SHOWS_WITH_NEXT =
      "LEFT OUTER JOIN "
          + Tables.EPISODES
          + " ON "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + "="
          + "("
          + "SELECT "
          + EpisodeColumns.ID
          + " FROM "
          + Tables.EPISODES
          + " JOIN ("
          + "SELECT "
          + EpisodeColumns.SEASON
          + ", "
          + EpisodeColumns.EPISODE
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + EpisodeColumns.WATCHED
          + "=1 AND "
          + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " ORDER BY "
          + EpisodeColumns.SEASON
          + " DESC, "
          + EpisodeColumns.EPISODE
          + " DESC LIMIT 1"
          + ") AS ep2"
          + " WHERE "
          + Tables.EPISODES + "." + EpisodeColumns.WATCHED
          + "=0 AND "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " AND ("
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + ">"
          + "ep2" + "." + EpisodeColumns.SEASON
          + "OR ("
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + "="
          + "ep2" + "." + EpisodeColumns.SEASON
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + ">"
          + "ep2" + "." + EpisodeColumns.EPISODE
          + "))"
          + " ORDER BY "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + " ASC, "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + " ASC LIMIT 1"
          + ")";

  String SHOWS_UNCOLLECTED =
      "LEFT OUTER JOIN "
          + Tables.EPISODES
          + " ON "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + "=(SELECT "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + " FROM "
          + Tables.EPISODES
          + " WHERE "
          + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION
          + "=0 AND "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + "<>0"
          + " ORDER BY "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + " ASC, "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + " ASC LIMIT 1)";

  String SHOWS_WITH_WATCHING =
      "LEFT OUTER JOIN "
          + Tables.EPISODES
          + " ON "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + "=(SELECT "
          + Tables.EPISODES + "." + EpisodeColumns.ID
          + " FROM "
          + Tables.EPISODES
          + " WHERE ("
          + Tables.EPISODES + "." + EpisodeColumns.WATCHING
          + "=1 OR "
          + Tables.EPISODES + "." + EpisodeColumns.CHECKED_IN
          + "=1)"
          + " AND "
          + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
          + "="
          + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID
          + " ORDER BY "
          + Tables.EPISODES + "." + EpisodeColumns.SEASON
          + " ASC, "
          + Tables.EPISODES + "." + EpisodeColumns.EPISODE
          + " ASC LIMIT 1)";

  String SHOW_RELATED = "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS
      + "." + DatabaseContract.ShowColumns.ID + "="
      + Tables.SHOW_RELATED + "." + DatabaseContract.RelatedShowsColumns.RELATED_SHOW_ID;

  String MOVIE_RELATED = "JOIN " + Tables.MOVIES + " AS " + Tables.MOVIES + " ON " + Tables.MOVIES
      + "." + DatabaseContract.MovieColumns.ID + "="
      + Tables.MOVIE_RELATED + "." + DatabaseContract.RelatedMoviesColumns.RELATED_MOVIE_ID;

  String SHOW_CAST_PERSON =
      "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
          + DatabaseContract.PersonColumns.ID + "=" + Tables.SHOW_CAST + "."
          + DatabaseContract.ShowCastColumns.PERSON_ID;

  String SHOW_CAST_SHOW =
      "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS + "."
          + DatabaseContract.ShowColumns.ID + "=" + Tables.SHOW_CAST + "."
          + DatabaseContract.ShowCrewColumns.SHOW_ID;

  String SHOW_CAST = SHOW_CAST_PERSON + " " + SHOW_CAST_SHOW;

  String SHOW_CREW_PERSON =
      "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
          + DatabaseContract.PersonColumns.ID + "=" + Tables.SHOW_CREW + "."
          + DatabaseContract.ShowCrewColumns.PERSON_ID;

  String SHOW_CREW_SHOW =
      "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS + "."
          + DatabaseContract.ShowColumns.ID + "=" + Tables.SHOW_CREW + "."
          + DatabaseContract.ShowCrewColumns.SHOW_ID;

  String SHOW_CREW = SHOW_CREW_PERSON + " " + SHOW_CREW_SHOW;

  String MOVIE_CAST_PERSON =
      "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
          + DatabaseContract.PersonColumns.ID + "=" + Tables.MOVIE_CAST + "."
          + DatabaseContract.MovieCastColumns.PERSON_ID;

  String MOVIE_CAST_MOVIE =
      "JOIN " + Tables.MOVIES + " AS " + Tables.MOVIES + " ON " + Tables.MOVIES + "."
          + DatabaseContract.MovieColumns.ID + "=" + Tables.MOVIE_CAST + "."
          + DatabaseContract.MovieCrewColumns.MOVIE_ID;

  String MOVIE_CAST = MOVIE_CAST_PERSON + " " + MOVIE_CAST_MOVIE;

  String MOVIE_CREW_PERSON =
      "JOIN " + Tables.PEOPLE + " AS " + Tables.PEOPLE + " ON " + Tables.PEOPLE + "."
          + DatabaseContract.PersonColumns.ID + "=" + Tables.MOVIE_CREW + "."
          + DatabaseContract.MovieCrewColumns.PERSON_ID;

  String MOVIE_CREW_MOVIE =
      "JOIN " + Tables.MOVIES + " AS " + Tables.MOVIES + " ON " + Tables.MOVIES + "."
          + DatabaseContract.MovieColumns.ID + "=" + Tables.MOVIE_CREW + "."
          + DatabaseContract.MovieCrewColumns.MOVIE_ID;

  String MOVIE_CREW = MOVIE_CREW_PERSON + " " + MOVIE_CREW_MOVIE;

  String EPISODES_WITH_SHOW = "LEFT OUTER JOIN " + Tables.SHOWS + " ON " + Tables.SHOWS + "."
          + DatabaseContract.ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID;

  String EPISODES_WITH_SHOW_TITLE =
      "JOIN " + Tables.SHOWS + " AS " + Tables.SHOWS + " ON " + Tables.SHOWS + "."
          + DatabaseContract.ShowColumns.ID + "=" + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID;

  String LIST_SHOWS = "LEFT JOIN " + Tables.SHOWS + " ON " + DatabaseContract.ListItemColumns.ITEM_TYPE + "='"
      + ItemTypeString.SHOW + "' AND " + Tables.LIST_ITEMS + "."
      + DatabaseContract.ListItemColumns.ITEM_ID + "=" + Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID;

  String LIST_SEASONS = "LEFT JOIN " + Tables.SEASONS + " ON " + DatabaseContract.ListItemColumns.ITEM_TYPE + "='"
      + ItemTypeString.SEASON + "' AND " + Tables.LIST_ITEMS + "."
      + DatabaseContract.ListItemColumns.ITEM_ID + "=" + Tables.SEASONS + "." + DatabaseContract.SeasonColumns.ID;

  String LIST_EPISODES = "LEFT JOIN " + Tables.EPISODES + " ON " + DatabaseContract.ListItemColumns.ITEM_TYPE + "='"
      + ItemTypeString.EPISODE + "' AND " + Tables.LIST_ITEMS + "."
      + DatabaseContract.ListItemColumns.ITEM_ID + "=" + Tables.EPISODES + "." + EpisodeColumns.ID;

  String LIST_MOVIES = "LEFT JOIN " + Tables.MOVIES + " ON " + DatabaseContract.ListItemColumns.ITEM_TYPE + "='"
      + ItemTypeString.MOVIE + "' AND " + Tables.LIST_ITEMS + "."
      + DatabaseContract.ListItemColumns.ITEM_ID + "=" + Tables.MOVIES + "." + DatabaseContract.MovieColumns.ID;

  String LIST_PEOPLE = "LEFT JOIN " + Tables.PEOPLE + " ON " + DatabaseContract.ListItemColumns.ITEM_TYPE + "='"
      + ItemTypeString.PERSON + "' AND " + Tables.LIST_ITEMS + "."
      + DatabaseContract.ListItemColumns.ITEM_ID + "=" + Tables.PEOPLE + "." + DatabaseContract.PersonColumns.ID;

  String LIST = LIST_SHOWS + " " + LIST_SEASONS + " " + LIST_EPISODES + " " + LIST_MOVIES + " "
      + LIST_PEOPLE;

  String COMMENT_PROFILE =
      "JOIN " + Tables.USERS + " AS " + Tables.USERS + " ON " + Tables.USERS + "."
          + DatabaseContract.UserColumns.ID + "=" + Tables.COMMENTS + "." + DatabaseContract.CommentColumns.USER_ID;
}
