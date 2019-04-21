package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.ListItem

object ListItemListMapper : MappedCursorLiveData.CursorMapper<List<ListItem>> {

  override fun map(cursor: Cursor): List<ListItem> {
    val listItems = mutableListOf<ListItem>()
    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
      listItems.add(ListItemMapper.mapItem(cursor))
    }
    return listItems
  }
}
