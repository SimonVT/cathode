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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

  private val job = GlobalScope.launch(Dispatchers.Main) {
    start()
  }

  val query = Channel<String>(RENDEZVOUS)
  val recents: LiveData<List<String>>
  private val _liveResults = MutableLiveData<SearchResult>()
  val liveResults: LiveData<SearchResult> get() = _liveResults

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

  private suspend fun start() = coroutineScope {
    launch {
      var query: String
      var searchJob: Job? = null
      this@SearchViewModel.query.consumeEach {
        query = it
        searchJob?.cancel()
        searchJob = launch(Dispatchers.IO) {
          delay(DEBOUNCE_DELAY)
          if (query.isNotEmpty()) {
            val result = searchHandler.search(query)
            _liveResults.postValue(result)
          } else {
            _liveResults.postValue(SearchResult(true, emptyList()))
          }
        }
      }
    }
  }

  fun search(searchQuery: String) {
    query.offer(searchQuery.trim())
  }

  override fun onCleared() {
    super.onCleared()
    job.cancel()
  }

  companion object {
    const val DEBOUNCE_DELAY = 300L
  }
}
