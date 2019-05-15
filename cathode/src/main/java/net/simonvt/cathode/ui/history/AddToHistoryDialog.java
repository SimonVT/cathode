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
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;

public class AddToHistoryDialog extends DialogFragment {

  public static final String TAG = "net.simonvt.cathode.ui.history.AddToHistoryDialog";

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.history.AddToHistoryDialog.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.history.AddToHistoryDialog.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.history.AddToHistoryDialog.title";

  public enum Type {
    SHOW, SEASON, EPISODE, EPISODE_OLDER, MOVIE,
  }

  private ShowTaskScheduler showScheduler;
  private SeasonTaskScheduler seasonScheduler;
  private EpisodeTaskScheduler episodeScheduler;
  private MovieTaskScheduler movieScheduler;

  private NavigationListener navigationListener;

  public static Bundle getArgs(Type type, long id, String title) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putLong(ARG_ID, id);
    args.putString(ARG_TITLE, title);
    return args;
  }

  @Inject public AddToHistoryDialog(ShowTaskScheduler showScheduler,
      SeasonTaskScheduler seasonScheduler,
      EpisodeTaskScheduler episodeScheduler,
      MovieTaskScheduler movieScheduler) {
    this.showScheduler = showScheduler;
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

    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
    builder.setTitle(R.string.history_watched_when);

    builder.setNegativeButton(R.string.history_watched_other,
        new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            switch (type) {
              case SHOW:
                navigationListener.onSelectShowWatchedDate(id, title);
                break;

              case SEASON:
                navigationListener.onSelectSeasonWatchedDate(id, title);
                break;

              case EPISODE:
                navigationListener.onSelectEpisodeWatchedDate(id, title);
                break;

              case EPISODE_OLDER:
                navigationListener.onSelectOlderEpisodeWatchedDate(id, title);
                break;

              case MOVIE:
                navigationListener.onSelectMovieWatchedDate(id, title);
                break;
            }
          }
        });

    builder.setNeutralButton(R.string.history_watched_release,
        new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            switch (type) {
              case SHOW:
                showScheduler.addToHistoryOnRelease(id);
                break;

              case SEASON:
                seasonScheduler.addToHistoryOnRelease(id);
                break;

              case EPISODE:
                episodeScheduler.addToHistoryOnRelease(id);
                break;

              case EPISODE_OLDER:
                episodeScheduler.addOlderToHistoryOnRelease(id);
                break;

              case MOVIE:
                movieScheduler.addToHistoryOnRelease(id);
                break;
            }
          }
        });

    builder.setPositiveButton(R.string.history_watched_now, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int which) {
        switch (type) {
          case SHOW:
            showScheduler.addToHistoryNow(id);
            break;

          case SEASON:
            seasonScheduler.addToHistoryNow(id);
            break;

          case EPISODE:
            episodeScheduler.addToHistoryNow(id);
            break;

          case EPISODE_OLDER:
            episodeScheduler.addOlderToHistoryNow(id);
            break;

          case MOVIE:
            movieScheduler.addToHistoryNow(id);
            break;
        }
      }
    });

    return builder.create();
  }
}
