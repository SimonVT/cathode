package net.simonvt.trakt.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import net.simonvt.trakt.R;
import net.simonvt.trakt.ui.fragment.LoginFragment;

public class LoginController extends UiController {

  private static final String TAG = "LoginController";

  private LoginFragment loginFragment;

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

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
  }

  @Override
  public void onAttach() {
    super.onAttach();

    activity.getActionBar().setDisplayHomeAsUpEnabled(false);
    activity.getActionBar().setHomeButtonEnabled(false);

    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.fade_out_back, R.anim.fade_in_front);

    if (loginFragment.isDetached()) {
      transaction.attach(loginFragment);
    } else if (!loginFragment.isAdded()) {
      transaction.add(android.R.id.content, loginFragment, FRAGMENT_LOGIN);
    }

    transaction.commit();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.fade_out_front, R.anim.fade_in_back);

    if (loginFragment.isAdded() && !loginFragment.isDetached()) {
      transaction.remove(loginFragment);
    }

    transaction.commit();
  }
}
