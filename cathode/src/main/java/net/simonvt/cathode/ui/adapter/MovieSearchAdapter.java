package net.simonvt.cathode.ui.adapter;

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
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.widget.IndicatorView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class MovieSearchAdapter extends CursorAdapter {

  private static final String TAG = "MovieSearchAdapter";

  @Inject MovieTaskScheduler movieScheduler;

  private Context context;

  public MovieSearchAdapter(Context context) {
    super(context, null, 0);
    this.context = context;
    CathodeApp.inject(context, this);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_search_movie, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();

    final int id = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies._ID));
    final boolean watched = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_WATCHLIST)) == 1;

    vh.poster.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.POSTER)));
    vh.title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.TITLE)));
    vh.overview.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.OVERVIEW)));

    vh.indicator.setWatched(watched);
    vh.indicator.setCollected(inCollection);
    vh.indicator.setInWatchlist(inWatchlist);

    vh.overflow.removeItems();
    if (watched) {
      vh.overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else if (inWatchlist) {
      vh.overflow.addItem(R.id.action_watched, R.string.action_watched);
      vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else {
      vh.overflow.addItem(R.id.action_watched, R.string.action_watched);
      vh.overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }

    if (inCollection) {
      vh.overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      vh.overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
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
          case R.id.action_watched:
            movieScheduler.setWatched(id, true);
            break;

          case R.id.action_unwatched:
            movieScheduler.setWatched(id, false);
            break;

          case R.id.action_watchlist_add:
            movieScheduler.setIsInWatchlist(id, true);
            break;

          case R.id.action_watchlist_remove:
            movieScheduler.setIsInWatchlist(id, false);
            break;

          case R.id.action_collection_add:
            movieScheduler.setIsInCollection(id, true);
            break;

          case R.id.action_collection_remove:
            movieScheduler.setIsInCollection(id, false);
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
