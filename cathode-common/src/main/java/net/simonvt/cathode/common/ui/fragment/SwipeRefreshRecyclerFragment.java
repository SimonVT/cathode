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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.util.Views;

public abstract class SwipeRefreshRecyclerFragment<T extends RecyclerView.ViewHolder>
    extends GridRecyclerViewFragment<T> implements SwipeRefreshLayout.OnRefreshListener {

  private SwipeRefreshLayout swipeRefreshLayout;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_swiperefresh_recyclerview, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
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
