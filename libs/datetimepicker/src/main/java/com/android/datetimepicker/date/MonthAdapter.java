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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import com.android.datetimepicker.date.MonthView.OnDayClickListener;
import java.util.HashMap;

/**
 * An adapter for a list of {@link MonthView} items.
 */
public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder>
    implements OnDayClickListener {

  private final Context context;
  protected final DatePickerController controller;

  private CalendarDay selectedDay;

  protected static final int MONTHS_IN_YEAR = 12;

  public MonthAdapter(Context context, DatePickerController controller) {
    this.context = context;
    this.controller = controller;
    selectedDay = new CalendarDay(System.currentTimeMillis());
    setSelectedDay(this.controller.getSelectedDay());

    setHasStableIds(true);
  }

  /**
   * Updates the selected day and related parameters.
   *
   * @param day The day to highlight
   */
  public void setSelectedDay(CalendarDay day) {
    selectedDay = day;
    notifyDataSetChanged();
  }

  public CalendarDay getSelectedDay() {
    return selectedDay;
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public int getItemCount() {
    return ((controller.getMaxYear() - controller.getMinYear()) + 1) * MONTHS_IN_YEAR;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    MonthView view = createMonthView(context);
    // Set up the new view
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(params);
    view.setClickable(true);
    view.setOnDayClickListener(this);

    return new ViewHolder(view);
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    HashMap<String, Integer> drawingParams = (HashMap<String, Integer>) holder.itemView.getTag();
    if (drawingParams == null) {
      drawingParams = new HashMap<>();
    }
    drawingParams.clear();

    final int month = position % MONTHS_IN_YEAR;
    final int year = position / MONTHS_IN_YEAR + controller.getMinYear();

    int selectedDay = -1;
    if (isSelectedDayInMonth(year, month)) {
      selectedDay = this.selectedDay.day;
    }

    // Invokes requestLayout() to ensure that the recycled view is set with the appropriate
    // height/number of weeks before being displayed.
    holder.monthView.reuse();

    drawingParams.put(MonthView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
    drawingParams.put(MonthView.VIEW_PARAMS_YEAR, year);
    drawingParams.put(MonthView.VIEW_PARAMS_MONTH, month);
    drawingParams.put(MonthView.VIEW_PARAMS_WEEK_START, controller.getFirstDayOfWeek());
    holder.monthView.setMonthParams(drawingParams);
    holder.monthView.invalidate();
  }

  public MonthView createMonthView(Context context) {
    final MonthView monthView = new MonthView(context);
    monthView.setDatePickerController(controller);
    return monthView;
  }

  private boolean isSelectedDayInMonth(int year, int month) {
    return selectedDay.year == year && selectedDay.month == month;
  }

  @Override public void onDayClick(MonthView view, CalendarDay day) {
    if (day != null) {
      onDayTapped(day);
    }
  }

  /**
   * Maintains the same hour/min/sec but moves the day to the tapped day.
   *
   * @param day The day that was tapped
   */
  protected void onDayTapped(CalendarDay day) {
    controller.tryVibrate();
    controller.onDayOfMonthSelected(day.year, day.month, day.day);
    setSelectedDay(day);
  }

  public class ViewHolder extends RecyclerView.ViewHolder {

    MonthView monthView;

    public ViewHolder(MonthView monthView) {
      super(monthView);
      this.monthView = monthView;
    }
  }
}
