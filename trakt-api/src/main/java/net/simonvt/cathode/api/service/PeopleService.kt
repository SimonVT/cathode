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

import net.simonvt.cathode.api.entity.Credits
import net.simonvt.cathode.api.entity.Person
import net.simonvt.cathode.api.enumeration.Extended
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PeopleService {

  @GET("/people/{id}")
  fun summary(
    @Path("id") traktId: Long,
    @Query("extended") extended: Extended? = null
  ): Call<Person>

  @GET("/people/{id}/shows")
  fun shows(
    @Path("id") traktId: Long,
    @Query("extended") extended: Extended? = null
  ): Call<Credits>

  @GET("/people/{id}/movies")
  fun movies(
    @Path("id") traktId: Long,
    @Query("extended") extended: Extended? = null
  ): Call<Credits>
}
