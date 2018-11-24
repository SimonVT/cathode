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
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.android.datetimepicker.R;
import com.android.datetimepicker.date.DatePickerDialog.OnDateChangedListener;

/**
 * This displays a list of months in a calendar format with selectable days.
 */
public class DayPickerView extends FrameLayout implements OnDateChangedListener {

  RecyclerView recyclerView;
  LinearLayoutManager layoutManager;

  // highlighted time
  protected CalendarDay mSelectedDay = new CalendarDay();
  protected MonthAdapter mAdapter;

  // which month should be displayed/highlighted [0-11]
  protected int mCurrentMonthDisplayed;

  private DatePickerController mController;

  public DayPickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public DayPickerView(Context context, DatePickerController controller) {
    super(context);
    init(context);
    setController(controller);
  }

  public void setController(DatePickerController controller) {
    mController = controller;
    mController.registerOnDateChangedListener(this);
    refreshAdapter();
    onDateChanged();
  }

  public void init(Context context) {
    setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    inflate(context, R.layout.day_picker_view, this);

    recyclerView = (RecyclerView) findViewById(android.R.id.list);
    layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(mAdapter);
    SnapHelper snapHelper = new LinearSnapHelper();
    snapHelper.attachToRecyclerView(recyclerView);
  }

  public void onChange() {
    refreshAdapter();
  }

  /**
   * Creates a new adapter if necessary and sets up its parameters. Override
   * this method to provide a custom adapter.
   */
  protected void refreshAdapter() {
    if (mAdapter == null) {
      mAdapter = createMonthAdapter(getContext(), mController);
      recyclerView.setAdapter(mAdapter);
    } else {
      mAdapter.setSelectedDay(mSelectedDay);
    }
  }

  public MonthAdapter createMonthAdapter(Context context, DatePickerController controller) {
    return new MonthAdapter(context, controller);
  }

  /**
   * This moves to the specified time in the view. If the time is not already
   * in range it will move the list so that the first of the month containing
   * the time is at the top of the view. If the new time is already in view
   * the list will not be scrolled unless forceScroll is true. This time may
   * optionally be highlighted as selected as well.
   *
   * @param day The time to move to
   * @return Whether or not the view animated to the new location
   */
  public boolean goTo(CalendarDay day) {
    mSelectedDay.set(day);

    final int position =
        (day.year - mController.getMinYear()) * MonthAdapter.MONTHS_IN_YEAR + day.month;

    layoutManager.scrollToPosition(position);

    mAdapter.setSelectedDay(mSelectedDay);

    mCurrentMonthDisplayed = day.month;

    return false;
  }

  public void setSelection(final int position) {
    layoutManager.scrollToPosition(position);
  }

  /**
   * Gets the position of the view that is most prominently displayed within the list view.
   */
  public int getMostVisiblePosition() {
    return layoutManager.findFirstCompletelyVisibleItemPosition();
  }

  @Override public void onDateChanged() {
    goTo(mController.getSelectedDay());
  }
}
