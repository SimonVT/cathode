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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import com.google.android.material.tabs.TabLayout;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.BaseFragment;
import net.simonvt.cathode.ui.NavigationListener;

public abstract class SuggestionsFragment extends BaseFragment {

  protected NavigationListener navigationListener;

  @BindView(R.id.tabLayout) TabLayout tabLayout;

  @BindView(R.id.pager) protected ViewPager pager;

  @Override public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    navigationListener = (NavigationListener) requireActivity();
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    return inflater.inflate(R.layout.fragment_suggestions, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    pager.setAdapter(getAdapter());
    pager.setOffscreenPageLimit(2);
    tabLayout.setupWithViewPager(pager);
  }

  protected abstract PagerAdapter getAdapter();
}
