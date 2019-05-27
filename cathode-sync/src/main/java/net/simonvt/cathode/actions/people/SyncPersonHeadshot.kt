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

package net.simonvt.cathode.actions.people

import android.content.ContentValues
import android.content.Context
import com.uwetrottmann.tmdb2.entities.PersonImages
import com.uwetrottmann.tmdb2.services.PeopleService
import net.simonvt.cathode.actions.TmdbCallAction
import net.simonvt.cathode.actions.people.SyncPersonHeadshot.Params
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns
import net.simonvt.cathode.provider.ProviderSchematic.People
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.update
import retrofit2.Call
import javax.inject.Inject

class SyncPersonHeadshot @Inject constructor(
  private val context: Context,
  private val personHelper: PersonDatabaseHelper,
  private val peopleService: PeopleService
) : TmdbCallAction<Params, PersonImages>() {

  override fun key(params: Params): String = "SyncPersonHeadshot&tmdbId=${params.tmdbId}"

  override fun getCall(params: Params): Call<PersonImages> = peopleService.images(params.tmdbId)

  override suspend fun handleResponse(params: Params, response: PersonImages) {
    val personId = personHelper.getIdFromTmdb(params.tmdbId)

    val values = ContentValues()

    if (response.profiles!!.size > 0) {
      val still = response.profiles!![0]
      val path = ImageUri.create(ImageType.STILL, still.file_path)
      values.put(PersonColumns.HEADSHOT, path)
    } else {
      values.putNull(PersonColumns.HEADSHOT)
    }

    context.contentResolver.update(People.withId(personId), values)
  }

  data class Params(val tmdbId: Int)
}
