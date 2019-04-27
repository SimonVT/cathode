/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.sync.trakt

import android.content.Context
import net.simonvt.cathode.api.body.ListInfoBody
import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.event.ErrorEvent
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.helper.ListDatabaseHelper
import net.simonvt.cathode.remote.sync.SyncUserActivity
import net.simonvt.cathode.sync.R
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class UserList @Inject constructor(
  private val context: Context,
  private val usersServie: UsersService,
  private val jobManager: JobManager
) {

  fun create(
    name: String,
    description: String,
    privacy: Privacy,
    displayNumbers: Boolean,
    allowComments: Boolean,
    sortBy: SortBy,
    sortOrientation: SortOrientation
  ): Boolean {
    try {
      val call = usersServie.createList(
        ListInfoBody(
          name, description, privacy, displayNumbers, allowComments, sortBy,
          sortOrientation
        )
      )
      val response = call.execute()
      if (response.isSuccessful) {
        val userList = response.body()
        ListDatabaseHelper.updateOrInsert(context.contentResolver, userList!!)
        jobManager.addJob(SyncUserActivity())
        return true
      }
    } catch (e: IOException) {
      Timber.d(e, "Unable to create list %s", name)
    }

    ErrorEvent.post(context.getString(R.string.list_create_error, name))
    return false
  }

  fun update(
    traktId: Long,
    name: String,
    description: String,
    privacy: Privacy,
    displayNumbers: Boolean,
    allowComments: Boolean,
    sortBy: SortBy,
    sortOrientation: SortOrientation
  ): Boolean {
    try {
      val call = usersServie.updateList(
        traktId,
        ListInfoBody(
          name, description, privacy, displayNumbers, allowComments, sortBy,
          sortOrientation
        )
      )
      val response = call.execute()
      if (response.isSuccessful) {
        val userList = response.body()
        ListDatabaseHelper.updateOrInsert(context.contentResolver, userList!!)
        jobManager.addJob(SyncUserActivity())
        return true
      }
    } catch (e: IOException) {
      Timber.d(e, "Unable to create list %s", name)
    }

    ErrorEvent.post(context.getString(R.string.list_update_error, name))
    return false
  }

  fun delete(traktId: Long, name: String): Boolean {
    try {
      val call = usersServie.deleteList(traktId)
      val response = call.execute()
      if (response.isSuccessful) {
        val body = response.body()
        jobManager.addJob(SyncUserActivity())
        return true
      }
    } catch (e: IOException) {
      Timber.d(e, "Unable to delete list %s", name)
    }

    ErrorEvent.post(context.getString(R.string.list_delete_error, name))
    return false
  }
}
