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

package net.simonvt.cathode.common.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import com.squareup.picasso.Transformation;

public class CircularShadowTransformation implements Transformation {

  private final int dropShadowSize;

  public CircularShadowTransformation(int dropShadowSize) {
    this.dropShadowSize = dropShadowSize;
  }

  @Override public Bitmap transform(Bitmap source) {
    final int sourceWidth = source.getWidth();
    final int sourceHeight = source.getHeight();
    final int resultWidth = sourceWidth + 2 * dropShadowSize;
    final int resultHeight = sourceHeight + 3 * dropShadowSize;

    final int shadowStartColor = 0x03000000;
    final int shadowEndColor = 0x37000000;

    float radius = Math.min(resultWidth / 2, resultHeight / 2);

    Bitmap result = Bitmap.createBitmap(resultWidth, resultHeight, source.getConfig());

    Paint cornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    cornerShadowPaint.setStyle(Paint.Style.FILL);
    cornerShadowPaint.setDither(true);

    RectF outerBounds = new RectF(-radius, -radius, radius, radius);
    RectF innerBounds = new RectF(outerBounds);
    innerBounds.inset(dropShadowSize, dropShadowSize);

    Path shadowPath = new Path();

    shadowPath.setFillType(Path.FillType.EVEN_ODD);
    shadowPath.moveTo(-radius, 0);
    shadowPath.rLineTo(-dropShadowSize, 0);
    shadowPath.arcTo(outerBounds, 180f, 90f, false);
    shadowPath.arcTo(innerBounds, 270f, -90f, false);
    shadowPath.close();

    float startRatio = radius / (radius + dropShadowSize);
    cornerShadowPaint.setShader(new RadialGradient(0, 0, radius + dropShadowSize, new int[] {
        shadowStartColor, shadowStartColor, shadowEndColor
    }, new float[] {
        0f, startRatio, 1f
    }, Shader.TileMode.CLAMP));

    Canvas canvas = new Canvas(result);

    canvas.translate(0, dropShadowSize / 2);

    int saved = canvas.save();
    final int inset = (int) radius;
    canvas.translate(inset, inset);
    canvas.drawPath(shadowPath, cornerShadowPaint);
    canvas.restoreToCount(saved);

    saved = canvas.save();
    canvas.translate(resultWidth - inset, resultHeight - inset - dropShadowSize);
    canvas.rotate(180f);
    canvas.drawPath(shadowPath, cornerShadowPaint);
    canvas.restoreToCount(saved);

    saved = canvas.save();
    canvas.translate(inset, resultHeight - inset - dropShadowSize);
    canvas.rotate(270f);
    canvas.drawPath(shadowPath, cornerShadowPaint);
    canvas.restoreToCount(saved);

    saved = canvas.save();
    canvas.translate(resultWidth - inset, inset);
    canvas.rotate(90f);
    canvas.drawPath(shadowPath, cornerShadowPaint);
    canvas.restoreToCount(saved);

    canvas.translate(0, -dropShadowSize / 2);

    canvas.drawBitmap(source, dropShadowSize, dropShadowSize, null);

    source.recycle();
    return result;
  }

  @Override public String key() {
    return "dropshadow=" + dropShadowSize;
  }
}
