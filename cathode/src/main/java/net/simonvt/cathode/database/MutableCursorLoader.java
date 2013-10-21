package net.simonvt.cathode.database;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

public class MutableCursorLoader extends AsyncTaskLoader<MutableCursor> {
  final ForceLoadContentObserver observer;

  Uri uri;
  String[] projection;
  String selection;
  String[] selectionArgs;
  String sortOrder;

  MutableCursor cursor;

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
      result = new MutableCursor(cursor);
      cursor.close();
      result.registerContentObserver(observer);
    }
    return result;
  }

  @Override
  public void deliverResult(MutableCursor cursor) {
    if (isReset()) {
      return;
    }

    this.cursor = cursor;

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
