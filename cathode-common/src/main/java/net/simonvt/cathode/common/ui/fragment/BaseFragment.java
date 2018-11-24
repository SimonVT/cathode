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

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.ui.FragmentCallbacks;
import net.simonvt.cathode.common.ui.FragmentContract;

public abstract class BaseFragment extends Fragment
    implements FragmentContract, Toolbar.OnMenuItemClickListener {

  private Unbinder unbinder;

  @Nullable private Toolbar toolbar;

  CharSequence title;

  private CharSequence subtitle;

  private FragmentCallbacks fragmentCallbacks;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof FragmentCallbacks) {
      fragmentCallbacks = (FragmentCallbacks) activity;
    }
  }

  public @Nullable Toolbar getToolbar() {
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
    return fragmentCallbacks.isFragmentTopLevel(this);
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
    toolbar = view.findViewById(R.id.toolbar);
    if (toolbar != null) {
      toolbar.setOnMenuItemClickListener(this);

      if (displaysMenuIcon()) {
        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp);
      } else {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
      }

      createMenu();

      if (fragmentCallbacks != null) {
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
      toolbar = null;
    }
    unbinder.unbind();
    unbinder = null;
    super.onDestroyView();
  }

  private View.OnClickListener navigationClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      onHomeClicked();
    }
  };

  protected void onHomeClicked() {
    fragmentCallbacks.onHomeClicked();
  }

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
