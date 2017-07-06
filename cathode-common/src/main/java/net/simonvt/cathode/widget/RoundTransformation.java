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
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import com.squareup.picasso.Transformation;

public class RoundTransformation implements Transformation {

  @Override public Bitmap transform(Bitmap source) {
    final int width = source.getWidth();
    final int height = source.getHeight();

    Bitmap.Config config = source.getConfig();
    if (config == null) {
      config = Bitmap.Config.ARGB_8888;
    }
    Bitmap roundBitmap = Bitmap.createBitmap(width, height, config);
    Canvas canvas = new Canvas(roundBitmap);

    BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setShader(shader);

    RectF roundRect = new RectF(0, 0, width, height);
    float radius = Math.min(width / 2, height / 2);

    canvas.drawRoundRect(roundRect, radius, radius, paint);

    source.recycle();

    return roundBitmap;
  }

  @Override public String key() {
    return "rounded";
  }
}
