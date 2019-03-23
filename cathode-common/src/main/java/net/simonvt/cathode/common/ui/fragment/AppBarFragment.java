/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.util.Views;
import net.simonvt.cathode.common.widget.AppBarRelativeLayout;
import net.simonvt.cathode.common.widget.RemoteImageView;

public abstract class AppBarFragment extends BaseFragment {

  private AppBarRelativeLayout appBarLayout;
  private RemoteImageView backdrop;

  private String backdropUri;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    View v = inflater.inflate(R.layout.fragment_appbar, container, false);

    FrameLayout appBarContent = Views.findRequired(v, R.id.appBarContent);

    View content = createView(inflater, appBarContent, inState);
    appBarContent.addView(content);

    return v;
  }

  protected abstract View createView(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup container, @Nullable Bundle inState);

  @Override public void onViewCreated(View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    appBarLayout = Views.findRequired(view, R.id.appBarLayout);
    appBarLayout.setTitle(title);
    backdrop = Views.findRequired(view, R.id.backdrop);
    backdrop.setImage(backdropUri);
  }

  @Override public void onDestroyView() {
    appBarLayout = null;
    backdrop = null;
    super.onDestroyView();
  }

  @Override public void setTitle(CharSequence title) {
    this.title = title;
    if (appBarLayout != null) {
      appBarLayout.setTitle(title);
    }
  }

  public void setBackdrop(String uri) {
    backdropUri = uri;

    if (backdrop != null) {
      backdrop.setImage(uri);
    }
  }

  public void setBackdrop(String uri, boolean animateIfDifferent) {
    backdropUri = uri;

    if (backdrop != null) {
      backdrop.setImage(uri, animateIfDifferent);
    }
  }
}
