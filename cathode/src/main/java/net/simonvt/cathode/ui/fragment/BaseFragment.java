package net.simonvt.cathode.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import butterknife.ButterKnife;
import net.simonvt.cathode.ui.FragmentContract;

public abstract class BaseFragment extends Fragment implements FragmentContract {

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    ButterKnife.inject(this, view);
  }

  @Override public void onDestroyView() {
    ButterKnife.reset(this);
    super.onDestroyView();
  }

  @Override public String getTitle() {
    return null;
  }

  @Override public String getSubtitle() {
    return null;
  }

  @Override public boolean onBackPressed() {
    return false;
  }
}
