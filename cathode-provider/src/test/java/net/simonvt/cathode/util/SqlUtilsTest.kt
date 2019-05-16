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

package net.simonvt.cathode.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.provider.util.SqlUtils
import net.simonvt.schematic.annotation.DataType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SqlUtilsTest {

  @Test
  @Throws(Exception::class)
  fun testCreateColumnIfNotExists() {
    val helper = TestDatabase(ApplicationProvider.getApplicationContext<Context>())

    val db = helper.writableDatabase

    db.execSQL("CREATE TABLE testTable (_id INTEGER PRIMARY KEY AUTOINCREMENT)")
    assertThat(
      SqlUtils.createColumnIfNotExists(db, "testTable", "testColumn", DataType.Type.INTEGER, "0")
    ).isTrue()
    assertThat(
      SqlUtils.createColumnIfNotExists(db, "testTable", "testColumn", DataType.Type.INTEGER, "0")
    ).isFalse()
  }
}
