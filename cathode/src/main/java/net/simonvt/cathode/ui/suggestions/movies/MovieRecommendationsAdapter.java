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
package net.simonvt.cathode.ui.suggestions.movies;

import android.view.View;
import androidx.fragment.app.FragmentActivity;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.entity.Movie;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class MovieRecommendationsAdapter extends MoviesAdapter {

  public interface DismissListener {

    void onDismissItem(View view, Movie movie);
  }

  private DismissListener dismissListener;

  public MovieRecommendationsAdapter(FragmentActivity activity, Callbacks callbacks,
      DismissListener dismissListener) {
    super(activity, callbacks, R.layout.list_row_movie_rating);
    this.dismissListener = dismissListener;
  }

  @Override
  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist, boolean watching, boolean checkedIn) {
    overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    super.setupOverflowItems(overflow, watched, collected, inWatchlist, watching, checkedIn);
  }

  @Override protected void onOverflowActionSelected(View view, long id, int action, int position,
      String title) {
    switch (action) {
      case R.id.action_dismiss:
        Movie movie = getList().get(position);
        dismissListener.onDismissItem(view, movie);
        break;

      default:
        super.onOverflowActionSelected(view, id, action, position, title);
        break;
    }
  }
}
