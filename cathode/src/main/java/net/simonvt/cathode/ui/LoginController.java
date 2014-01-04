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
package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.fragment.LoginFragment;
import net.simonvt.menudrawer.MenuDrawer;

public class LoginController extends UiController {

  private LoginFragment loginFragment;

  @InjectView(R.id.drawer) MenuDrawer menuDrawer;

  public static LoginController newInstance(HomeActivity activity) {
    return newInstance(activity, null);
  }

  public static LoginController newInstance(HomeActivity activity, Bundle inState) {
    return new LoginController(activity, inState);
  }

  public LoginController(HomeActivity activity, Bundle inState) {
    super(activity, inState);
    loginFragment =
        (LoginFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_LOGIN);

    if (loginFragment == null) {
      loginFragment = new LoginFragment();
    }

    ButterKnife.inject(this, activity);

    menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_NONE);

    activity.getActionBar().setDisplayHomeAsUpEnabled(false);
    activity.getActionBar().setHomeButtonEnabled(false);

    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.fade_in_front, R.anim.fade_out_back);

    if (loginFragment.isDetached()) {
      transaction.attach(loginFragment);
    } else if (!loginFragment.isAdded()) {
      transaction.add(android.R.id.content, loginFragment, FRAGMENT_LOGIN);
    }

    transaction.commit();
  }

  @Override public void destroy(boolean completely) {
    if (loginFragment.isAdded()) {
      FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
      transaction.setCustomAnimations(R.anim.fade_out_front, R.anim.fade_in_back);
      transaction.remove(loginFragment);
      transaction.commit();
    }
    super.destroy(completely);
  }
}
