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
package net.simonvt.cathode.ui.history;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;

public class RemoveFromHistoryDialog extends DialogFragment {

  public static final String TAG = "net.simonvt.cathode.ui.history.RemoveFromHistoryFragment";

  private static final String ARG_TYPE =
      "net.simonvt.cathode.ui.history.RemoveFromHistoryFragment.type";
  private static final String ARG_ID =
      "net.simonvt.cathode.ui.history.RemoveFromHistoryFragment.id";
  private static final String ARG_TITLE =
      "net.simonvt.cathode.ui.history.RemoveFromHistoryFragment.title";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.history.RemoveFromHistoryFragment.showTitle";

  public enum Type {
    SEASON, EPISODE, MOVIE,
  }

  private SeasonTaskScheduler seasonScheduler;
  private EpisodeTaskScheduler episodeScheduler;
  private MovieTaskScheduler movieScheduler;

  private NavigationListener navigationListener;

  public static Bundle getArgs(Type type, long id, String title, String showTitle) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putLong(ARG_ID, id);
    args.putString(ARG_TITLE, title);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Inject public RemoveFromHistoryDialog(SeasonTaskScheduler seasonScheduler,
      EpisodeTaskScheduler episodeScheduler, MovieTaskScheduler movieScheduler) {
    this.seasonScheduler = seasonScheduler;
    this.episodeScheduler = episodeScheduler;
    this.movieScheduler = movieScheduler;
  }

  @Override public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    navigationListener = (NavigationListener) requireActivity();
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle inState) {
    final Type type = (Type) getArguments().getSerializable(ARG_TYPE);
    final long id = getArguments().getLong(ARG_ID);
    final String title = getArguments().getString(ARG_TITLE);
    final String showTitle = getArguments().getString(ARG_SHOW_TITLE);

    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
    builder.setTitle(R.string.history_unwatched_title);

    if (type == Type.EPISODE || type == Type.MOVIE) {
      builder.setNegativeButton(R.string.history_unwatched_view_all,
          new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
              switch (type) {
                case EPISODE:
                  navigationListener.onDisplayEpisodeHistory(id, showTitle);
                  break;

                case MOVIE:
                  navigationListener.onDisplayMovieHistory(id, title);
                  break;
              }
            }
          });
    }

    builder.setPositiveButton(R.string.history_unwatched_all,
        new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            switch (type) {
              case SEASON:
                seasonScheduler.removeFromHistory(id);
                break;

              case EPISODE:
                episodeScheduler.removeFromHistory(id);
                break;

              case MOVIE:
                movieScheduler.removeFromHistory(id);
                break;
            }
          }
        });

    return builder.create();
  }
}
