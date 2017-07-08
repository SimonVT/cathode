/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
import android.util.AttributeSet;
import net.simonvt.cathode.R;

public class CheckInView extends OverflowView {

  private boolean watching;

  private CheckInDrawable checkInDrawable;

  public CheckInView(Context context) {
    super(context);
    init();
  }

  public CheckInView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CheckInView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    checkInDrawable = new CheckInDrawable(getContext(), R.drawable.ic_anim_checkin_32dp,
        R.drawable.ic_anim_cancel_32dp, R.drawable.ic_action_checkin_32dp,
        R.drawable.ic_action_cancel_32dp);

    setImageDrawable(checkInDrawable);

    addItem(R.id.action_checkin, R.string.action_checkin);
    addItem(R.id.action_history_add, R.string.action_history_add);
  }

  public void reset() {
    updateItems(false);
    checkInDrawable.reset();
  }

  public void setId(long id) {
    checkInDrawable.setId(id);
  }

  private void updateItems(boolean watching) {
    removeItems();

    if (watching) {
      addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
    } else {
      addItem(R.id.action_checkin, R.string.action_checkin);
      addItem(R.id.action_history_add, R.string.action_history_add);
    }
  }

  public void setWatching(boolean watching) {
    if (watching != this.watching) {
      updateItems(watching);

      checkInDrawable.setWatching(watching);

      this.watching = watching;
    }
  }
}
