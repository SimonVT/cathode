package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Show;

public class ShowListMapper implements MappedCursorLiveData.CursorMapper<List<Show>> {

  @Override public List<Show> map(Cursor cursor) {
    List<Show> shows = new ArrayList<>();

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      shows.add(ShowMapper.mapShow(cursor));
    }

    return shows;
  }
}
