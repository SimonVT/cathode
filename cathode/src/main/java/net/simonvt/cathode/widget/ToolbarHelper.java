/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.widget;

import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.dialog.AboutDialog;

public class ToolbarHelper {

  private Fragment fragment;

  private Toolbar toolbar;

  public ToolbarHelper(Fragment fragment, Toolbar toolbar, boolean displayMenuIcon) {
    this.fragment = fragment;
    this.toolbar = toolbar;

    if (displayMenuIcon) {
      toolbar.setNavigationIcon(R.drawable.ic_menu);
    } else {
      toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
    }
  }

  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        new AboutDialog().show(fragment.getFragmentManager(), HomeActivity.DIALOG_ABOUT);
        return true;
    }

    return false;
  }

  public void createMenu() {
    toolbar.inflateMenu(R.menu.activity_base);
  }
}
