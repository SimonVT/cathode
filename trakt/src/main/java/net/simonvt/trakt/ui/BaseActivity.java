package net.simonvt.trakt.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public abstract class BaseActivity extends FragmentActivity {

  @SuppressWarnings("unchecked")
  protected <T extends Fragment> T findFragment(String tag) {
    return (T) getSupportFragmentManager().findFragmentByTag(tag);
  }
}
