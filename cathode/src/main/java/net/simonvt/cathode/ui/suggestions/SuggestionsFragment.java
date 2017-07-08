/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.suggestions;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.fragment.BaseFragment;

public abstract class SuggestionsFragment extends BaseFragment {

  protected NavigationListener navigationListener;

  @BindView(R.id.tabLayout) TabLayout tabLayout;

  @BindView(R.id.pager) protected ViewPager pager;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_suggestions, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    pager.setAdapter(getAdapter());
    pager.setOffscreenPageLimit(2);
    tabLayout.setupWithViewPager(pager);
  }

  protected abstract PagerAdapter getAdapter();
}
