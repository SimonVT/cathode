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
import kotlinx.coroutines.async
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.comments.SyncEpisodeComments
import net.simonvt.cathode.actions.seasons.SyncSeason
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Comment
import net.simonvt.cathode.common.entity.Episode
import net.simonvt.cathode.entitymapper.CommentListMapper
import net.simonvt.cathode.entitymapper.CommentMapper
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class EpisodeViewModel @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncSeason: SyncSeason,
  private val syncEpisodeComments: SyncEpisodeComments
) : RefreshableViewModel() {

  private var episodeId = -1L

  lateinit var episode: LiveData<Episode>
    private set
  lateinit var userComments: LiveData<List<Comment>>
    private set
  lateinit var comments: LiveData<List<Comment>>
    private set

  fun setEpisodeId(episodeId: Long) {
    if (this.episodeId == -1L) {
      this.episodeId = episodeId

      episode = MappedCursorLiveData(
        context,
        Episodes.withId(episodeId),
        PROJECTION,
        null,
        null,
        null,
        EpisodeMapper()
      )
      userComments = MappedCursorLiveData(
        context,
        Comments.fromEpisode(episodeId),
        CommentMapper.PROJECTION,
        CommentColumns.IS_USER_COMMENT + "=1",
        null,
        null,
        CommentListMapper()
      )
      comments = MappedCursorLiveData(
        context,
        Comments.fromEpisode(episodeId),
        CommentMapper.PROJECTION,
        CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0",
        null,
        CommentColumns.LIKES + " DESC LIMIT 3",
        CommentListMapper()
      )

      episode.observeForever(episodeObserver)
    }
  }

  override fun onCleared() {
    episode.removeObserver(episodeObserver)
    super.onCleared()
  }

  private val episodeObserver = Observer<Episode> { episode ->
    viewModelScope.async {
      if (System.currentTimeMillis() > episode.lastCommentSync!! + SYNC_INTERVAL_COMMENTS) {
        val showId = episodeHelper.getShowId(episodeId)
        val traktId = showHelper.getTraktId(showId)
        ActionManager.invokeAsync(
          SyncEpisodeComments.key(traktId, episode.season, episode.episode),
          syncEpisodeComments,
          SyncEpisodeComments.Params(traktId, episode.season, episode.episode)
        )
      }
    }
  }

  override suspend fun onRefresh() {
    val showId = episodeHelper.getShowId(episodeId)
    val traktId = showHelper.getTraktId(showId)
    val season = episodeHelper.getSeason(episodeId)
    val number = episodeHelper.getNumber(episodeId)

    val seasonDeferred = ActionManager.invokeAsync(
      SyncSeason.key(traktId, season),
      syncSeason,
      SyncSeason.Params(traktId, season)
    )
    val commentsDeferred = ActionManager.invokeAsync(
      SyncEpisodeComments.key(traktId, season, number),
      syncEpisodeComments,
      SyncEpisodeComments.Params(traktId, season, number)
    )

    seasonDeferred.await()
    commentsDeferred.await()
  }

  companion object {

    private const val SYNC_INTERVAL_COMMENTS = 3 * DateUtils.HOUR_IN_MILLIS

    internal val PROJECTION = arrayOf(
      EpisodeColumns.ID,
      EpisodeColumns.TRAKT_ID,
      EpisodeColumns.TITLE,
      EpisodeColumns.OVERVIEW,
      EpisodeColumns.FIRST_AIRED,
      EpisodeColumns.WATCHED,
      EpisodeColumns.IN_COLLECTION,
      EpisodeColumns.IN_WATCHLIST,
      EpisodeColumns.WATCHING,
      EpisodeColumns.CHECKED_IN,
      EpisodeColumns.USER_RATING,
      EpisodeColumns.RATING,
      EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE,
      EpisodeColumns.LAST_COMMENT_SYNC,
      EpisodeColumns.SHOW_ID,
      EpisodeColumns.SEASON_ID,
      EpisodeColumns.SHOW_TITLE
    )
  }
}
