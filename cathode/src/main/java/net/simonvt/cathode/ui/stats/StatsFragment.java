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
package net.simonvt.cathode.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.BaseFragment;
import net.simonvt.cathode.common.util.DateStringUtils;

public class StatsFragment extends BaseFragment {

  public static final String TAG = "net.simonvt.cathode.ui.stats.StatsFragment";

  @BindView(R.id.stats_shows) View statsShows;
  @BindView(R.id.episodeTime) TextView episodeTime;
  @BindView(R.id.episodeCount) TextView episodeCount;
  @BindView(R.id.showCount) TextView showCount;

  @BindView(R.id.stats_movies) View statsMovies;
  @BindView(R.id.movieCount) TextView movieCount;
  @BindView(R.id.moviesTime) TextView movieTime;

  private StatsViewModel viewModel;

  private Stats stats;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.navigation_stats);

    viewModel = ViewModelProviders.of(this).get(StatsViewModel.class);
    viewModel.getStats().observe(this, new Observer<Stats>() {
      @Override public void onChanged(Stats stats) {
        StatsFragment.this.stats = stats;
        updateViews();
      }
    });
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_stats, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    updateViews();
  }

  private void updateViews() {
    if (stats != null && getView() != null) {
      statsShows.setVisibility(View.VISIBLE);
      episodeTime.setText(DateStringUtils.getRuntimeString(getContext(), stats.episodeTime));
      episodeCount.setText(
          getResources().getQuantityString(R.plurals.stats_episodes, stats.episodeCount,
              stats.episodeCount));
      showCount.setText(getResources().getQuantityString(R.plurals.stats_shows, stats.showCount,
          stats.showCount));

      statsMovies.setVisibility(View.VISIBLE);
      movieCount.setText(getResources().getQuantityString(R.plurals.stats_movies, stats.movieCount,
          stats.movieCount));
      movieTime.setText(DateStringUtils.getRuntimeString(getContext(), stats.moviesTime));
    }
  }
}
