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

import android.content.Context;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.YEAR_IN_MILLIS;
import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml")
public class DateStringUtilsTest {

  private static final long FROM_MILLIS = 1000000000000L;
  private static final long NEXT_YEAR = 9839600000L;

  @Before public void setup() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
  }

  private void assertUpdateInterval(long airtimeMillis, long expectedInterval) {
    final long interval = DateStringUtils.timeUntilUpdate(FROM_MILLIS, airtimeMillis);
    assertThat(interval).isEqualTo(expectedInterval);
  }

  @Test public void testGetTimeString() throws Exception {
    String timeString = DateStringUtils.getTimeString(FROM_MILLIS, false);
    assertThat(timeString).isEqualTo("3:46 am");

    timeString = DateStringUtils.getTimeString(FROM_MILLIS + 12 * HOUR_IN_MILLIS, false);
    assertThat(timeString).isEqualTo("3:46 pm");
  }

  @Test public void testGetTimeString24h() throws Exception {
    String timeString = DateStringUtils.getTimeString(FROM_MILLIS, true);
    assertThat(timeString).isEqualTo("3:46");

    timeString = DateStringUtils.getTimeString(FROM_MILLIS + 12 * HOUR_IN_MILLIS, true);
    assertThat(timeString).isEqualTo("15:46");
  }

  @Test public void testGetDateString() throws Exception {
    final String dateString = DateStringUtils.getDateString(FROM_MILLIS);
    assertThat(dateString).isEqualTo("Sunday, September 9");
  }

  final void assertInterval(long airTime, String expected) {
    final Context context = RuntimeEnvironment.application;
    final String airtimeString =
        DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, false, false);
    assertThat(airtimeString).isEqualTo(expected);
  }

  final void assertInterval24h(long airTime, String expected) {
    final Context context = RuntimeEnvironment.application;
    final String airtimeString =
        DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, false, true);
    assertThat(airtimeString).isEqualTo(expected);
  }

  final void assertIntervalExtended(long airTime, String expected) {
    final Context context = RuntimeEnvironment.application;
    final String airtimeString =
        DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, true, false);
    assertThat(airtimeString).isEqualTo(expected);
  }

  final void assertIntervalExtended24h(long airTime, String expected) {
    final Context context = RuntimeEnvironment.application;
    final String airtimeString =
        DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, true, true);
    assertThat(airtimeString).isEqualTo(expected);
  }

  @Test public void testAirdateInterval() throws Exception {
    assertInterval(0L, "Unknown airdate");
    assertUpdateInterval(0L, NEXT_YEAR);

    assertInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now");
    assertUpdateInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, 1L);

    assertInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now");
    assertUpdateInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, 4 * MINUTE_IN_MILLIS + 1L);

    assertInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago");
    assertUpdateInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, 1L);

    assertInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes");
    assertUpdateInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, 1L);

    assertInterval(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago");
    assertUpdateInterval(FROM_MILLIS - HOUR_IN_MILLIS + 1L, MINUTE_IN_MILLIS - 1L);

    assertInterval(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes");
    assertUpdateInterval(FROM_MILLIS + HOUR_IN_MILLIS - 1L, MINUTE_IN_MILLIS - 1L);

    assertInterval(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago");
    assertUpdateInterval(FROM_MILLIS - HOUR_IN_MILLIS, 1L);

    assertInterval(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour");
    assertUpdateInterval(FROM_MILLIS + HOUR_IN_MILLIS, 1L);

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago");
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS + 1L, HOUR_IN_MILLIS - 1L);

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours");
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS - 1L, HOUR_IN_MILLIS - 1L);

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago");
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS, 1L);

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours");
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS, 1L);

    assertInterval(FROM_MILLIS - YEAR_IN_MILLIS, "2000");
    assertUpdateInterval(FROM_MILLIS - YEAR_IN_MILLIS, NEXT_YEAR);

    assertInterval(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "1999");
    assertUpdateInterval(FROM_MILLIS - 2 * YEAR_IN_MILLIS, NEXT_YEAR);

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8");
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS - 1L, NEXT_YEAR);

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10");
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS + 1L, 1L);

    assertInterval(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002");
    assertUpdateInterval(FROM_MILLIS + YEAR_IN_MILLIS, NEXT_YEAR);

    assertInterval(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003");
    assertUpdateInterval(FROM_MILLIS + 2 * YEAR_IN_MILLIS, NEXT_YEAR);
  }

  @Test public void testAirdateIntervalExtended() throws Exception {
    assertIntervalExtended(0L, "Unknown airdate");
    assertIntervalExtended(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now");
    assertIntervalExtended(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now");
    assertIntervalExtended(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago");
    assertIntervalExtended(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes");
    assertIntervalExtended(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago");
    assertIntervalExtended(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes");
    assertIntervalExtended(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago");
    assertIntervalExtended(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour");
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago");
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours");
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago");
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours");

    assertIntervalExtended(FROM_MILLIS - YEAR_IN_MILLIS, "Sep 10, 2000 3:46 am");
    assertIntervalExtended(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "Sep 12, 1999 3:46 am");
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8 3:46 am");
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10 3:46 am");
    assertIntervalExtended(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002 3:46 am");
    assertIntervalExtended(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003 3:46 am");

    assertIntervalExtended(FROM_MILLIS - YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 10, 2000 3:46 pm");
    assertIntervalExtended(FROM_MILLIS - 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 12, 1999 3:46 pm");
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS - 12 * HOUR_IN_MILLIS, "Sep 7 3:46 pm");
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS + 12 * HOUR_IN_MILLIS, "Sep 10 3:46 pm");
    assertIntervalExtended(FROM_MILLIS + YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 8, 2002 3:46 pm");
    assertIntervalExtended(FROM_MILLIS + 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 7, 2003 3:46 pm");
  }

  @Test public void testAirdateInterval24HourFormat() throws Exception {
    assertInterval24h(0L, "Unknown airdate");
    assertInterval24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now");
    assertInterval24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now");
    assertInterval24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago");
    assertInterval24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes");
    assertInterval24h(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago");
    assertInterval24h(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes");
    assertInterval24h(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago");
    assertInterval24h(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour");
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago");
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours");
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago");
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours");

    assertInterval24h(FROM_MILLIS - YEAR_IN_MILLIS, "2000");
    assertInterval24h(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "1999");
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8");
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10");
    assertInterval24h(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002");
    assertInterval24h(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003");
  }

  @Test public void testAirdateIntervalExtended24HourFormat() throws Exception {
    assertIntervalExtended24h(0L, "Unknown airdate");
    assertIntervalExtended24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now");
    assertIntervalExtended24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now");
    assertIntervalExtended24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago");
    assertIntervalExtended24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes");
    assertIntervalExtended24h(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago");
    assertIntervalExtended24h(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes");
    assertIntervalExtended24h(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago");
    assertIntervalExtended24h(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour");
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago");
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours");
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago");
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours");

    assertIntervalExtended24h(FROM_MILLIS - YEAR_IN_MILLIS, "Sep 10, 2000 3:46");
    assertIntervalExtended24h(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "Sep 12, 1999 3:46");
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8 3:46");
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10 3:46");
    assertIntervalExtended24h(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002 3:46");
    assertIntervalExtended24h(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003 3:46");

    assertIntervalExtended24h(FROM_MILLIS - YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 10, 2000 15:46");
    assertIntervalExtended24h(FROM_MILLIS - 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 12, 1999 15:46");
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS - 12 * HOUR_IN_MILLIS, "Sep 7 15:46");
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS + 12 * HOUR_IN_MILLIS, "Sep 10 15:46");
    assertIntervalExtended24h(FROM_MILLIS + YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 8, 2002 15:46");
    assertIntervalExtended24h(FROM_MILLIS + 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
        "Sep 7, 2003 15:46");
  }

  @Test public void testStatsString() throws Exception {
    final long hourInMinutes = 60;
    final long dayInMinutes = hourInMinutes * 24;

    final long zeroMinutes = 0L;
    final String zeroMinutesString =
        DateStringUtils.getStatsString(RuntimeEnvironment.application, zeroMinutes);
    assertThat(zeroMinutesString).isEqualTo("0m");

    final long thirtyMinutes = 30L;
    final String thirtyMinutesString =
        DateStringUtils.getStatsString(RuntimeEnvironment.application, thirtyMinutes);
    assertThat(thirtyMinutesString).isEqualTo("30m");

    final long sixHoursThirtyMinutes = 6 * hourInMinutes + 30;
    final String sixHoursThirtyMinutesString =
        DateStringUtils.getStatsString(RuntimeEnvironment.application, sixHoursThirtyMinutes);
    assertThat(sixHoursThirtyMinutesString).isEqualTo("6h 30m");

    final long twoDaysSixHoursThirtyMinutes = 2 * dayInMinutes + 6 * hourInMinutes + 30;
    final String twoDaysSixHoursThirtyMinutesString =
        DateStringUtils.getStatsString(RuntimeEnvironment.application,
            twoDaysSixHoursThirtyMinutes);
    assertThat(twoDaysSixHoursThirtyMinutesString).isEqualTo("2d 6h 30m");
  }
}
