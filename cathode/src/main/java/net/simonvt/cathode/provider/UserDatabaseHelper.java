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
import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Users;

public class UserDatabaseHelper {

  private static UserDatabaseHelper instance;

  public static UserDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (UserDatabaseHelper.class) {
        if (instance == null) {
          instance = new UserDatabaseHelper(context);
        }
      }
    }
    return instance;
  }

  private static final Object LOCK_ID = new Object();

  private Context context;

  private ContentResolver resolver;

  private UserDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    CathodeApp.inject(context, this);
  }

  public static final class IdResult {

    public long id;

    public boolean didCreate;

    public IdResult(long id, boolean didCreate) {
      this.id = id;
      this.didCreate = didCreate;
    }
  }

  public long getId(String username) {
    synchronized (LOCK_ID) {
      Cursor c = resolver.query(Users.USERS, new String[] {
          UserColumns.ID,
      }, UserColumns.USERNAME + "=?", new String[] {
          username,
      }, null);

      long id = -1L;

      if (c.moveToFirst()) {
        id = c.getLong(c.getColumnIndex(UserColumns.ID));
      }

      c.close();

      return id;
    }
  }

  public IdResult updateOrCreate(Profile profile) {
    synchronized (LOCK_ID) {
      long id = getId(profile.getUsername());

      if (id == -1L) {
        id = create(profile);
        return new IdResult(id, true);
      } else {
        update(id, profile);
        return new IdResult(id, false);
      }
    }
  }

  private long create(Profile profile) {
    ContentValues values = getValues(profile);
    return Users.getUserId(resolver.insert(Users.USERS, values));
  }

  private void update(long id, Profile profile) {
    ContentValues values = getValues(profile);
    resolver.update(Users.withId(id), values, null, null);
  }

  public static ContentValues getValues(Profile profile) {
    ContentValues values = new ContentValues();

    values.put(UserColumns.USERNAME, profile.getUsername());
    values.put(UserColumns.IS_PRIVATE, profile.isPrivate());
    values.put(UserColumns.NAME, profile.getName());
    values.put(UserColumns.VIP, profile.isVip());
    values.put(UserColumns.VIP_EP, profile.isVipEP());
    if (profile.getJoinedAt() != null) {
      values.put(UserColumns.JOINED_AT, profile.getJoinedAt().getTimeInMillis());
    }
    values.put(UserColumns.LOCATION, profile.getLocation());
    values.put(UserColumns.ABOUT, profile.getAbout());
    if (profile.getGender() != null) {
      values.put(UserColumns.GENDER, profile.getGender().toString());
    }
    values.put(UserColumns.AGE, profile.getAge());
    if (profile.getImages() != null && profile.getImages().getAvatar() != null) {
      values.put(UserColumns.AVATAR, profile.getImages().getAvatar().getFull());
    }

    return values;
  }
}
