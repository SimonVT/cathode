package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.ListItem;

public class ListItemListMapper implements MappedCursorLiveData.CursorMapper<List<ListItem>> {

  @Override public List<ListItem> map(Cursor cursor) {
    List<ListItem> listItems = new ArrayList<>();
    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      listItems.add(ListItemMapper.mapItem(cursor));
    }
    return listItems;
  }
}
