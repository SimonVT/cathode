package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.OverflowView;

public class ShowRecommendationsAdapter extends ShowDescriptionAdapter {

  public ShowRecommendationsAdapter(Context context) {
    super(context);
  }

  @Override protected void setupOverflowItems(OverflowView overflow, boolean inWatchlist) {
    overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    super.setupOverflowItems(overflow, inWatchlist);
  }

  protected void onOverflowActionSelected(long id, int action) {
    switch (action) {
      case R.id.action_dismiss:
        showScheduler.dismissRecommendation(id);
        break;

      default:
        super.onOverflowActionSelected(id, action);
        break;
    }
  }
}
