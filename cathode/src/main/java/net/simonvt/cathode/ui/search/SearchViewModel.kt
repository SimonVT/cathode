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

package net.simonvt.cathode.ui.search

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.data.StringMapper
import net.simonvt.cathode.provider.DatabaseContract.RecentQueriesColumns
import net.simonvt.cathode.provider.ProviderSchematic.RecentQueries
import net.simonvt.cathode.search.SearchHandler
import net.simonvt.cathode.search.SearchHandler.SearchResult
import javax.inject.Inject

class SearchViewModel @Inject constructor(
  context: Context,
  private val searchHandler: SearchHandler
) : ViewModel() {

  val recents: LiveData<List<String>>

  private val queryChannel = BroadcastChannel<String>(Channel.CONFLATED)
  val liveResults = queryChannel
    .asFlow()
    .debounce(DEBOUNCE_DELAY)
    .mapLatest { query ->
      if (query.isNotEmpty()) {
        searchHandler.search(query)
      } else {
        SearchResult(true, emptyList())
      }
    }
    .flowOn(Dispatchers.IO)
    .asLiveData(viewModelScope.coroutineContext)

  init {
    recents = MappedCursorLiveData(
      context,
      RecentQueries.RECENT_QUERIES,
      arrayOf(RecentQueriesColumns.QUERY),
      null,
      null,
      RecentQueriesColumns.QUERIED_AT + " DESC LIMIT 3", StringMapper(RecentQueriesColumns.QUERY)
    )
  }

  fun search(searchQuery: String) {
    queryChannel.offer(searchQuery)
  }

  companion object {
    const val DEBOUNCE_DELAY = 500L
  }
}
