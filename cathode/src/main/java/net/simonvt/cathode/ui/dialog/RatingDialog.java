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
package net.simonvt.cathode.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;

public class RatingDialog extends DialogFragment {

  public enum Type {
    SHOW, EPISODE, MOVIE,
  }

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.dialog.RatingDialog.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.dialog.RatingDialog.id";
  private static final String ARG_RATING = "net.simonvt.cathode.ui.dialog.RatingDialog.rating";

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject MovieTaskScheduler movieScheduler;

  private Type type;
  private long id;

  private String[] ratingTexts;

  public static RatingDialog newInstance(Type type, long id, int rating) {
    RatingDialog dialog = new RatingDialog();

    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putLong(ARG_ID, id);
    args.putInt(ARG_RATING, rating);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    type = (Type) args.getSerializable(ARG_TYPE);
    id = args.getLong(ARG_ID);

    ratingTexts = getResources().getStringArray(R.array.ratings);
  }

  @NonNull @SuppressWarnings("InflateParams") @Override
  public Dialog onCreateDialog(Bundle inState) {
    final int ratingArg = getArguments().getInt(ARG_RATING);
    final float initialRating = ratingArg / 2.0f;

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_rating, null);
    final TextView ratingText = v.findViewById(R.id.ratingText);
    ratingText.setText(this.ratingTexts[ratingArg]);
    final RatingBar ratingBar = v.findViewById(R.id.rating);
    ratingBar.setRating(initialRating);
    ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
      @Override public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
        final int rating = (int) (v * 2);
        ratingText.setText(ratingTexts[rating]);
      }
    });

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      LayerDrawable ld = (LayerDrawable) ratingBar.getProgressDrawable();
      Drawable background = ld.findDrawableByLayerId(android.R.id.background);
      background = DrawableCompat.wrap(background);
      Drawable secondaryProgress = ld.findDrawableByLayerId(android.R.id.secondaryProgress);
      secondaryProgress = DrawableCompat.wrap(secondaryProgress);
      Drawable progress = ld.findDrawableByLayerId(android.R.id.progress);
      progress = DrawableCompat.wrap(progress);

      DrawableCompat.setTint(background, 0xFF009688);
      DrawableCompat.setTint(secondaryProgress, 0xFF009688);
      DrawableCompat.setTint(progress, 0xFF009688);

      ld.setDrawableByLayerId(android.R.id.background, background);
      ld.setDrawableByLayerId(android.R.id.secondaryProgress, secondaryProgress);
      ld.setDrawableByLayerId(android.R.id.progress, progress);
    }

    builder.setView(v);
    builder.setPositiveButton(R.string.action_rate, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        final int rating = (int) (ratingBar.getRating() * 2);

        switch (type) {
          case SHOW:
            showScheduler.rate(id, rating);
            break;

          case EPISODE:
            episodeScheduler.rate(id, rating);
            break;

          case MOVIE:
            movieScheduler.rate(id, rating);
            break;
        }
      }
    });

    return builder.create();
  }
}
