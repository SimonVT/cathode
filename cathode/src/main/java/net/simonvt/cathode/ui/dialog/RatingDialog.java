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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;

public class RatingDialog extends DialogFragment {

  public enum Type {
    SHOW,
    EPISODE,
    MOVIE,
  }

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.dialog.RatingDialog.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.dialog.RatingDialog.id";
  private static final String ARG_RATING = "net.simonvt.cathode.ui.dialog.RatingDialog.rating";

  @Inject ShowTaskScheduler showScheduler;

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject MovieTaskScheduler movieScheduler;

  private Type type;

  private long id;

  private int rating;

  private String[] ratingText;

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
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    type = (Type) args.getSerializable(ARG_TYPE);
    id = args.getLong(ARG_ID);
    rating = args.getInt(ARG_RATING);

    ratingText = getResources().getStringArray(R.array.ratings);
  }

  @SuppressWarnings("InflateParams") @Override public Dialog onCreateDialog(Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_rating, null);
    final int rating = getArguments().getInt(ARG_RATING);
    final TextView ratingText = (TextView) v.findViewById(R.id.ratingText);
    ratingText.setText(this.ratingText[this.rating]);
    final RatingBar ratingBar = (RatingBar) v.findViewById(R.id.rating);
    ratingBar.setRating(rating);
    ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
      @Override public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
        RatingDialog.this.rating = (int) v;
        ratingText.setText(RatingDialog.this.ratingText[(int) v]);
      }
    });

    builder.setView(v);
    builder.setPositiveButton(R.string.action_rate, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        switch (type) {
          case SHOW:
            showScheduler.rate(id, (int) ratingBar.getRating());
            break;

          case EPISODE:
            episodeScheduler.rate(id, (int) ratingBar.getRating());
            break;

          case MOVIE:
            movieScheduler.rate(id, (int) ratingBar.getRating());
            break;
        }
      }
    });

    return builder.create();
  }
}
