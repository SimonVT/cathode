package net.simonvt.cathode.database;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.CursorLoader;

public class ThrottleableCursorLoader extends CursorLoader {

  private Cursor cursor;

  private long waitUntil;

  private Handler handler = new Handler(Looper.getMainLooper());

  private Runnable postResult = new Runnable() {
    @Override public void run() {
      deliverResult(cursor);
    }
  };

  public ThrottleableCursorLoader(Context context) {
    super(context);
  }

  public ThrottleableCursorLoader(Context context, Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    super(context, uri, projection, selection, selectionArgs, sortOrder);
  }

  public void throttle(long ms) {
    waitUntil = System.currentTimeMillis() + ms;
  }

  @Override public void deliverResult(Cursor cursor) {
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

    super.deliverResult(cursor);
  }
}
