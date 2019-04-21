package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.UserList

object UserListListMapper : MappedCursorLiveData.CursorMapper<List<UserList>> {

  override fun map(cursor: Cursor): List<UserList> {
    val userLists = mutableListOf<UserList>()
    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
      userLists.add(UserListMapper.mapList(cursor))
    }
    return userLists
  }
}
