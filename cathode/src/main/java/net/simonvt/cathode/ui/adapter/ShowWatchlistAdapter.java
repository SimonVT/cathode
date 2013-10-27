package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

public class ShowWatchlistAdapter extends ShowDescriptionAdapter {

  public interface RemoveListener {

    void onRemoveItem(View view, int position);
  }

  private RemoveListener listener;

  public ShowWatchlistAdapter(Context context, Cursor cursor, RemoveListener listener) {
    super(context, cursor);
    this.listener = listener;
  }

  @Override protected void onWatchlistRemove(View view, int position, long id) {
    listener.onRemoveItem(view, position);
    super.onWatchlistRemove(view, position, id);
  }
}
