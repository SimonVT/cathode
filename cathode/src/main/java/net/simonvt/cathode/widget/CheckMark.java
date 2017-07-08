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
package net.simonvt.cathode.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.LibraryType;

public class CheckMark extends AppCompatTextView {

  private LibraryType type;

  public CheckMark(Context context) {
    super(context);
  }

  public CheckMark(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CheckMark(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setType(LibraryType type) {
    this.type = type;

    switch (type) {
      case COLLECTION:
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_collection, 0);
        setText(R.string.checkmark_collection);
        setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        break;

      case WATCHED:
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_watched, 0);
        setText(R.string.checkmark_watched);
        setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        break;

      case WATCHLIST:
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_watchlist, 0);
        setText(R.string.checkmark_watchlist);
        setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        break;
    }
  }
}
