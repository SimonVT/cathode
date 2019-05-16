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

import android.content.Context
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.HOUR_IN_MILLIS
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.text.format.DateUtils.YEAR_IN_MILLIS
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml")
class DateStringUtilsTest {

  @Before
  fun setup() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"))
  }

  private fun assertUpdateInterval(airtimeMillis: Long, expectedInterval: Long) {
    val interval = DateStringUtils.timeUntilUpdate(FROM_MILLIS, airtimeMillis)
    assertThat(interval).isEqualTo(expectedInterval)
  }

  @Test
  fun testGetTimeString() {
    var timeString = DateStringUtils.getTimeString(FROM_MILLIS, false)
    assertThat(timeString).isEqualTo("3:46 am")

    timeString = DateStringUtils.getTimeString(FROM_MILLIS + 12 * HOUR_IN_MILLIS, false)
    assertThat(timeString).isEqualTo("3:46 pm")
  }

  @Test
  fun testGetTimeString24h() {
    var timeString = DateStringUtils.getTimeString(FROM_MILLIS, true)
    assertThat(timeString).isEqualTo("3:46")

    timeString = DateStringUtils.getTimeString(FROM_MILLIS + 12 * HOUR_IN_MILLIS, true)
    assertThat(timeString).isEqualTo("15:46")
  }

  @Test
  fun testGetDateString() {
    val dateString = DateStringUtils.getDateString(FROM_MILLIS)
    assertThat(dateString).isEqualTo("Sunday, September 9")
  }

  private fun assertInterval(airTime: Long, expected: String) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val airtimeString =
      DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, false, false)
    assertThat(airtimeString).isEqualTo(expected)
  }

  private fun assertInterval24h(airTime: Long, expected: String) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val airtimeString =
      DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, false, true)
    assertThat(airtimeString).isEqualTo(expected)
  }

  private fun assertIntervalExtended(airTime: Long, expected: String) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val airtimeString =
      DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, true, false)
    assertThat(airtimeString).isEqualTo(expected)
  }

  private fun assertIntervalExtended24h(airTime: Long, expected: String) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val airtimeString =
      DateStringUtils.getAirdateInterval(context, airTime, FROM_MILLIS, true, true)
    assertThat(airtimeString).isEqualTo(expected)
  }

  @Test
  fun testAirdateInterval() {
    assertInterval(0L, "Unknown airdate")
    assertUpdateInterval(0L, NEXT_YEAR)

    assertInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now")
    assertUpdateInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, 1L)

    assertInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now")
    assertUpdateInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, 4 * MINUTE_IN_MILLIS + 1L)

    assertInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago")
    assertUpdateInterval(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, 1L)

    assertInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes")
    assertUpdateInterval(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, 1L)

    assertInterval(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago")
    assertUpdateInterval(FROM_MILLIS - HOUR_IN_MILLIS + 1L, MINUTE_IN_MILLIS - 1L)

    assertInterval(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes")
    assertUpdateInterval(FROM_MILLIS + HOUR_IN_MILLIS - 1L, MINUTE_IN_MILLIS - 1L)

    assertInterval(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago")
    assertUpdateInterval(FROM_MILLIS - HOUR_IN_MILLIS, 1L)

    assertInterval(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour")
    assertUpdateInterval(FROM_MILLIS + HOUR_IN_MILLIS, 1L)

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago")
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS + 1L, HOUR_IN_MILLIS - 1L)

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours")
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS - 1L, HOUR_IN_MILLIS - 1L)

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago")
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS, 1L)

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours")
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS, 1L)

    assertInterval(FROM_MILLIS - YEAR_IN_MILLIS, "2000")
    assertUpdateInterval(FROM_MILLIS - YEAR_IN_MILLIS, NEXT_YEAR)

    assertInterval(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "1999")
    assertUpdateInterval(FROM_MILLIS - 2 * YEAR_IN_MILLIS, NEXT_YEAR)

    assertInterval(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8")
    assertUpdateInterval(FROM_MILLIS - DAY_IN_MILLIS - 1L, NEXT_YEAR)

    assertInterval(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10")
    assertUpdateInterval(FROM_MILLIS + DAY_IN_MILLIS + 1L, 1L)

    assertInterval(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002")
    assertUpdateInterval(FROM_MILLIS + YEAR_IN_MILLIS, NEXT_YEAR)

    assertInterval(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003")
    assertUpdateInterval(FROM_MILLIS + 2 * YEAR_IN_MILLIS, NEXT_YEAR)
  }

  @Test
  fun testAirdateIntervalExtended() {
    assertIntervalExtended(0L, "Unknown airdate")
    assertIntervalExtended(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now")
    assertIntervalExtended(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now")
    assertIntervalExtended(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago")
    assertIntervalExtended(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes")
    assertIntervalExtended(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago")
    assertIntervalExtended(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes")
    assertIntervalExtended(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago")
    assertIntervalExtended(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour")
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago")
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours")
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago")
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours")

    assertIntervalExtended(FROM_MILLIS - YEAR_IN_MILLIS, "Sep 10, 2000 3:46 am")
    assertIntervalExtended(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "Sep 12, 1999 3:46 am")
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8 3:46 am")
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10 3:46 am")
    assertIntervalExtended(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002 3:46 am")
    assertIntervalExtended(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003 3:46 am")

    assertIntervalExtended(
      FROM_MILLIS - YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 10, 2000 3:46 pm"
    )
    assertIntervalExtended(
      FROM_MILLIS - 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 12, 1999 3:46 pm"
    )
    assertIntervalExtended(FROM_MILLIS - DAY_IN_MILLIS - 12 * HOUR_IN_MILLIS, "Sep 7 3:46 pm")
    assertIntervalExtended(FROM_MILLIS + DAY_IN_MILLIS + 12 * HOUR_IN_MILLIS, "Sep 10 3:46 pm")
    assertIntervalExtended(
      FROM_MILLIS + YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 8, 2002 3:46 pm"
    )
    assertIntervalExtended(
      FROM_MILLIS + 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 7, 2003 3:46 pm"
    )
  }

  @Test
  fun testAirdateInterval24HourFormat() {
    assertInterval24h(0L, "Unknown airdate")
    assertInterval24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now")
    assertInterval24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now")
    assertInterval24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago")
    assertInterval24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes")
    assertInterval24h(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago")
    assertInterval24h(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes")
    assertInterval24h(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago")
    assertInterval24h(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour")
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago")
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours")
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago")
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours")

    assertInterval24h(FROM_MILLIS - YEAR_IN_MILLIS, "2000")
    assertInterval24h(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "1999")
    assertInterval24h(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8")
    assertInterval24h(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10")
    assertInterval24h(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002")
    assertInterval24h(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003")
  }

  @Test
  fun testAirdateIntervalExtended24HourFormat() {
    assertIntervalExtended24h(0L, "Unknown airdate")
    assertIntervalExtended24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS, "Now")
    assertIntervalExtended24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS, "Now")
    assertIntervalExtended24h(FROM_MILLIS - 2 * MINUTE_IN_MILLIS - 1L, "2 minutes ago")
    assertIntervalExtended24h(FROM_MILLIS + 2 * MINUTE_IN_MILLIS + 1L, "In 2 minutes")
    assertIntervalExtended24h(FROM_MILLIS - HOUR_IN_MILLIS + 1L, "59 minutes ago")
    assertIntervalExtended24h(FROM_MILLIS + HOUR_IN_MILLIS - 1L, "In 59 minutes")
    assertIntervalExtended24h(FROM_MILLIS - HOUR_IN_MILLIS, "1 hour ago")
    assertIntervalExtended24h(FROM_MILLIS + HOUR_IN_MILLIS, "In 1 hour")
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS + 1L, "23 hours ago")
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS - 1L, "In 23 hours")
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS, "24 hours ago")
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS, "In 24 hours")

    assertIntervalExtended24h(FROM_MILLIS - YEAR_IN_MILLIS, "Sep 10, 2000 3:46")
    assertIntervalExtended24h(FROM_MILLIS - 2 * YEAR_IN_MILLIS, "Sep 12, 1999 3:46")
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS - 1L, "Sep 8 3:46")
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS + 1L, "Sep 10 3:46")
    assertIntervalExtended24h(FROM_MILLIS + YEAR_IN_MILLIS, "Sep 8, 2002 3:46")
    assertIntervalExtended24h(FROM_MILLIS + 2 * YEAR_IN_MILLIS, "Sep 7, 2003 3:46")

    assertIntervalExtended24h(
      FROM_MILLIS - YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 10, 2000 15:46"
    )
    assertIntervalExtended24h(
      FROM_MILLIS - 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 12, 1999 15:46"
    )
    assertIntervalExtended24h(FROM_MILLIS - DAY_IN_MILLIS - 12 * HOUR_IN_MILLIS, "Sep 7 15:46")
    assertIntervalExtended24h(FROM_MILLIS + DAY_IN_MILLIS + 12 * HOUR_IN_MILLIS, "Sep 10 15:46")
    assertIntervalExtended24h(
      FROM_MILLIS + YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 8, 2002 15:46"
    )
    assertIntervalExtended24h(
      FROM_MILLIS + 2 * YEAR_IN_MILLIS + 12 * HOUR_IN_MILLIS,
      "Sep 7, 2003 15:46"
    )
  }

  @Test
  fun testStatsString() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val hourInMinutes: Long = 60
    val dayInMinutes = hourInMinutes * 24

    val zeroMinutes = 0L
    val zeroMinutesString = DateStringUtils.getRuntimeString(context, zeroMinutes)
    assertThat(zeroMinutesString).isEqualTo("0m")

    val thirtyMinutes = 30L
    val thirtyMinutesString = DateStringUtils.getRuntimeString(context, thirtyMinutes)
    assertThat(thirtyMinutesString).isEqualTo("30m")

    val sixHoursThirtyMinutes = 6 * hourInMinutes + 30
    val sixHoursThirtyMinutesString =
      DateStringUtils.getRuntimeString(context, sixHoursThirtyMinutes)
    assertThat(sixHoursThirtyMinutesString).isEqualTo("6h 30m")

    val twoDaysSixHoursThirtyMinutes = 2 * dayInMinutes + 6 * hourInMinutes + 30
    val twoDaysSixHoursThirtyMinutesString =
      DateStringUtils.getRuntimeString(context, twoDaysSixHoursThirtyMinutes)
    assertThat(twoDaysSixHoursThirtyMinutesString).isEqualTo("2d 6h 30m")
  }

  companion object {

    private const val FROM_MILLIS = 1000000000000L
    private const val NEXT_YEAR = 9839600000L
  }
}
