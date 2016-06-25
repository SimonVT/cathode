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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.FragmentContract;
import net.simonvt.cathode.ui.NavigationListener;

public abstract class BaseFragment extends Fragment
    implements FragmentContract, Toolbar.OnMenuItemClickListener {

  private Unbinder unbinder;

  @BindView(R.id.toolbar) @Nullable Toolbar toolbar;

  private NavigationListener navigationListener;

  CharSequence title;

  private CharSequence subtitle;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof NavigationListener) {
      navigationListener = (NavigationListener) activity;
    }
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public void setTitle(int titleRes) {
    setTitle(getResources().getString(titleRes));
  }

  public void setTitle(CharSequence title) {
    this.title = title;
    if (toolbar != null) {
      toolbar.setTitle(title);
    }
  }

  public void setSubtitle(CharSequence subtitle) {
    this.subtitle = subtitle;
    if (toolbar != null) {
      toolbar.setSubtitle(subtitle);
    }
  }

  protected boolean amITopLevel() {
    return navigationListener.isFragmentTopLevel(this);
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
    unbinder = ButterKnife.bind(this, view);
    if (toolbar != null) {
      toolbar.setOnMenuItemClickListener(this);

      if (displaysMenuIcon()) {
        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp);
      } else {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
      }

      createMenu();

      if (navigationListener != null) {
        toolbar.setNavigationOnClickListener(navigationClickListener);
      }
    }

    setTitle(title);
    setSubtitle(subtitle);
  }

  @Override public void onDestroyView() {
    if (toolbar != null) {
      toolbar.setOnMenuItemClickListener(null);
      toolbar.setNavigationOnClickListener(null);
    }
    unbinder.unbind();
    unbinder = null;
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
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    return false;
  }

  @Override public boolean onBackPressed() {
    return false;
  }
}
