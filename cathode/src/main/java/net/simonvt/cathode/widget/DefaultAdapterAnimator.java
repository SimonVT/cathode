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

import android.graphics.Rect;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class DefaultAdapterAnimator implements AdapterViewAnimator.Callback {

  private static final int FADE_OUT_DURATION = 300;
  private static final int FADE_IN_DURATION = 400;

  @Override public boolean onAddView(AdapterView parent, View view, int position, long id) {
    return false;
  }

  @Override
  public boolean onMoveView(AdapterView parent, final View view, int position, long id,
      Rect startBounds, Runnable endAction) {
    if (!(parent instanceof GridView)) return false;

    final int startLeft = startBounds.left;
    final int startTop = startBounds.top;
    final int left = view.getLeft();
    final int top = view.getTop();

    if (left <= startLeft) return false;

    int dX = startLeft - left;
    int dY = startTop - top;

    view.setTranslationX(dX);
    view.setTranslationY(dY);
    view.animate().alpha(0.0f).setDuration(FADE_OUT_DURATION).withEndAction(new Runnable() {
      @Override public void run() {
        view.setTranslationX(0.0f);
        view.setTranslationY(0.0f);
        view.animate().alpha(1.0f).setDuration(FADE_IN_DURATION);
      }
    });
    return false;
  }

  @Override public boolean onRemoveView(AdapterView parent, View view, long id, Rect startBounds) {
    return false;
  }
}
