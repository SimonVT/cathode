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
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

public class EpisodeWatchlistAdapter extends CursorAdapter {

  private static final String TAG = "EpisodeWatchlistAdapter";

  @Inject EpisodeTaskScheduler episodeScheduler;

  public EpisodeWatchlistAdapter(Context context, Cursor c, int flags) {
    super(context, c, flags);
    TraktApp.inject(context, this);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v =
        LayoutInflater.from(context).inflate(R.layout.list_row_watchlist_episode, parent, false);

    ViewHolder vh = new ViewHolder(v);
    v.setTag(vh);

    vh.overflow.addItem(R.id.action_watched, R.string.action_watched);
    vh.overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);

    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();

    final long id = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes._ID));
    final String posterUrl =
        cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SCREEN));
    final String title =
        cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.TITLE));
    final long firstAired =
        cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes.FIRST_AIRED));
    final int season = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
    final int episode = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));

    vh.screen.setImage(posterUrl);
    vh.title.setText(title);
    vh.firstAired.setText(DateUtils.millisToString(context, firstAired, false));
    vh.episode.setText(season + "x" + episode);
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
            episodeScheduler.setWatched(id, true);
            break;

          case R.id.action_watchlist_remove:
            episodeScheduler.setIsInWatchlist(id, false);
            break;
        }
      }
    });
  }

  static class ViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TextView firstAired;
    @InjectView(R.id.episode) TextView episode;
    @InjectView(R.id.overflow) OverflowView overflow;

    public ViewHolder(View v) {
      Views.inject(this, v);
    }
  }
}
