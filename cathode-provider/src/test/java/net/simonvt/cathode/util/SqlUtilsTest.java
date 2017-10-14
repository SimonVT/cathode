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

package net.simonvt.cathode.util;

import android.database.sqlite.SQLiteDatabase;
import net.simonvt.cathode.provider.util.SqlUtils;
import net.simonvt.schematic.annotation.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) public class SqlUtilsTest {

  @Test public void testCreateColumnIfNotExists() throws Exception {
    TestDatabase helper = new TestDatabase(RuntimeEnvironment.application);

    SQLiteDatabase db = helper.getWritableDatabase();

    db.execSQL("CREATE TABLE testTable (_id INTEGER PRIMARY KEY AUTOINCREMENT)");
    assertThat(
        SqlUtils.createColumnIfNotExists(db, "testTable", "testColumn", DataType.Type.INTEGER, "0"))
        .isTrue();
    assertThat(
        SqlUtils.createColumnIfNotExists(db, "testTable", "testColumn", DataType.Type.INTEGER, "0"))
        .isFalse();
  }
}
