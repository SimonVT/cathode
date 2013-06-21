package net.simonvt.trakt.ui.adapter;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.widget.CheckMark;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import javax.inject.Inject;

public class SeasonAdapter extends CursorAdapter {

    private static final String TAG = "SeasonAdapter";

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    private LibraryType mType;

    public SeasonAdapter(Context context, LibraryType type) {
        super(context, null, 0);
        mType = type;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_row_episode, parent, false);

        ViewHolder vh = new ViewHolder(v);
        vh.mCheckbox.setType(mType);
        v.setTag(vh);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes._ID));
        final String title = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.TITLE));
        final int season = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));
        final boolean watched = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.WATCHED)) == 1;
        final boolean inCollection =
                cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.IN_COLLECTION)) == 1;
        final boolean inWatchlist =
                cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.IN_WATCHLIST)) == 1;
        final long firstAired = cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes.FIRST_AIRED));
        final String screen = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SCREEN));

        final ViewHolder vh = (ViewHolder) view.getTag();

        vh.mTitle.setText(title);

        vh.mFirstAired.setText(DateUtils.secondsToDate(context, firstAired));
        vh.mNumber.setText(String.valueOf(episode));

        vh.mScreen.setImage(screen);

        if (mType == LibraryType.COLLECTION) {
            vh.mCheckbox.setVisibility(inCollection ? View.VISIBLE : View.INVISIBLE);
        } else {
            vh.mCheckbox.setVisibility(watched ? View.VISIBLE : View.INVISIBLE);
        }

        updateOverflowMenu(vh.mOverflow, watched, inCollection, inWatchlist);

        vh.mOverflow.setListener(new OverflowView.OverflowActionListener() {
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
                        updateOverflowMenu(vh.mOverflow, true, inCollection, inWatchlist);
                        mEpisodeScheduler.setWatched(id, true);
                        if (mType == LibraryType.WATCHED) vh.mCheckbox.setVisibility(View.VISIBLE);
                        break;

                    case R.id.action_unwatched:
                        updateOverflowMenu(vh.mOverflow, false, inCollection, inWatchlist);
                        mEpisodeScheduler.setWatched(id, false);
                        if (mType == LibraryType.WATCHED) vh.mCheckbox.setVisibility(View.INVISIBLE);
                        break;

                    case R.id.action_collection_add:
                        updateOverflowMenu(vh.mOverflow, watched, true, inWatchlist);
                        mEpisodeScheduler.setIsInCollection(id, true);
                        if (mType == LibraryType.COLLECTION) vh.mCheckbox.setVisibility(View.VISIBLE);
                        break;

                    case R.id.action_collection_remove:
                        updateOverflowMenu(vh.mOverflow, watched, false, inWatchlist);
                        mEpisodeScheduler.setIsInCollection(id, false);
                        if (mType == LibraryType.COLLECTION) vh.mCheckbox.setVisibility(View.INVISIBLE);
                        break;

                    case R.id.action_watchlist_add:
                        updateOverflowMenu(vh.mOverflow, watched, inCollection, true);
                        mEpisodeScheduler.setIsInWatchlist(id, true);
                        break;

                    case R.id.action_watchlist_remove:
                        updateOverflowMenu(vh.mOverflow, watched, inCollection, false);
                        mEpisodeScheduler.setIsInWatchlist(id, false);
                        break;
                }
            }
        });
    }

    private void updateOverflowMenu(OverflowView overflow, boolean watched, boolean inCollection, boolean inWatchlist) {
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

        @InjectView(R.id.screen) RemoteImageView mScreen;

        @InjectView(R.id.infoParent) ViewGroup mInfoParent;
        @InjectView(R.id.title) TextView mTitle;
        @InjectView(R.id.firstAired) TextView mFirstAired;
        @InjectView(R.id.episode) TextView mNumber;
        @InjectView(R.id.overflow) OverflowView mOverflow;
        @InjectView(R.id.checkbox) CheckMark mCheckbox;

        ViewHolder(View v) {
            Views.inject(this, v);
        }
    }
}
