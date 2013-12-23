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
