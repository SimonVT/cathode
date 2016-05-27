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

package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;
import net.simonvt.schematic.Cursors;

public final class ListWrapper {

  private ListWrapper() {
  }

  public static long getId(ContentResolver resolver, long traktId) {
    Cursor c = resolver.query(Lists.LISTS, new String[] {
        ListsColumns.ID,
    }, ListsColumns.TRAKT_ID + "=?", new String[] {
        String.valueOf(traktId),
    }, null);

    long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, ListsColumns.ID);

    c.close();

    return id;
  }

  public static long getTraktId(ContentResolver resolver, long listId) {
    Cursor c = resolver.query(Lists.withId(listId), new String[] {
        ListsColumns.TRAKT_ID,
    }, null, null, null);

    long traktId = !c.moveToFirst() ? -1L : Cursors.getLong(c, ListsColumns.TRAKT_ID);

    c.close();

    return traktId;
  }

  public static long createList(ContentResolver resolver, String name, String description,
      Privacy privacy, boolean displayNumbers, boolean allowComments) {
    ContentValues values = new ContentValues();
    values.put(ListsColumns.NAME, name);
    values.put(ListsColumns.DESCRIPTION, description);
    values.put(ListsColumns.PRIVACY, privacy.toString());
    values.put(ListsColumns.DISPLAY_NUMBERS, displayNumbers);
    values.put(ListsColumns.ALLOW_COMMENTS, allowComments);
    values.put(ListsColumns.TRAKT_ID, -1L);

    Uri uri = resolver.insert(Lists.LISTS, values);
    final long listId = Lists.getId(uri);

    return listId;
  }

  public static long updateOrInsert(ContentResolver resolver, CustomList list) {
    final long traktId = list.getIds().getTrakt();
    long listId = getId(resolver, traktId);

    if (listId == -1L) {
      listId = Lists.getId(resolver.insert(Lists.LISTS, getValues(list)));
    } else {
      update(resolver, listId, list);
    }

    return listId;
  }

  public static void update(ContentResolver resolver, long listId, CustomList list) {
    resolver.update(Lists.withId(listId), getValues(list), null, null);
  }

  private static ContentValues getValues(CustomList list) {
    ContentValues cv = new ContentValues();

    cv.put(ListsColumns.NAME, list.getName());
    cv.put(ListsColumns.DESCRIPTION, list.getDescription());
    if (list.getPrivacy() != null) {
      cv.put(ListsColumns.PRIVACY, list.getPrivacy().toString());
    }
    cv.put(ListsColumns.DISPLAY_NUMBERS, list.getDisplayNumbers());
    cv.put(ListsColumns.ALLOW_COMMENTS, list.getAllowComments());
    if (list.getUpdatedAt() != null) {
      cv.put(ListsColumns.UPDATED_AT, list.getUpdatedAt().getTimeInMillis());
    }
    cv.put(ListsColumns.LIKES, list.getLikes());
    cv.put(ListsColumns.SLUG, list.getIds().getSlug());
    cv.put(ListsColumns.TRAKT_ID, list.getIds().getTrakt());

    return cv;
  }
}
