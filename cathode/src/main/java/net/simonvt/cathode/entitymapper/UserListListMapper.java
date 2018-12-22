package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.UserList;

public class UserListListMapper implements MappedCursorLiveData.CursorMapper<List<UserList>> {

  @Override public List<UserList> map(Cursor cursor) {
    List<UserList> userLists = new ArrayList<>();
    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      userLists.add(UserListMapper.mapList(cursor));
    }
    return userLists;
  }
}
