package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import android.text.TextUtils;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;

public class UserListMapper implements MappedCursorLiveData.CursorMapper<UserList> {

  public static final String[] PROJECTION = new String[] {
      ListsColumns.ID, ListsColumns.NAME, ListsColumns.DESCRIPTION, ListsColumns.PRIVACY,
      ListsColumns.DISPLAY_NUMBERS, ListsColumns.ALLOW_COMMENTS, ListsColumns.UPDATED_AT,
      ListsColumns.LIKES, ListsColumns.SLUG, ListsColumns.TRAKT_ID
  };

  @Override public UserList map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapList(cursor);
    }

    return null;
  }

  static UserList mapList(Cursor cursor) {
    long id = Cursors.getLong(cursor, ListsColumns.ID);
    String name = Cursors.getString(cursor, ListsColumns.NAME);
    String description = Cursors.getStringOrNull(cursor, ListsColumns.DESCRIPTION);
    Privacy privacy = null;
    String privacyString = Cursors.getStringOrNull(cursor, ListsColumns.PRIVACY);
    if (!TextUtils.isEmpty(privacyString)) {
      privacy = Privacy.fromValue(privacyString);
    }
    Boolean displayNumbers = Cursors.getBooleanOrNull(cursor, ListsColumns.DISPLAY_NUMBERS);
    Boolean allowComments = Cursors.getBooleanOrNull(cursor, ListsColumns.ALLOW_COMMENTS);
    Long updatedAt = Cursors.getLongOrNull(cursor, ListsColumns.UPDATED_AT);
    Integer likes = Cursors.getIntOrNull(cursor, ListsColumns.LIKES);
    String slug = Cursors.getStringOrNull(cursor, ListsColumns.SLUG);
    Long traktId = Cursors.getLongOrNull(cursor, ListsColumns.TRAKT_ID);

    return new UserList(id, name, description, privacy, displayNumbers, allowComments, updatedAt,
        likes, slug, traktId);
  }
}
