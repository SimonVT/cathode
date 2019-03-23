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

package net.simonvt.cathode.ui.credits

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.movies.SyncMovieCredits
import net.simonvt.cathode.actions.movies.SyncMovieCredits.Params
import net.simonvt.cathode.actions.shows.SyncShowCredits
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class CreditsViewModel @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper,
  private val syncShowCredits: SyncShowCredits,
  private val syncMovieCredits: SyncMovieCredits
) : RefreshableViewModel() {

  private var itemType: ItemType? = null
  private var itemId: Long = 0

  lateinit var credits: LiveData<Credits>
    private set

  fun setItemTypeAndId(itemType: ItemType, itemId: Long) {
    if (this.itemType == null) {
      this.itemType = itemType
      this.itemId = itemId

      credits = CreditsLiveData(context, itemType, itemId)
    }
  }

  override suspend fun onRefresh() {
    when (itemType) {
      SHOW -> {
        val traktId = showHelper.getTraktId(itemId)
        ActionManager.invokeSync(
          SyncShowCredits.key(traktId),
          syncShowCredits,
          SyncShowCredits.Params(traktId)
        )
      }
      MOVIE -> {
        val traktId = movieHelper.getTraktId(itemId)
        ActionManager.invokeSync(
          SyncMovieCredits.key(traktId),
          syncMovieCredits,
          Params(traktId)
        )
      }
      else -> IllegalArgumentException("Illegal item type: $itemType")
    }
  }
}
