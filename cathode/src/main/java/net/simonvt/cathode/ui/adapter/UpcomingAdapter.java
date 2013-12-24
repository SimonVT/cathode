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

import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.OverflowView;

public class UpcomingAdapter extends ShowsWithNextAdapter {

  public UpcomingAdapter(Context context, Cursor cursor) {
    super(context, cursor, LibraryType.WATCHED);
  }

  protected void setupOverflowItems(OverflowView overflow, int typeCount, int airedCount,
      boolean hasNext, boolean isHidden) {
    super.setupOverflowItems(overflow, typeCount, airedCount, hasNext, isHidden);

    if (isHidden) {
      overflow.addItem(R.id.action_unhide, R.string.action_unhide);
    } else {
      overflow.addItem(R.id.action_hide, R.string.action_hide);
    }
  }
}
