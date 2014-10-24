/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

import android.support.v7.widget.GridLayoutManager;

public class HeaderSpanLookup extends GridLayoutManager.SpanSizeLookup {

  public static final int TYPE_HEADER = 0x11EAD;

  private HeaderCursorAdapter headerAdapter;

  private int columnCount;

  public HeaderSpanLookup(HeaderCursorAdapter headerAdapter, int columnCount) {
    this.headerAdapter = headerAdapter;
    this.columnCount = columnCount;
    setSpanIndexCacheEnabled(true);
  }

  @Override public int getSpanSize(int position) {
    return headerAdapter.getItemViewType(position) == TYPE_HEADER ? columnCount : 1;
  }
}
