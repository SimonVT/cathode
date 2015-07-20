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
package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.FragmentContract;
import net.simonvt.cathode.ui.NavigationClickListener;
import net.simonvt.cathode.widget.AppBarRelativeLayout;
import net.simonvt.cathode.widget.ToolbarHelper;

public abstract class BaseFragment extends Fragment
    implements FragmentContract, Toolbar.OnMenuItemClickListener {

  @Bind(R.id.appBarLayout) @Nullable AppBarRelativeLayout appBarLayout;

  @Bind(R.id.toolbar) @Nullable Toolbar toolbar;

  private ToolbarHelper toolbarHelper;

  private NavigationClickListener navigationListener;

  private CharSequence title;

  private CharSequence subtitle;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof NavigationClickListener) {
      navigationListener = (NavigationClickListener) activity;
    }
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public ToolbarHelper getToolbarHelper() {
    return toolbarHelper;
  }

  public void setTitle(int titleRes) {
    setTitle(getResources().getString(titleRes));
  }

  public void setTitle(CharSequence title) {
    this.title = title;
    if (appBarLayout != null) {
      appBarLayout.setTitle(title);
    } else if (toolbar != null) {
      toolbar.setTitle(title);
    }
  }

  public void setSubtitle(CharSequence subtitle) {
    this.subtitle = subtitle;
    if (toolbar != null) {
      toolbar.setSubtitle(subtitle);
    }
  }

  public boolean displaysMenuIcon() {
    return false;
  }

  public void invalidateMenu() {
    if (toolbar != null) {
      toolbar.getMenu().clear();
      createMenu();
    }
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    ButterKnife.bind(this, view);
    if (toolbar != null) {
      toolbar.setOnMenuItemClickListener(this);
      toolbarHelper = new ToolbarHelper(this, toolbar, displaysMenuIcon());
      createMenu();

      if (navigationListener != null) {
        toolbar.setNavigationOnClickListener(navigationClickListener);
      }
    }

    setTitle(title);
    setSubtitle(subtitle);
  }

  @Override public void onDestroyView() {
    ButterKnife.unbind(this);
    super.onDestroyView();
  }

  private View.OnClickListener navigationClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      navigationListener.onHomeClicked();
    }
  };

  private void createMenu() {
    createMenu(toolbar);
  }

  public void createMenu(Toolbar toolbar) {
    toolbarHelper.createMenu();
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    return toolbarHelper.onMenuItemClick(item);
  }

  @Override public boolean onBackPressed() {
    return false;
  }
}
