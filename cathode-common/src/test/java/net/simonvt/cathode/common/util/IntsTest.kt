/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntsTest {

  @Test
  fun testToRange() {
    assertThat(listOf(-1).toRanges()).isEqualTo("-1")
    assertThat(listOf(-3, -2, -1).toRanges()).isEqualTo("(-3)-(-1)")
    assertThat(listOf(-1, 0, 1, 2).toRanges()).isEqualTo("(-1)-2")
    assertThat(listOf(1).toRanges()).isEqualTo("1")
    assertThat(listOf(1, 2, 3).toRanges()).isEqualTo("1-3")
    assertThat(listOf(3, 2, 1).toRanges()).isEqualTo("1-3")
    assertThat(listOf(1, 2, 3, 5).toRanges()).isEqualTo("1-3, 5")
    assertThat(listOf(1, 3, 5).toRanges()).isEqualTo("1, 3, 5")
    assertThat(listOf(1, 2, 3, 5, 6, 7).toRanges()).isEqualTo("1-3, 5-7")
    assertThat(listOf(1, 2, 3, 5, 7, 8, 9).toRanges()).isEqualTo("1-3, 5, 7-9")
  }
}
