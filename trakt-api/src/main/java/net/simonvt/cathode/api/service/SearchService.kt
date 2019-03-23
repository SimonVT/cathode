/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.service

import net.simonvt.cathode.api.entity.SearchResult
import net.simonvt.cathode.api.enumeration.Enums
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.enumeration.ItemType
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchService {

  /**
   * Queries will search fields like the title and description.
   */
  @GET("/search/{type}")
  fun search(
    @Path("type") types: Enums<ItemType>,
    @Query("query") query: String,
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<SearchResult>>

  companion object {
    const val LIMIT = 100
  }
}
