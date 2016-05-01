/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;

public abstract class RefreshableAppBarFragment extends AppBarFragment
    implements SwipeRefreshLayout.OnRefreshListener {

  @BindView(R.id.swipeRefresh) SwipeRefreshLayout swipeRefreshLayout;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    View v = inflater.inflate(R.layout.fragment_appbar_refreshable, container, false);

    FrameLayout appBarContent = ButterKnife.findById(v, R.id.appBarContent);

    View content = createView(inflater, appBarContent, inState);
    appBarContent.addView(content);

    return v;
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    swipeRefreshLayout.setOnRefreshListener(this);
    swipeRefreshLayout.setColorSchemeResources(getColorScheme());
  }

  public int[] getColorScheme() {
    return new int[] {
        R.color.watchedColor, R.color.collectedColor, R.color.watchlistColor
    };
  }

  public SwipeRefreshLayout getSwipeRefreshLayout() {
    return swipeRefreshLayout;
  }

  public void setRefreshing(boolean refreshing) {
    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setRefreshing(refreshing);
    }
  }
}
