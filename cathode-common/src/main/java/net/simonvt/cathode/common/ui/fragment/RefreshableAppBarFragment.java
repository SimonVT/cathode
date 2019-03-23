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

package net.simonvt.cathode.common.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.util.Views;

public abstract class RefreshableAppBarFragment extends AppBarFragment
    implements SwipeRefreshLayout.OnRefreshListener {

  private SwipeRefreshLayout swipeRefreshLayout;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    View v = inflater.inflate(R.layout.fragment_appbar_refreshable, container, false);

    FrameLayout appBarContent = Views.findRequired(v, R.id.appBarContent);

    View content = createView(inflater, appBarContent, inState);
    appBarContent.addView(content);

    return v;
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    swipeRefreshLayout = Views.findRequired(view, R.id.swipeRefresh);
    swipeRefreshLayout.setOnRefreshListener(this);
    swipeRefreshLayout.setColorSchemeResources(getColorScheme());
  }

  @Override public void onDestroyView() {
    swipeRefreshLayout = null;
    super.onDestroyView();
  }

  public int[] getColorScheme() {
    return new int[] {
        R.color.refreshableColor1, R.color.refreshableColor2, R.color.refreshableColor3,
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
