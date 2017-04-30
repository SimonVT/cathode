/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.datetimepicker.date;

import android.text.format.Time;
import java.util.Calendar;

/**
 * A convenience class to represent a specific date.
 */
public class CalendarDay {

  private Calendar calendar;
  private Time time;
  int year;
  int month;
  int day;

  public CalendarDay() {
    setTime(System.currentTimeMillis());
  }

  public CalendarDay(long timeInMillis) {
    setTime(timeInMillis);
  }

  public CalendarDay(Calendar calendar) {
    year = calendar.get(Calendar.YEAR);
    month = calendar.get(Calendar.MONTH);
    day = calendar.get(Calendar.DAY_OF_MONTH);
  }

  public CalendarDay(int year, int month, int day) {
    setDay(year, month, day);
  }

  public void set(CalendarDay date) {
    year = date.year;
    month = date.month;
    day = date.day;
  }

  public void setDay(int year, int month, int day) {
    this.year = year;
    this.month = month;
    this.day = day;
  }

  public synchronized void setJulianDay(int julianDay) {
    if (time == null) {
      time = new Time();
    }
    time.setJulianDay(julianDay);
    setTime(time.toMillis(false));
  }

  private void setTime(long timeInMillis) {
    if (calendar == null) {
      calendar = Calendar.getInstance();
    }
    calendar.setTimeInMillis(timeInMillis);
    month = calendar.get(Calendar.MONTH);
    year = calendar.get(Calendar.YEAR);
    day = calendar.get(Calendar.DAY_OF_MONTH);
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }
}
