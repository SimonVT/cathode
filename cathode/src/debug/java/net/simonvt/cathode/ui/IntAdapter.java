/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;

public class IntAdapter extends BaseAdapter {

  private int[] ints;

  public IntAdapter(int[] ints) {
    this.ints = ints;
  }

  @Override public int getCount() {
    return ints.length;
  }

  @Override public Integer getItem(int position) {
    return ints[position];
  }

  public int getPositionForValue(int value) {
    for (int i = 0; i < ints.length; i++) {
      if (value == ints[i]) {
        return i;
      }
    }

    throw new IllegalArgumentException("No position found for value " + value);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      v = LayoutInflater.from(parent.getContext())
          .inflate(android.R.layout.simple_spinner_item, parent, false);
    }

    TextView tv = ButterKnife.findById(v, android.R.id.text1);

    tv.setText(String.format("%d", ints[position]));

    return tv;
  }
}
