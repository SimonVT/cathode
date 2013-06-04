package net.simonvt.trakt.ui;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.trakt.R;
import net.simonvt.trakt.ui.fragment.LoginFragment;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

public class LoginController extends UiController {

    private static final String TAG = "LoginController";

    private LoginFragment mLoginFragment;

    public static LoginController newInstance(HomeActivity activity) {
        return new LoginController(activity);
    }

    public LoginController(HomeActivity activity) {
        super(activity);
        mLoginFragment =
                (LoginFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_LOGIN);

        if (mLoginFragment == null) {
            mLoginFragment = new LoginFragment();
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override
    public void onAttach() {
        super.onAttach();

        mActivity.getActionBar().setDisplayHomeAsUpEnabled(false);
        mActivity.getActionBar().setHomeButtonEnabled(false);

        FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_out_back, R.anim.fade_in_front);

        if (mLoginFragment.isDetached()) {
            transaction.attach(mLoginFragment);
        } else if (!mLoginFragment.isAdded()) {
            transaction.add(android.R.id.content, mLoginFragment, FRAGMENT_LOGIN);
        }

        transaction.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_out_front, R.anim.fade_in_back);

        if (mLoginFragment.isAdded() && !mLoginFragment.isDetached()) {
            transaction.remove(mLoginFragment);
        }

        transaction.commit();
    }
}
