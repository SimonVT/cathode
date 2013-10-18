package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class ShowRecommendationsAdapter extends CursorAdapter {

  private static final String TAG = "ShowDescriptionAdapter";

  public static final String[] PROJECTION = new String[] {
      CathodeDatabase.Tables.SHOWS + "." + BaseColumns._ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TITLE,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.OVERVIEW,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.POSTER,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.TVDB_ID,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.WATCHED_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_COLLECTION_COUNT,
      CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows.IN_WATCHLIST,
  };

  @Inject ShowTaskScheduler showScheduler;

  private Context context;

  public ShowRecommendationsAdapter(Context context) {
    super(context, null, 0);
    this.context = context;
    CathodeApp.inject(context, this);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_search_show, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();

    final long id = cursor.getLong(cursor.getColumnIndex(CathodeContract.Shows._ID));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.WATCHED_COUNT)) > 0;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_COLLECTION_COUNT)) > 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Shows.IN_WATCHLIST)) == 1;

    vh.indicator.setWatched(watched);
    vh.indicator.setCollected(inCollection);
    vh.indicator.setInWatchlist(inWatchlist);

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.POSTER)));
    vh.title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.TITLE)));
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Shows.OVERVIEW)));

    vh.overflow.removeItems();
    vh.overflow.addItem(R.id.action_dismiss, R.string.action_recommendation_dismiss);
    if (inWatchlist) {
      vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      vh.overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }

    vh.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override
      public void onPopupShown() {
      }

      @Override
      public void onPopupDismissed() {
      }

      @Override
      public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_watchlist_add:
            showScheduler.setIsInWatchlist(id, true);
            break;

          case R.id.action_watchlist_remove:
            showScheduler.setIsInWatchlist(id, false);
            break;

          case R.id.action_dismiss:
            showScheduler.dismissRecommendation(id);
            break;
        }
      }
    });
  }

  static class ViewHolder {

    @InjectView(R.id.poster) RemoteImageView poster;
    @InjectView(R.id.indicator) IndicatorView indicator;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.overview) TextView overview;
    @InjectView(R.id.overflow) OverflowView overflow;

    ViewHolder(View v) {
      Views.inject(this, v);
    }
  }
}
