package net.simonvt.cathode.ui.lists;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;

public class DialogListItemListMapper
    implements MappedCursorLiveData.CursorMapper<List<DialogListItemListMapper.DialogListItem>> {

  public static class DialogListItem {

    long id;
    long listId;

    public DialogListItem(long id, long listId) {
      this.id = id;
      this.listId = listId;
    }

    public long getId() {
      return id;
    }

    public long getListId() {
      return listId;
    }
  }

  static final String[] PROJECTION = {
      ListItemColumns.ID, ListItemColumns.LIST_ID,
  };

  @Override public List<DialogListItem> map(Cursor cursor) {
    List<DialogListItem> listItems = new ArrayList<>();
    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      listItems.add(mapItem(cursor));
    }
    return listItems;
  }

  static DialogListItem mapItem(Cursor cursor) {
    long id = Cursors.getLong(cursor, ListItemColumns.ID);
    long listId = Cursors.getLong(cursor, ListItemColumns.LIST_ID);
    return new DialogListItem(id, listId);
  }
}
