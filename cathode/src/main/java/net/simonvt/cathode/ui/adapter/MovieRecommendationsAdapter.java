package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.OverflowView;

public class MovieRecommendationsAdapter extends MoviesAdapter {

  public MovieRecommendationsAdapter(Context context, Cursor c) {
    super(context, c);
  }

  @Override
  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist) {
    overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    super.setupOverflowItems(overflow, watched, collected, inWatchlist);
  }

  @Override protected void onOverflowActionSelected(long id, int action) {
    switch (action) {
      case R.id.action_dismiss:
        movieScheduler.dismissRecommendation(id);
        break;

      default:
        super.onOverflowActionSelected(id, action);
        break;
    }
  }
}
