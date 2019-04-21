package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Show

object ShowListMapper : MappedCursorLiveData.CursorMapper<List<Show>> {

  override fun map(cursor: Cursor): List<Show> {
    val shows = mutableListOf<Show>()
    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
      shows.add(ShowMapper.mapShow(cursor))
    }
    return shows
  }
}
