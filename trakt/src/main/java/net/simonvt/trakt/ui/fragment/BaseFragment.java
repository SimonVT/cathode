package net.simonvt.trakt.ui.fragment;

import butterknife.Views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);
    }

    @Override
    public void onDestroyView() {
        Views.reset(this);
        super.onDestroyView();
    }

    public String getTitle() {
        return null;
    }

    public String getSubtitle() {
        return null;
    }
}
