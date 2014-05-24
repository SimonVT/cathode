/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

public class ViewCopy extends View {

  private Bitmap viewCopy;

  public ViewCopy(View view) {
    super(view.getContext());
    copyView(view);
    measure(MeasureSpec.makeMeasureSpec(view.getWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(view.getHeight(), MeasureSpec.EXACTLY));
    layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
  }

  private void copyView(View view) {
    try {
      Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
      Canvas c = new Canvas(b);
      view.draw(c);
      viewCopy = b;
    } catch (OutOfMemoryError e) {
      // ignore
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    if (viewCopy != null) {
      canvas.drawBitmap(viewCopy, 0.0f, 0.0f, null);
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
        MeasureSpec.getSize(heightMeasureSpec));
  }
}
