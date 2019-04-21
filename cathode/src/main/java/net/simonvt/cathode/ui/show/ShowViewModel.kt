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

package net.simonvt.cathode.ui.show

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.comments.SyncShowComments
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.shows.SyncRelatedShows
import net.simonvt.cathode.actions.shows.SyncShow
import net.simonvt.cathode.actions.shows.SyncShowCollectedStatus
import net.simonvt.cathode.actions.shows.SyncShowCredits
import net.simonvt.cathode.actions.shows.SyncShowImages
import net.simonvt.cathode.actions.shows.SyncShowWatchedStatus
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.data.StringMapper
import net.simonvt.cathode.entity.CastMember
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Season
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.entitymapper.CommentListMapper
import net.simonvt.cathode.entitymapper.CommentMapper
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.entitymapper.SeasonListMapper
import net.simonvt.cathode.entitymapper.SeasonMapper
import net.simonvt.cathode.entitymapper.ShowCastMapper
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.entitymapper.ShowMapper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class ShowViewModel @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val syncShow: SyncShow,
  private val syncRelatedShows: SyncRelatedShows,
  private val syncShowImages: SyncShowImages,
  private val syncShowComments: SyncShowComments,
  private val syncShowCredits: SyncShowCredits,
  private val syncShowWatchedStatus: SyncShowWatchedStatus,
  private val syncShowCollectedStatus: SyncShowCollectedStatus
) : RefreshableViewModel() {

  private var showId = -1L

  lateinit var show: LiveData<Show>
    private set
  lateinit var genres: LiveData<List<String>>
    private set
  lateinit var seasons: LiveData<List<Season>>
    private set
  lateinit var cast: LiveData<List<CastMember>>
    private set
  lateinit var userComments: LiveData<List<Comment>>
    private set
  lateinit var comments: LiveData<List<Comment>>
    private set
  lateinit var related: LiveData<List<Show>>
    private set
  lateinit var toWatch: LiveData<Episode>
    private set
  lateinit var lastWatched: LiveData<Episode>
    private set
  lateinit var toCollect: LiveData<Episode>
    private set
  lateinit var lastCollected: LiveData<Episode>
    private set

  fun setShowId(showId: Long) {
    if (this.showId == -1L) {
      this.showId = showId

      show = MappedCursorLiveData(
        context,
        Shows.withId(showId),
        ShowMapper.projection,
        null,
        null,
        null,
        ShowMapper
      )
      genres = MappedCursorLiveData(
        context,
        ShowGenres.fromShow(showId),
        arrayOf(ShowGenreColumns.GENRE),
        null,
        null,
        null,
        StringMapper(ShowGenreColumns.GENRE)
      )
      seasons = MappedCursorLiveData(
        context,
        Seasons.fromShow(showId),
        SeasonMapper.projection,
        null,
        null,
        Seasons.DEFAULT_SORT,
        SeasonListMapper
      )
      cast = MappedCursorLiveData(
        context,
        ShowCast.fromShow(showId),
        ShowCastMapper.projection,
        null,
        null,
        Tables.SHOW_CAST + "." + ShowCastColumns.ID + " ASC LIMIT 3",
        ShowCastMapper
      )
      userComments = MappedCursorLiveData(
        context,
        Comments.fromShow(showId),
        CommentMapper.projection,
        CommentColumns.IS_USER_COMMENT + "=1",
        null,
        null,
        CommentListMapper
      )
      comments = MappedCursorLiveData(
        context,
        Comments.fromShow(showId),
        CommentMapper.projection,
        CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0",
        null,
        CommentColumns.LIKES + " DESC LIMIT 3",
        CommentListMapper
      )
      related = MappedCursorLiveData(
        context,
        RelatedShows.fromShow(showId),
        ShowMapper.projection,
        null,
        null,
        RelatedShowsColumns.RELATED_INDEX + " ASC LIMIT 3",
        ShowListMapper
      )
      toWatch = WatchedLiveData(context, showId)
      lastWatched = MappedCursorLiveData(
        context,
        Episodes.fromShow(showId),
        EpisodeMapper.projection,
        EpisodeColumns.WATCHED + "=1",
        null,
        EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1",
        EpisodeMapper
      )
      toCollect = MappedCursorLiveData(
        context,
        Episodes.fromShow(showId),
        EpisodeMapper.projection,
        EpisodeColumns.IN_COLLECTION + "=0 AND " +
            EpisodeColumns.FIRST_AIRED + " IS NOT NULL AND " +
            EpisodeColumns.SEASON + ">0",
        null,
        EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1",
        EpisodeMapper
      )
      lastCollected = MappedCursorLiveData(
        context,
        Episodes.fromShow(showId),
        EpisodeMapper.projection,
        EpisodeColumns.IN_COLLECTION + "=1",
        null,
        EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1",
        EpisodeMapper
      )

      show.observeForever(showObserver)
    }
  }

  override fun onCleared() {
    show.removeObserver(showObserver)
    super.onCleared()
  }

  private val showObserver = Observer<Show> { show ->
    if (show != null) {
      viewModelScope.launch {
        val currentTime = System.currentTimeMillis()
        val needsSync = show.needsSync
        if (needsSync || currentTime > show.lastSync + SYNC_INTERVAL) {
          syncShow.invokeAsync(SyncShow.Params(show.traktId))
        }

        if (currentTime > show.lastCommentSync + SYNC_INTERVAL_COMMENTS) {
          syncShowComments.invokeAsync(SyncShowComments.Params(show.traktId))
        }

        if (show.lastSync > show.lastCreditsSync) {
          syncShowCredits.invokeAsync(SyncShowCredits.Params(show.traktId))
        }

        if (show.lastSync > show.lastRelatedSync) {
          syncRelatedShows.invokeAsync(SyncRelatedShows.Params(show.traktId))
        }
      }
    }
  }

  override suspend fun onRefresh() {
    val traktId = showHelper.getTraktId(showId)
    val tmdbId = showHelper.getTmdbId(showId)

    val showDeferred = syncShow.invokeAsync(SyncShow.Params(traktId))
    val relatedDeferred = syncRelatedShows.invokeAsync(SyncRelatedShows.Params(traktId))
    val imagesDeferred = syncShowImages.invokeAsync(SyncShowImages.Params(tmdbId))
    val commentsDeferred = syncShowComments.invokeAsync(SyncShowComments.Params(traktId))
    val creditsDeferred = syncShowCredits.invokeAsync(SyncShowCredits.Params(traktId))

    if (TraktLinkSettings.isLinked(context)) {
      val watchedDeferred = syncShowWatchedStatus.invokeAsync(SyncShowWatchedStatus.Params(traktId))
      val collectedDeferred =
        syncShowCollectedStatus.invokeAsync(SyncShowCollectedStatus.Params(traktId))
      watchedDeferred.await()
      collectedDeferred.await()
    }

    showDeferred.await()
    relatedDeferred.await()
    imagesDeferred.await()
    commentsDeferred.await()
    creditsDeferred.await()
    return
  }

  companion object {

    private const val SYNC_INTERVAL = 2 * DateUtils.DAY_IN_MILLIS
    private const val SYNC_INTERVAL_COMMENTS = 3 * DateUtils.HOUR_IN_MILLIS
  }
}
