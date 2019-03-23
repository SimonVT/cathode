/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.actions.shows

import android.content.Context
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.services.TvService
import net.simonvt.cathode.actions.TmdbCallAction
import net.simonvt.cathode.actions.shows.SyncShowImages.Params
import net.simonvt.cathode.images.ShowRequestHandler
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncShowImages @Inject constructor(
  private val context: Context,
  private val tvService: TvService,
  private val showHelper: ShowDatabaseHelper
) : TmdbCallAction<Params, TvShow>() {

  override fun getCall(params: Params): Call<TvShow> = tvService.tv(params.tmdbId, "en")

  override suspend fun handleResponse(params: Params, response: TvShow) {
    val showId = showHelper.getIdFromTmdb(params.tmdbId)
    ShowRequestHandler.retainImages(context, showId, response)
  }

  data class Params(val tmdbId: Int)

  companion object {

    fun key(tmdbId: Int): String {
      return "SyncShowImages&tmdbId=$tmdbId"
    }
  }
}
