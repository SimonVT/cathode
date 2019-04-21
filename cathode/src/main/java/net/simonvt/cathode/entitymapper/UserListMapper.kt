package net.simonvt.cathode.entitymapper

import android.database.Cursor
import android.text.TextUtils
import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns

object UserListMapper : MappedCursorLiveData.CursorMapper<UserList> {

  override fun map(cursor: Cursor): UserList? {
    return if (cursor.moveToFirst()) mapList(cursor) else null
  }

  fun mapList(cursor: Cursor): UserList {
    val id = cursor.getLong(ListsColumns.ID)
    val name = cursor.getString(ListsColumns.NAME)
    val description = cursor.getStringOrNull(ListsColumns.DESCRIPTION)
    val privacyString = cursor.getStringOrNull(ListsColumns.PRIVACY)
    val privacy: Privacy? = if (!TextUtils.isEmpty(privacyString)) {
      Privacy.fromValue(privacyString!!)
    } else {
      null
    }
    val displayNumbers = cursor.getBoolean(ListsColumns.DISPLAY_NUMBERS)
    val allowComments = cursor.getBoolean(ListsColumns.ALLOW_COMMENTS)
    val updatedAt = cursor.getLong(ListsColumns.UPDATED_AT)
    val likes = cursor.getInt(ListsColumns.LIKES)
    val slug = cursor.getStringOrNull(ListsColumns.SLUG)
    val traktId = cursor.getLong(ListsColumns.TRAKT_ID)

    return UserList(
      id,
      name,
      description,
      privacy,
      displayNumbers,
      allowComments,
      updatedAt,
      likes,
      slug,
      traktId
    )
  }

  val projection = arrayOf(
    ListsColumns.ID,
    ListsColumns.NAME,
    ListsColumns.DESCRIPTION,
    ListsColumns.PRIVACY,
    ListsColumns.DISPLAY_NUMBERS,
    ListsColumns.ALLOW_COMMENTS,
    ListsColumns.UPDATED_AT,
    ListsColumns.LIKES,
    ListsColumns.SLUG,
    ListsColumns.TRAKT_ID
  )
}
