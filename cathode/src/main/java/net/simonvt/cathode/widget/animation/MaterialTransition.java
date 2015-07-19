/*
 * Copyright (C) 2014 Square, Inc.
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.widget.animation;

import android.graphics.ColorMatrix;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import static net.simonvt.cathode.widget.animation.ColorMatrices.COLUMN_ALPHA;
import static net.simonvt.cathode.widget.animation.ColorMatrices.ROW_ALPHA;

/**
 * A transition filter that is based on the suggested loading treatment for Material Design. This
 * transition adjusts alpha, saturation, and contrast.
 */
public final class MaterialTransition {

  private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();

  private static final ColorMatrix ALPHA_MATRIX = new ColorMatrix();
  private static final ColorMatrix CONTRAST_MATRIX = new ColorMatrix();
  private static final ColorMatrix SATURATION_MATRIX = new ColorMatrix();

  private MaterialTransition() {
  }

  public static void apply(ColorMatrix colorMatrix, float fraction) {
    // Alpha fade from 0 to 1 for the first 2/3 of the transition.
    float alpha = INTERPOLATOR.getInterpolation(Math.min(1f, fraction / 0.33f));
    ALPHA_MATRIX.getArray()[ROW_ALPHA + COLUMN_ALPHA] = alpha;

    // Contrast fade from 0 to 1 for the first half of the transition.
    float contrast = INTERPOLATOR.getInterpolation(Math.min(1f, fraction / 0.5f));
    ColorMatrices.setContrast(CONTRAST_MATRIX, contrast);

    // Saturation fade from 0.2 to 1 for the last two thirds of the transition.
    float saturation =
        0.2f + 0.8f * INTERPOLATOR.getInterpolation(Math.max(0, fraction - 0.33f) / 0.67f);
    SATURATION_MATRIX.setSaturation(saturation);

    colorMatrix.postConcat(SATURATION_MATRIX);
    colorMatrix.postConcat(ALPHA_MATRIX);
    colorMatrix.postConcat(CONTRAST_MATRIX);
  }
}
