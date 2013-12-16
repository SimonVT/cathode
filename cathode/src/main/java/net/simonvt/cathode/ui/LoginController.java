package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.fragment.LoginFragment;
import net.simonvt.menudrawer.MenuDrawer;

public class LoginController extends UiController {

  private static final String TAG = "LoginController";

  private LoginFragment loginFragment;

  @InjectView(R.id.drawer) MenuDrawer menuDrawer;

  public static LoginController newInstance(HomeActivity activity) {
    return new LoginController(activity);
  }

  public LoginController(HomeActivity activity) {
    super(activity);
    loginFragment =
        (LoginFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_LOGIN);

    if (loginFragment == null) {
      loginFragment = new LoginFragment();
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);

    ButterKnife.inject(this, activity);
  }

  @Override public void onAttach() {
    super.onAttach();
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

  @Override public void onDestroy(boolean completely) {
    super.onDestroy(completely);
    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.fade_out_front, R.anim.fade_in_back);

    if (loginFragment.isAdded()) {
      transaction.remove(loginFragment);
    }

    transaction.commit();
  }
}
