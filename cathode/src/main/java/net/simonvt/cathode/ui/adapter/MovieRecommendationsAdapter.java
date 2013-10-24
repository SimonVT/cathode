package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.OverflowView;

public class MovieRecommendationsAdapter extends MoviesAdapter {

  public interface DismissListener {
    void onDismissItem(View view, int position);
  }

  private DismissListener listener;

  public MovieRecommendationsAdapter(Context context, Cursor c, DismissListener listener) {
    super(context, c);
    this.listener = listener;
  }

  @Override
  protected void setupOverflowItems(OverflowView overflow, boolean watched, boolean collected,
      boolean inWatchlist) {
    overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    super.setupOverflowItems(overflow, watched, collected, inWatchlist);
  }

  @Override protected void onOverflowActionSelected(View view, long id, int action, int position) {
    switch (action) {
      case R.id.action_dismiss:
        movieScheduler.dismissRecommendation(id);
        listener.onDismissItem(view, position);
        break;

      default:
        super.onOverflowActionSelected(view, id, action, position);
        break;
    }
  }
}
