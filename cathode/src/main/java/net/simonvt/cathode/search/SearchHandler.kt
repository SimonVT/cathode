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
package net.simonvt.cathode.search

import android.text.TextUtils
import android.text.format.DateUtils
import net.simonvt.cathode.api.enumeration.Enums
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.service.SearchService
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SearchHandler @Inject constructor(
  private val searchService: SearchService,
  private val showHelper: ShowDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper
) {

  data class SearchItem(
    val itemType: ItemType,
    val itemId: Long,
    val title: String?,
    val overview: String?,
    val rating: Float,
    val relevance: Int
  )

  data class SearchResult(val success: Boolean, val results: List<SearchItem>? = null)

  private val cache = mutableMapOf<String, List<SearchItem>>()

  fun search(query: String): SearchResult {
    MainHandler.removeCallbacks(pruneRunnable)
    val types = Enums(ItemType.SHOW, ItemType.MOVIE)
    try {
      val cachedResults = cache[query]
      if (cachedResults != null) {
        return SearchResult(true, cachedResults.toList())
      }

      val call = searchService.search(types, query, extended = Extended.FULL)
      val response = call.execute()

      if (response.isSuccessful) {
        var relevance = 0
        val searchResults = response.body()
        val results = mutableListOf<SearchItem>()

        searchResults?.take(RESULT_LIMIT)
          ?.filter { !it.show?.title.isNullOrBlank() || !it.movie?.title.isNullOrBlank() }
          ?.forEach { searchResult ->
            if (searchResult.type == ItemType.SHOW) {
              val show = searchResult.show!!
              if (!TextUtils.isEmpty(show.title)) {
                val showId = showHelper.partialUpdate(show)
                val result = SearchItem(
                  ItemType.SHOW, showId, show.title, show.overview, show.rating
                    ?: 0.0f, relevance++
                )
                results.add(result)
              }
            } else if (searchResult.type == ItemType.MOVIE) {
              val movie = searchResult.movie!!
              if (!TextUtils.isEmpty(movie.title)) {
                val movieId = movieHelper.partialUpdate(movie)

                val title = movie.title
                val overview = movie.overview
                val rating = movie.rating ?: 0.0f

                val result =
                  SearchItem(ItemType.MOVIE, movieId, title, overview, rating, relevance++)
                results.add(result)
              }
            }
          }

        cache[query] = results.toList()

        return SearchResult(true, results.toList())
      }
    } catch (e: IOException) {
      Timber.d(e, "Search failed")
    } catch (t: Throwable) {
      Timber.e(t, "Search failed")
    } finally {
      MainHandler.postDelayed(pruneRunnable, PRUNE_DELAY)
    }

    return SearchResult(false)
  }

  private val pruneRunnable = Runnable { cache.clear() }

  companion object {
    const val RESULT_LIMIT = 50
    const val PRUNE_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS
  }
}
