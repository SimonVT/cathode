/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.CastMember;
import net.simonvt.cathode.common.entity.Person;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;

public class ShowCastMapper implements MappedCursorLiveData.CursorMapper<List<CastMember>> {

  public static final String[] PROJECTION = new String[] {
      Tables.SHOW_CAST + "." + ShowCastColumns.ID,
      Tables.SHOW_CAST + "." + ShowCastColumns.CHARACTER,
      Tables.SHOW_CAST + "." + ShowCastColumns.PERSON_ID, Tables.PEOPLE + "." + PersonColumns.NAME,
  };

  @Override public List<CastMember> map(Cursor cursor) {
    List<CastMember> castMembers = new ArrayList<>();

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      long personId = Cursors.getLong(cursor, ShowCastColumns.PERSON_ID);
      String personName = Cursors.getString(cursor, PersonColumns.NAME);
      Person person = new Person(personId, personName, null, null, null, null, null);

      String character = Cursors.getString(cursor, ShowCastColumns.CHARACTER);
      castMembers.add(new CastMember(character, person));
    }

    return castMembers;
  }
}
