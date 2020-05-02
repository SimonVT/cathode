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

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.suggestions.SuggestionsFragment;

public class MovieSuggestionsFragment extends SuggestionsFragment {

  public static final String TAG =
      "net.simonvt.cathode.ui.suggestions.movies.MovieSuggestionsFragment";

  private MovieSuggestionsPagerAdapter adapter;

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    setTitle(R.string.title_suggestions);
    adapter = new MovieSuggestionsPagerAdapter(requireContext(), getChildFragmentManager());
  }

  @Override protected PagerAdapter getAdapter() {
    return adapter;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies);
    toolbar.inflateMenu(R.menu.fragment_movies_recommended);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        return adapter.getItem(getBinding().pager.getCurrentItem()).onMenuItemClick(item);

      case R.id.menu_search:
        getNavigationListener().onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }
}
