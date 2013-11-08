package net.simonvt.cathode.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.support.v4.content.AsyncTaskLoader;
import net.simonvt.cathode.util.DateUtils;

public class WatchedLoader extends AsyncTaskLoader<Cursor> {

  private static final String TAG = "WatchedLoader";

  final ForceLoadContentObserver observer;

  private long showId;

  Cursor cursor;

  public WatchedLoader(Context context, long showId) {
    super(context);
    this.showId = showId;
    this.observer = new ForceLoadContentObserver();
  }

  @Override public Cursor loadInBackground() {

    Cursor toWatch = getContext().getContentResolver()
        .query(CathodeContract.Episodes.buildFromShowId(showId), null,
            CathodeContract.Episodes.WATCHED
                + "=0 AND "
                + CathodeContract.Episodes.FIRST_AIRED
                + ">"
                + DateUtils.YEAR_IN_SECONDS
                + " AND "
                + CathodeContract.Episodes.SEASON
                + ">0", null, CathodeContract.Episodes.SEASON
            + " ASC, "
            + CathodeContract.Episodes.EPISODE
            + " ASC LIMIT 1");
    toWatch.registerContentObserver(observer);
    if (toWatch.getCount() == 0) {
      return toWatch;
    }

    Cursor lastWatched = getContext().getContentResolver()
        .query(CathodeContract.Episodes.buildFromShowId(showId), null,
            CathodeContract.Episodes.WATCHED + "=1", null, CathodeContract.Episodes.SEASON
            + " DESC, "
            + CathodeContract.Episodes.EPISODE
            + " DESC LIMIT 1");
    lastWatched.registerContentObserver(observer);
    lastWatched.getCount();

    return new MergeCursor(new Cursor[] {
        toWatch, lastWatched,
    });
  }

  @Override protected void onStartLoading() {
    if (cursor != null) {
      deliverResult(cursor);
    }
    if (takeContentChanged() || cursor == null) {
      forceLoad();
    }
  }

  @Override protected void onStopLoading() {
    // Attempt to cancel the current load task if possible.
    cancelLoad();
  }

  @Override public void onCanceled(Cursor cursor) {
    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
  }

  @Override protected void onReset() {
    super.onReset();

    // Ensure the loader is stopped
    onStopLoading();

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
    cursor = null;
  }
}
