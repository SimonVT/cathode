/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.api.entity.Profile
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.UserColumns
import net.simonvt.cathode.provider.ProviderSchematic.Users
import net.simonvt.cathode.provider.query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDatabaseHelper @Inject constructor(private val context: Context) {

  class IdResult(var id: Long, var didCreate: Boolean)

  fun getId(username: String): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Users.USERS,
        arrayOf(UserColumns.ID),
        UserColumns.USERNAME + "=?",
        arrayOf(username)
      )
      val id = if (c.moveToFirst()) c.getLong(UserColumns.ID) else -1L
      c.close()
      return id
    }
  }

  fun updateOrCreate(profile: Profile): IdResult {
    synchronized(LOCK_ID) {
      var id = getId(profile.username)

      if (id == -1L) {
        id = create(profile)
        return IdResult(id, true)
      } else {
        update(id, profile)
        return IdResult(id, false)
      }
    }
  }

  private fun create(profile: Profile): Long {
    val values = getValues(profile)
    return Users.getUserId(context.contentResolver.insert(Users.USERS, values)!!)
  }

  private fun update(id: Long, profile: Profile) {
    val values = getValues(profile)
    context.contentResolver.update(Users.withId(id), values, null, null)
  }

  private fun getValues(profile: Profile): ContentValues {
    val values = ContentValues()
    values.put(UserColumns.USERNAME, profile.username)
    values.put(UserColumns.IS_PRIVATE, profile.isPrivate)
    values.put(UserColumns.NAME, profile.name)
    values.put(UserColumns.VIP, profile.vip)
    values.put(UserColumns.VIP_EP, profile.vip_ep)
    values.put(UserColumns.JOINED_AT, profile.joined_at?.timeInMillis)
    values.put(UserColumns.LOCATION, profile.location)
    values.put(UserColumns.ABOUT, profile.about)
    values.put(UserColumns.GENDER, profile.gender?.toString())
    values.put(UserColumns.AGE, profile.age)
    values.put(UserColumns.AVATAR, profile.images?.avatar?.full)
    return values
  }

  companion object {

    private val LOCK_ID = Any()
  }
}
