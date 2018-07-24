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

package net.simonvt.cathode.ui.movie;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.provider.util.SqlColumn;

public class MovieViewModel extends AndroidViewModel {

  private static final String[] GENRES_PROJECTION = new String[] {
      DatabaseContract.MovieGenreColumns.GENRE,
  };

  private static final String[] CAST_PROJECTION = new String[] {
      Tables.MOVIE_CAST + "." + MovieCastColumns.ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.PERSON_ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.CHARACTER,
      Tables.PEOPLE + "." + PersonColumns.NAME,
  };

  private static final String[] COMMENTS_PROJECTION = new String[] {
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.ID),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.COMMENT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.SPOILER),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REVIEW),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.CREATED_AT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKES),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.USER_RATING),
      SqlColumn.table(Tables.USERS).column(UserColumns.USERNAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.NAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR),
  };

  private static final String[] RELATED_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_RELATED).column(RelatedMoviesColumns.RELATED_MOVIE_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.VOTES),
  };

  private long movieId = -1L;

  private LiveData<Cursor> movie;
  private LiveData<Cursor> genres;
  private LiveData<Cursor> cast;
  private LiveData<Cursor> userComments;
  private LiveData<Cursor> comments;
  private LiveData<Cursor> relatedMovies;

  public MovieViewModel(@NonNull Application application) {
    super(application);
  }

  public void setMovieId(long movieId) {
    if (this.movieId == -1L) {
      this.movieId = movieId;
      movie = new CursorLiveData(getApplication(), Movies.withId(movieId), null, null, null, null);
      genres =
          new CursorLiveData(getApplication(), MovieGenres.fromMovie(movieId), GENRES_PROJECTION,
              null, null, null);
      cast = new CursorLiveData(getApplication(), MovieCast.fromMovie(movieId), CAST_PROJECTION,
          Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null, null);
      userComments =
          new CursorLiveData(getApplication(), Comments.fromMovie(movieId), COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=1", null, null);
      comments =
          new CursorLiveData(getApplication(), Comments.fromMovie(movieId), COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
              CommentColumns.LIKES + " DESC LIMIT 3");
      relatedMovies =
          new CursorLiveData(getApplication(), RelatedMovies.fromMovie(movieId), RELATED_PROJECTION,
              null, null, RelatedMoviesColumns.RELATED_INDEX + " ASC LIMIT 3");
    }
  }

  public LiveData<Cursor> getMovie() {
    return movie;
  }

  public LiveData<Cursor> getGenres() {
    return genres;
  }

  public LiveData<Cursor> getCast() {
    return cast;
  }

  public LiveData<Cursor> getUserComments() {
    return userComments;
  }

  public LiveData<Cursor> getComments() {
    return comments;
  }

  public LiveData<Cursor> getRelatedMovies() {
    return relatedMovies;
  }

  @Override protected void onCleared() {
  }
}
