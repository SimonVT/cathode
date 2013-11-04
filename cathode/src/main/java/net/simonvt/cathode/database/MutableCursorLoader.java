package net.simonvt.cathode.database;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.AsyncTaskLoader;

public class MutableCursorLoader extends AsyncTaskLoader<MutableCursor> {
  final ForceLoadContentObserver observer;

  private Uri uri;
  private String[] projection;
  private String selection;
  private String[] selectionArgs;
  private String sortOrder;

  private MutableCursor cursor;

  private long waitUntil;

  private Handler handler = new Handler(Looper.getMainLooper());

  private Runnable postResult = new Runnable() {
    @Override public void run() {
      deliverResult(cursor);
    }
  };

  public MutableCursorLoader(Context context, Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    super(context);
    this.observer = new ForceLoadContentObserver();
    this.uri = uri;
    this.projection = projection;
    this.selection = selection;
    this.selectionArgs = selectionArgs;
    this.sortOrder = sortOrder;
  }

  @Override
  public MutableCursor loadInBackground() {
    Cursor cursor = getContext().getContentResolver()
        .query(uri, projection, selection, selectionArgs, sortOrder);
    MutableCursor result = null;
    if (cursor != null) {
      result = new MutableCursor(getContext(), cursor);
      cursor.close();
      result.registerContentObserver(observer);
    }
    return result;
  }

  public void throttle(long ms) {
    waitUntil = System.currentTimeMillis() + ms;
  }

  @Override
  public void deliverResult(MutableCursor cursor) {
    if (isReset()) {
      return;
    }

    this.cursor = cursor;
    handler.removeCallbacks(postResult);

    final long now = System.currentTimeMillis();
    if (now < waitUntil) {
      handler.postDelayed(postResult, waitUntil - now + 250);
      return;
    }

    if (isStarted()) {
      super.deliverResult(cursor);
    }
  }

  @Override
  protected void onStartLoading() {
    if (cursor != null) {
      deliverResult(cursor);
    }
    if (takeContentChanged() || cursor == null) {
      forceLoad();
    }
  }

  @Override
  protected void onStopLoading() {
    cancelLoad();
  }

  @Override
  protected void onReset() {
    super.onReset();
    onStopLoading();
    cursor = null;
  }
}
