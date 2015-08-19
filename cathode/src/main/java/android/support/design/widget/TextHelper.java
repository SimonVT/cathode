/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package android.support.design.widget;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class TextHelper {

  private CollapsingTextHelper textHelper;

  public TextHelper(View view) {
    textHelper = new CollapsingTextHelper(view);
  }

  public void setExpandedTextGravity(int expandedTextVerticalGravity) {
    textHelper.setExpandedTextGravity(expandedTextVerticalGravity);
  }

  public void setTextSizeInterpolator(DecelerateInterpolator textSizeInterpolator) {
    textHelper.setTextSizeInterpolator(textSizeInterpolator);
  }

  public void setExpandedTextAppearance(int expandedTextAppearance) {
    textHelper.setExpandedTextAppearance(expandedTextAppearance);
  }

  public void setCollapsedTextAppearance(int collapsedTextAppearance) {
    textHelper.setCollapsedTextAppearance(collapsedTextAppearance);
  }

  public void draw(Canvas canvas) {
    textHelper.draw(canvas);
  }

  public void setCollapsedBounds(Rect bounds) {
    textHelper.setCollapsedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

  public void setExpandedBounds(Rect bounds) {
    textHelper.setExpandedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

  public void recalculate() {
    textHelper.recalculate();
  }

  public void setExpansionFraction(float expansionFraction) {
    textHelper.setExpansionFraction(expansionFraction);
  }

  public void setText(CharSequence text) {
    textHelper.setText(text);
  }
}
