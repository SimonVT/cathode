package net.simonvt.trakt.ui.adapter;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.SeasonTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.widget.OverflowView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

public class SeasonsAdapter extends CursorAdapter {

    @Inject SeasonTaskScheduler mSeasonScheduler;

    private Resources mResources;

    private LibraryType mType;

    public SeasonsAdapter(Context context, LibraryType type) {
        super(context, null, 0);
        TraktApp.inject(context, this);
        mResources = context.getResources();
        mType = type;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_row_season, parent, false);

        ViewHolder vh = new ViewHolder(v);
        v.setTag(vh);

        return v;
    }

    private void bindWatched(Context context, ViewHolder vh, Cursor cursor) {
        final int airdateCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.AIRDATE_COUNT));
        final int unairedCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.UNAIRED_COUNT));
        final int watchedCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.WATCHED_COUNT));
        final int toWatch = airdateCount - unairedCount - watchedCount;

        vh.mProgress.setMax(airdateCount);
        vh.mProgress.setProgress(watchedCount);

        TypedArray a = context.obtainStyledAttributes(new int[] {
                android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondary,
        });
        ColorStateList primaryColor = a.getColorStateList(0);
        ColorStateList secondaryColor = a.getColorStateList(1);

        final String unwatched = mResources.getQuantityString(R.plurals.x_unwatched, toWatch, toWatch);
        String unaired;
        if (unairedCount > 0) {
            unaired = mResources.getString(R.string.x_unaired, unairedCount);
        } else {
            unaired = "";
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder().append(unwatched).append(" ").append(unaired);
        ssb.setSpan(
                new TextAppearanceSpan(null, 0, 0, primaryColor, null),
                0, unwatched.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (unairedCount > 0) {
            ssb.setSpan(
                    new TextAppearanceSpan(null, 0, 0, secondaryColor, null),
                    unwatched.length(), unwatched.length() + unaired.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        vh.mSummary.setText(ssb.toString());
    }

    private void bindCollection(Context context, ViewHolder vh, Cursor cursor) {
        final int airdateCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.AIRDATE_COUNT));
        final int unairedCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.UNAIRED_COUNT));
        final int collectedCount =
                cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.IN_COLLECTION_COUNT));
        final int toCollect = airdateCount - unairedCount - collectedCount;

        vh.mProgress.setMax(airdateCount);
        vh.mProgress.setProgress(collectedCount);

        final String unwatched = mResources.getQuantityString(R.plurals.x_uncollected, toCollect, toCollect);
        vh.mSummary.setText(unwatched);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();

        final int seasonId = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons._ID));
        final int seasonNumber = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.SEASON));
        final int airdateCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.AIRDATE_COUNT));
        final int unairedCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.UNAIRED_COUNT));
        final int airedCount = airdateCount - unairedCount;
        final int collectedCount =
                cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.IN_COLLECTION_COUNT));
        final int watchedCount = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Seasons.WATCHED_COUNT));

        switch (mType) {
            case WATCHLIST:
            case WATCHED:
                bindWatched(context, vh, cursor);
                break;

            case COLLECTION:
                bindCollection(context, vh, cursor);
                break;
        }

        vh.mOverflow.removeItems();
        if (airedCount - collectedCount > 0) {
            vh.mOverflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
        }
        if (collectedCount > 0) {
            vh.mOverflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
        }
        if (airedCount - watchedCount > 0) {
            vh.mOverflow.addItem(R.id.action_watched, R.string.action_watched);
        }
        if (watchedCount > 0) {
            vh.mOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
        }

        vh.mTitle.setText(mResources.getQuantityString(R.plurals.season_x, seasonNumber, seasonNumber));
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
                        mSeasonScheduler.setWatched(seasonId, true);
                        break;

                    case R.id.action_unwatched:
                        mSeasonScheduler.setWatched(seasonId, false);
                        break;

                    case R.id.action_collection_add:
                        mSeasonScheduler.setInCollection(seasonId, true);
                        break;

                    case R.id.action_collection_remove:
                        mSeasonScheduler.setInCollection(seasonId, false);
                        break;
                }
            }
        });
    }

    static class ViewHolder {

        @InjectView(R.id.title) TextView mTitle;
        @InjectView(R.id.progress) ProgressBar mProgress;
        @InjectView(R.id.summary) TextView mSummary;
        @InjectView(R.id.overflow) OverflowView mOverflow;

        ViewHolder(View v) {
            Views.inject(this, v);
            mOverflow.addItem(R.id.action_watched, R.string.action_watched);
            mOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
        }
    }
}
