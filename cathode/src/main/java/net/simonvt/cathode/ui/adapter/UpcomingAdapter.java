/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.adapter;

import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.OverflowView;

public class UpcomingAdapter extends ShowsWithNextAdapter {

  public interface OnRemoveListener {
    void onRemove(View view, int position);
  }

  private OnRemoveListener listener;

  public UpcomingAdapter(FragmentActivity activity, Cursor cursor, OnRemoveListener listener) {
    super(activity, cursor, LibraryType.WATCHED);
    this.listener = listener;
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean isHidden, boolean watching) {
    super.setupOverflowItems(overflow, typeCount, airedCount, hasNext, isHidden, watching);

    if (isHidden) {
      overflow.addItem(R.id.action_unhide, R.string.action_unhide);
    } else {
      overflow.addItem(R.id.action_hide, R.string.action_hide);
    }
  }

  @Override protected void onWatchNext(View view, int position, long showId, int watchedCount,
      int airedCount) {
    if (watchedCount + 1 >= airedCount) {
      listener.onRemove(view, position);
    }
    super.onWatchNext(view, position, showId, watchedCount, airedCount);
  }
}
