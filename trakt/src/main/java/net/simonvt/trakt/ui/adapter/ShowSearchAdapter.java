package net.simonvt.trakt.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.ShowTaskScheduler;
import net.simonvt.trakt.widget.IndicatorView;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

public class ShowSearchAdapter extends CursorAdapter {

  private static final String TAG = "ShowSearchAdapter";

  @Inject ShowTaskScheduler showScheduler;

  private Context context;

  public ShowSearchAdapter(Context context) {
    super(context, null, 0);
    this.context = context;
    TraktApp.inject(context, this);
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

    final long id = cursor.getLong(cursor.getColumnIndex(TraktContract.Shows._ID));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.WATCHED_COUNT)) > 0;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.IN_COLLECTION_COUNT)) > 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(TraktContract.Shows.IN_WATCHLIST)) == 1;

    vh.indicator.setWatched(watched);
    vh.indicator.setCollected(inCollection);
    vh.indicator.setInWatchlist(inWatchlist);

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(TraktContract.Shows.POSTER)));
    vh.title.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Shows.TITLE)));
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Shows.OVERVIEW)));

    vh.overflow.removeItems();
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
