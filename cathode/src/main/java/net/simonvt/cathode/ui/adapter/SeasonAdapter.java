package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.widget.CheckMark;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.cathode.widget.TimeStamp;

public class SeasonAdapter extends CursorAdapter {

  private static final String TAG = "SeasonAdapter";

  @Inject EpisodeTaskScheduler episodeScheduler;

  private LibraryType type;

  public SeasonAdapter(Context context, LibraryType type) {
    super(context, null, 0);
    this.type = type;
    CathodeApp.inject(context, this);
  }

  @Override
  public void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.list_row_episode, parent, false);

    ViewHolder vh = new ViewHolder(v);
    vh.checkbox
        .setType(type == LibraryType.COLLECTION ? LibraryType.COLLECTION : LibraryType.WATCHED);
    v.setTag(vh);

    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    final long id = cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes._ID));
    final String title =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.TITLE));
    final int season = cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
    final int episode =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));
    final boolean watched =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.WATCHED)) == 1;
    final boolean inCollection =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_COLLECTION)) == 1;
    final boolean inWatchlist =
        cursor.getInt(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.IN_WATCHLIST)) == 1;
    final long firstAired =
        cursor.getLong(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.FIRST_AIRED));
    final String screen =
        cursor.getString(cursor.getColumnIndexOrThrow(CathodeContract.Episodes.SCREEN));

    final ViewHolder vh = (ViewHolder) view.getTag();

    vh.title.setText(title);

    vh.firstAired.setTimeInMillis(firstAired);
    vh.firstAired.setExtended(true);
    vh.number.setText(String.valueOf(episode));

    vh.screen.setImage(screen);

    if (type == LibraryType.COLLECTION) {
      vh.checkbox.setVisibility(inCollection ? View.VISIBLE : View.INVISIBLE);
    } else {
      vh.checkbox.setVisibility(watched ? View.VISIBLE : View.INVISIBLE);
    }

    updateOverflowMenu(vh.overflow, watched, inCollection, inWatchlist);

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
            updateOverflowMenu(vh.overflow, true, inCollection, inWatchlist);
            episodeScheduler.setWatched(id, true);
            if (type == LibraryType.WATCHED) vh.checkbox.setVisibility(View.VISIBLE);
            break;

          case R.id.action_unwatched:
            updateOverflowMenu(vh.overflow, false, inCollection, inWatchlist);
            episodeScheduler.setWatched(id, false);
            if (type == LibraryType.WATCHED) vh.checkbox.setVisibility(View.INVISIBLE);
            break;

          case R.id.action_collection_add:
            updateOverflowMenu(vh.overflow, watched, true, inWatchlist);
            episodeScheduler.setIsInCollection(id, true);
            if (type == LibraryType.COLLECTION) vh.checkbox.setVisibility(View.VISIBLE);
            break;

          case R.id.action_collection_remove:
            updateOverflowMenu(vh.overflow, watched, false, inWatchlist);
            episodeScheduler.setIsInCollection(id, false);
            if (type == LibraryType.COLLECTION) vh.checkbox.setVisibility(View.INVISIBLE);
            break;

          case R.id.action_watchlist_add:
            updateOverflowMenu(vh.overflow, watched, inCollection, true);
            episodeScheduler.setIsInWatchlist(id, true);
            break;

          case R.id.action_watchlist_remove:
            updateOverflowMenu(vh.overflow, watched, inCollection, false);
            episodeScheduler.setIsInWatchlist(id, false);
            break;
        }
      }
    });
  }

  private void updateOverflowMenu(OverflowView overflow, boolean watched, boolean inCollection,
      boolean inWatchlist) {
    overflow.removeItems();
    if (watched) {
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else {
      overflow.addItem(R.id.action_watched, R.string.action_watched);
    }

    if (inCollection) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }

    if (inWatchlist) {
      overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
    } else if (!watched) {
      overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
    }
  }

  static class ViewHolder {

    @InjectView(R.id.screen) RemoteImageView screen;

    @InjectView(R.id.infoParent) ViewGroup infoParent;
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.firstAired) TimeStamp firstAired;
    @InjectView(R.id.episode) TextView number;
    @InjectView(R.id.overflow) OverflowView overflow;
    @InjectView(R.id.checkbox) CheckMark checkbox;

    ViewHolder(View v) {
      Views.inject(this, v);
    }
  }
}
