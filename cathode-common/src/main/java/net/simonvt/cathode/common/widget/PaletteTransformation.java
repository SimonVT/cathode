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

package net.simonvt.cathode.common.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.graphics.Palette;
import com.squareup.picasso.Transformation;

public class PaletteTransformation implements Transformation {

  public static boolean shouldTransform = false;

  @Override public Bitmap transform(Bitmap source) {
    if (!shouldTransform) {
      return source;
    }

    Palette palette = Palette.from(source).generate();

    final int sourceWidth = source.getWidth();
    final int sourceHeight = source.getHeight();

    final Paint paint = new Paint();
    paint.setColor(palette.getVibrantColor(0xFF1C1C1E));

    Bitmap output = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(output);
    canvas.drawRect(new Rect(0, 0, sourceWidth, sourceHeight), paint);

    if (source != output) {
      source.recycle();
    }

    return output;
  }

  @Override public String key() {
    return "palette=true";
  }
}
