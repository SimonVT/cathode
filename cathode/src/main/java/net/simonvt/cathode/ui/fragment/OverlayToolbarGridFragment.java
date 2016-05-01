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

package net.simonvt.cathode.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import net.simonvt.cathode.R;

public abstract class OverlayToolbarGridFragment<T extends RecyclerView.ViewHolder>
    extends ToolbarGridFragment<T> {

  @BindView(R.id.overlayParent) ViewGroup overlayParent;

  @BindView(R.id.content) View layout;
  @BindView(R.id.overlay) TextView overlay;
  private int overlayText;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_search_overlay, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);

    if (overlayText != 0) {
      layout.setVisibility(View.GONE);
      overlay.setVisibility(View.VISIBLE);
      overlay.setText(overlayText);
    } else {
      layout.setVisibility(View.VISIBLE);
      overlay.setVisibility(View.GONE);
    }
  }

  public void setOverlay(int overlayText) {
    if (overlayText == this.overlayText) {
      return;
    }

    this.overlayText = overlayText;

    if (overlayText == 0) {
      if (overlayParent != null) {
        if (layout.getVisibility() == View.GONE) {
          layout.setVisibility(View.VISIBLE);
          layout.setAlpha(0.0f);
        }

        layout.animate().alpha(1.0f);

        overlay.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override public void run() {
            overlay.setVisibility(View.GONE);
          }
        });
      }
    } else if (overlayParent != null) {
      if (layout.getVisibility() != View.GONE) {
        layout.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override public void run() {
            layout.setVisibility(View.GONE);
          }
        });
      }

      overlay.setAlpha(0.0f);
      overlay.animate().alpha(1.0f);

      overlay.setText(overlayText);
    }
  }
}
