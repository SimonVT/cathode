/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
