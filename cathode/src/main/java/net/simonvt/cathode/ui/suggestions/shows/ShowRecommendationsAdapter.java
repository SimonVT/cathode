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
package net.simonvt.cathode.ui.suggestions.shows;

import android.content.Context;
import android.view.View;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class ShowRecommendationsAdapter extends ShowDescriptionAdapter {

  public interface DismissListener {

    void onDismissItem(View view, Show show);
  }

  private DismissListener listener;

  public ShowRecommendationsAdapter(Context context, ShowCallbacks callbacks,
      DismissListener listener) {
    super(context, callbacks);
    this.listener = listener;
  }

  @Override protected void setupOverflowItems(OverflowView overflow, boolean inWatchlist) {
    overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    super.setupOverflowItems(overflow, inWatchlist);
  }

  protected void onOverflowActionSelected(View view, long id, int action, int position) {
    switch (action) {
      case R.id.action_dismiss:
        Show show = getList().get(position);
        listener.onDismissItem(view, show);
        break;

      default:
        super.onOverflowActionSelected(view, id, action, position);
        break;
    }
  }
}
