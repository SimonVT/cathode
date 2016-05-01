/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.AppBarRelativeLayout;
import net.simonvt.cathode.widget.RemoteImageView;

public abstract class AppBarFragment extends BaseFragment {

  @BindView(R.id.appBarLayout) AppBarRelativeLayout appBarLayout;

  @BindView(R.id.backdrop) RemoteImageView backdrop;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    View v = inflater.inflate(R.layout.fragment_appbar, container, false);

    FrameLayout appBarContent = ButterKnife.findById(v, R.id.appBarContent);

    View content = createView(inflater, appBarContent, inState);
    appBarContent.addView(content);

    return v;
  }

  protected abstract View createView(LayoutInflater inflater, ViewGroup container, Bundle inState);

  @Override public void setTitle(CharSequence title) {
    this.title = title;
    if (appBarLayout != null) {
      appBarLayout.setTitle(title);
    }
  }

  public void setBackdrop(String url) {
    backdrop.setImage(url);
  }

  public void setBackdrop(String url, boolean animateIfDifferent) {
    backdrop.setImage(url, animateIfDifferent);
  }
}
