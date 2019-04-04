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

package net.simonvt.cathode.ui.movie

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.comments.SyncMovieComments
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.movies.SyncMovie
import net.simonvt.cathode.actions.movies.SyncMovieCredits
import net.simonvt.cathode.actions.movies.SyncMovieImages
import net.simonvt.cathode.actions.movies.SyncRelatedMovies
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.data.StringMapper
import net.simonvt.cathode.common.entity.CastMember
import net.simonvt.cathode.common.entity.Comment
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.entitymapper.CommentListMapper
import net.simonvt.cathode.entitymapper.CommentMapper
import net.simonvt.cathode.entitymapper.MovieCastMapper
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.util.SqlColumn
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class MovieViewModel @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncMovie: SyncMovie,
  private val syncMovieComments: SyncMovieComments,
  private val syncMovieCredits: SyncMovieCredits,
  private val syncMovieImages: SyncMovieImages,
  private val syncRelatedMovies: SyncRelatedMovies
) : RefreshableViewModel() {

  private var movieId = -1L

  lateinit var movie: LiveData<Movie>
    private set
  lateinit var genres: LiveData<List<String>>
    private set
  lateinit var cast: LiveData<List<CastMember>>
    private set
  lateinit var userComments: LiveData<List<Comment>>
    private set
  lateinit var comments: LiveData<List<Comment>>
    private set
  lateinit var relatedMovies: LiveData<List<Movie>>
    private set

  fun setMovieId(movieId: Long) {
    if (this.movieId == -1L) {
      this.movieId = movieId
      movie = MappedCursorLiveData(
        context,
        Movies.withId(movieId),
        null,
        null,
        null,
        null,
        MovieMapper()
      )
      genres = MappedCursorLiveData(
        context,
        MovieGenres.fromMovie(movieId),
        GENRES_PROJECTION,
        null,
        null,
        null,
        StringMapper(DatabaseContract.MovieGenreColumns.GENRE)
      )
      cast = MappedCursorLiveData(
        context,
        MovieCast.fromMovie(movieId),
        MovieCastMapper.PROJECTION,
        Tables.PEOPLE,
        null,
        Tables.MOVIE_CAST + "." + MovieCastColumns.ID + " ASC LIMIT 3",
        MovieCastMapper()
      )
      userComments = MappedCursorLiveData(
        context,
        Comments.fromMovie(movieId),
        CommentMapper.PROJECTION,
        CommentColumns.IS_USER_COMMENT + "=1",
        null,
        null,
        CommentListMapper()
      )
      comments = MappedCursorLiveData(
        context,
        Comments.fromMovie(movieId),
        CommentMapper.PROJECTION,
        CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0",
        null,
        CommentColumns.LIKES + " DESC LIMIT 3",
        CommentListMapper()
      )
      relatedMovies = MappedCursorLiveData(
        context,
        RelatedMovies.fromMovie(movieId),
        RELATED_PROJECTION,
        null,
        null,
        RelatedMoviesColumns.RELATED_INDEX + " ASC LIMIT 3",
        MovieListMapper()
      )
    }

    movie.observeForever(movieObserver)
  }

  override fun onCleared() {
    movie.removeObserver(movieObserver)
    super.onCleared()
  }

  private val movieObserver = Observer<Movie> { movie ->
    if (movie != null) {
      viewModelScope.launch {
        val currentTime = System.currentTimeMillis()
        val needsSync = movie.needsSync!!
        val lastSync = movie.lastSync!!
        if (needsSync || System.currentTimeMillis() > lastSync + SYNC_INTERVAL) {
          syncMovie.invokeAsync(SyncMovie.Params(movie.traktId))
        }

        if (currentTime > movie.lastCommentSync + SYNC_INTERVAL_COMMENTS) {
          syncMovieComments.invokeAsync(SyncMovieComments.Params(movie.traktId))
        }

        if (lastSync > movie.lastCreditsSync) {
          syncMovieCredits.invokeAsync(SyncMovieCredits.Params(movie.traktId))
        }

        if (lastSync > movie.lastRelatedSync) {
          syncRelatedMovies.invokeAsync(SyncRelatedMovies.Params(movie.traktId))
        }
      }
    }
  }

  override suspend fun onRefresh() {
    val traktId = movieHelper.getTraktId(movieId)
    val tmdbId = movieHelper.getTmdbId(movieId)
    val movieDeferred = syncMovie.invokeAsync(SyncMovie.Params(traktId))
    val creditsDeferred = syncMovieCredits.invokeAsync(SyncMovieCredits.Params(traktId))
    val imagesDeferred = syncMovieImages.invokeAsync(SyncMovieImages.Params(tmdbId))
    val relatedDeferred = syncRelatedMovies.invokeAsync(SyncRelatedMovies.Params(traktId))

    movieDeferred.await()
    creditsDeferred.await()
    imagesDeferred.await()
    relatedDeferred.await()
  }

  companion object {

    private const val SYNC_INTERVAL = 2 * DateUtils.DAY_IN_MILLIS
    private const val SYNC_INTERVAL_COMMENTS = 3 * DateUtils.HOUR_IN_MILLIS

    private val GENRES_PROJECTION = arrayOf(DatabaseContract.MovieGenreColumns.GENRE)

    private val RELATED_PROJECTION = arrayOf(
      SqlColumn.table(Tables.MOVIE_RELATED).column(RelatedMoviesColumns.RELATED_MOVIE_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.VOTES)
    )
  }
}
