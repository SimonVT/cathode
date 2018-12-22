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
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.data.StringMapper;
import net.simonvt.cathode.common.entity.CastMember;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.entitymapper.CommentListMapper;
import net.simonvt.cathode.entitymapper.MovieCastMapper;
import net.simonvt.cathode.entitymapper.MovieListMapper;
import net.simonvt.cathode.entitymapper.MovieMapper;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
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

  private static final String[] RELATED_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_RELATED).column(RelatedMoviesColumns.RELATED_MOVIE_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.VOTES),
  };

  private long movieId = -1L;

  private LiveData<Movie> movie;
  private LiveData<List<String>> genres;
  private LiveData<List<CastMember>> cast;
  private LiveData<List<Comment>> userComments;
  private LiveData<List<Comment>> comments;
  private LiveData<List<Movie>> relatedMovies;

  public MovieViewModel(@NonNull Application application) {
    super(application);
  }

  public void setMovieId(long movieId) {
    if (this.movieId == -1L) {
      this.movieId = movieId;
      movie = new MappedCursorLiveData<>(getApplication(), Movies.withId(movieId), null, null, null,
          null, new MovieMapper());
      genres = new MappedCursorLiveData<>(getApplication(), MovieGenres.fromMovie(movieId),
          GENRES_PROJECTION, null, null, null,
          new StringMapper(DatabaseContract.MovieGenreColumns.GENRE));
      cast = new MappedCursorLiveData<>(getApplication(), MovieCast.fromMovie(movieId),
          MovieCastMapper.PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null,
          Tables.MOVIE_CAST + "." + MovieCastColumns.ID + " ASC LIMIT 3", new MovieCastMapper());
      userComments = new MappedCursorLiveData<>(getApplication(), Comments.fromMovie(movieId),
          CommentListMapper.PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null,
          new CommentListMapper());
      comments = new MappedCursorLiveData<>(getApplication(), Comments.fromMovie(movieId),
          CommentListMapper.PROJECTION,
          CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
          CommentColumns.LIKES + " DESC LIMIT 3", new CommentListMapper());
      relatedMovies = new MappedCursorLiveData<>(getApplication(), RelatedMovies.fromMovie(movieId),
          RELATED_PROJECTION, null, null, RelatedMoviesColumns.RELATED_INDEX + " ASC LIMIT 3",
          new MovieListMapper());
    }
  }

  public LiveData<Movie> getMovie() {
    return movie;
  }

  public LiveData<List<String>> getGenres() {
    return genres;
  }

  public LiveData<List<CastMember>> getCast() {
    return cast;
  }

  public LiveData<List<Comment>> getUserComments() {
    return userComments;
  }

  public LiveData<List<Comment>> getComments() {
    return comments;
  }

  public LiveData<List<Movie>> getRelatedMovies() {
    return relatedMovies;
  }

  @Override protected void onCleared() {
  }
}
