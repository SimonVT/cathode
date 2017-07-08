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

package net.simonvt.cathode.common.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) public class TextUtilsTest {

  @Test public void testWordCount() throws Exception {
    String empty = "";
    String specialChar = "!";
    String fiveWords = " This has exactly 5 words";
    String doubleSpace = "2  words";
    String doubleSpaceSpecialChars = "2 ! words";

    assertThat(TextUtils.wordCount(empty)).isEqualTo(0);
    assertThat(TextUtils.wordCount(specialChar)).isEqualTo(0);
    assertThat(TextUtils.wordCount(fiveWords)).isEqualTo(5);
    assertThat(TextUtils.wordCount(doubleSpace)).isEqualTo(2);
    assertThat(TextUtils.wordCount(doubleSpaceSpecialChars)).isEqualTo(2);
  }

  @Test public void testUpperCaseFirstLetter() throws Exception {
    assertThat(TextUtils.upperCaseFirstLetter(null)).isEqualTo(null);
    assertThat(TextUtils.upperCaseFirstLetter("")).isEqualTo("");
    assertThat(TextUtils.upperCaseFirstLetter("a")).isEqualTo("A");
    assertThat(TextUtils.upperCaseFirstLetter("A")).isEqualTo("A");
    assertThat(TextUtils.upperCaseFirstLetter("string string")).isEqualTo("String string");
  }
}
