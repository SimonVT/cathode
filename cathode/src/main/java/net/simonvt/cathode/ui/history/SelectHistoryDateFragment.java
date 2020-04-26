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
package net.simonvt.cathode.ui.history;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import java.text.DateFormat;
import java.util.Calendar;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.AppBarFragment;
import net.simonvt.cathode.databinding.HistorySelectDateBinding;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;

public class SelectHistoryDateFragment extends AppBarFragment
    implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

  public static final String TAG = "net.simonvt.cathode.ui.history.SelectHistoryDateFragment";

  private static final String DIALOG_DATE =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.dateDialog";
  private static final String DIALOG_TIME =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.timeDialog";

  private static final String ARG_TYPE =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.id";
  private static final String ARG_TITLE =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.title";

  private static final String STATE_YEAR =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.year";
  private static final String STATE_MONTH =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.month";
  private static final String STATE_DAY =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.day";
  private static final String STATE_HOUR =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.hour";
  private static final String STATE_MINUTE =
      "net.simonvt.cathode.ui.dialog.SelectHistoryDateFragment.minute";

  public enum Type {
    SHOW, SEASON, EPISODE, EPISODE_OLDER, MOVIE,
  }

  private ShowTaskScheduler showScheduler;
  private SeasonTaskScheduler seasonScheduler;
  private EpisodeTaskScheduler episodeScheduler;
  private MovieTaskScheduler movieScheduler;

  private NavigationListener navigationListener;

  private Type type;
  private long id;
  private String title;

  private HistorySelectDateBinding binding;

  private int year;
  private int month;
  private int day;
  private int hour;
  private int minute;

  public static Bundle getArgs(Type type, long id, String title) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putLong(ARG_ID, id);
    args.putString(ARG_TITLE, title);
    return args;
  }

  @Inject public SelectHistoryDateFragment(ShowTaskScheduler showScheduler,
      SeasonTaskScheduler seasonScheduler,
      EpisodeTaskScheduler episodeScheduler,
      MovieTaskScheduler movieScheduler) {
    this.showScheduler = showScheduler;
    this.seasonScheduler = seasonScheduler;
    this.episodeScheduler = episodeScheduler;
    this.movieScheduler = movieScheduler;
  }

  @Override public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    navigationListener = (NavigationListener) requireActivity();
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    type = (Type) getArguments().getSerializable(ARG_TYPE);
    id = getArguments().getLong(ARG_ID);
    title = getArguments().getString(ARG_TITLE);

    setTitle(title);

    String backdrop = null;
    switch (type) {
      case SHOW:
        backdrop = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.BACKDROP, id);
        break;

      case SEASON:
        backdrop = ImageUri.create(ImageUri.ITEM_SEASON, ImageType.BACKDROP, id);
        break;

      case EPISODE_OLDER:
      case EPISODE:
        backdrop = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, id);
        break;

      case MOVIE:
        backdrop = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, id);
        break;
    }

    setBackdrop(backdrop);

    if (inState == null) {
      Calendar calendar = Calendar.getInstance();
      year = calendar.get(Calendar.YEAR);
      month = calendar.get(Calendar.MONTH);
      day = calendar.get(Calendar.DAY_OF_MONTH);
      hour = calendar.get(Calendar.HOUR_OF_DAY);
      minute = calendar.get(Calendar.MINUTE);
    } else {
      year = inState.getInt(STATE_YEAR);
      month = inState.getInt(STATE_MONTH);
      day = inState.getInt(STATE_DAY);
      hour = inState.getInt(STATE_HOUR);
      minute = inState.getInt(STATE_MINUTE);
    }
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_YEAR, year);
    outState.putInt(STATE_MONTH, month);
    outState.putInt(STATE_DAY, day);
    outState.putInt(STATE_HOUR, hour);
    outState.putInt(STATE_MINUTE, minute);
  }

  @Override
  protected View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    binding = HistorySelectDateBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    binding.date.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        selectDate();
      }
    });
    binding.time.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        selectTime();
      }
    });
    updateDateText();
    updateTimeText();
  }

  @Override public void onDestroyView() {
    binding = null;
    super.onDestroyView();
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.history_select_date);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_history_add) {
      switch (type) {
        case SHOW:
          showScheduler.addToHistory(id, year, month, day, hour, minute);
          navigationListener.popIfTop(this);
          break;

        case SEASON:
          seasonScheduler.addToHistory(id, year, month, day, hour, minute);
          navigationListener.popIfTop(this);
          break;

        case EPISODE:
          episodeScheduler.addToHistory(id, year, month, day, hour, minute);
          navigationListener.popIfTop(this);
          break;

        case EPISODE_OLDER:
          episodeScheduler.addOlderToHistory(id, year, month, day, hour, minute);
          navigationListener.popIfTop(this);
          break;

        case MOVIE:
          movieScheduler.addToHistory(id, year, month, day, hour, minute);
          break;
      }

      return true;
    }

    return super.onMenuItemClick(item);
  }

  private void updateDateText() {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month, day);
    DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
    binding.dateText.setText(df.format(cal.getTime()));
  }

  private void updateTimeText() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
    binding.timeText.setText(df.format(cal.getTime()));
  }

  @Override
  public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
    this.year = year;
    this.month = monthOfYear;
    this.day = dayOfMonth;
    updateDateText();
  }

  @Override public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
    this.hour = hourOfDay;
    this.minute = minute;
    updateTimeText();
  }

  void selectDate() {
    DatePickerDialog dialog = DatePickerDialog.newInstance(year, month, day);
    dialog.setTargetFragment(this, 0);
    dialog.show(requireFragmentManager(), DIALOG_DATE);
  }

  void selectTime() {
    TimePickerDialog dialog = TimePickerDialog.newInstance(hour, minute);
    dialog.setTargetFragment(this, 0);
    dialog.show(requireFragmentManager(), DIALOG_TIME);
  }
}
