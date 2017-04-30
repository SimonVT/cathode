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

package net.simonvt.cathode.api.util;

import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TimeUtilsTest {

  static final long DATE_MILLIS = 1000000000000L;

  @Before public void setup() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
  }

  @Test public void testDateParsing() throws Exception {
    assertThat(TimeUtils.getMillis("2001-09-09T01:46:40.000Z")).isEqualTo(DATE_MILLIS);
    assertThat(TimeUtils.getMillis("2001-09-09T01:46:40Z")).isEqualTo(DATE_MILLIS);
  }

  @Test public void testIsoTime() throws Exception {
    String isoTime = TimeUtils.getIsoTime(DATE_MILLIS);
    assertThat(isoTime).isEqualTo("2001-09-09T01:46:40.000Z");
  }

  @Test public void testGetMillis() throws Exception {
    final int year = 2017;
    final int month = 4;
    final int day = 9;
    final int hour = 12;
    final int minute = 30;

    assertThat(TimeUtils.getMillis(year, month, day, hour, minute)).isEqualTo(1494325800000L);
  }
}
