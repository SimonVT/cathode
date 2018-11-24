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

package net.simonvt.cathode.common.ui.adapter;

import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseAdapter<VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH> {

  private AdapterNotifier notifier;

  public BaseAdapter() {
    notifier = new AdapterNotifier(this);
    Adapters.registerAdapter(this);
  }

  public abstract long getLastModified(int position);

  public final void notifyChanged() {
    onNotifyChanged();
    notifier.notifyChanged();
  }

  protected void onNotifyChanged() {
  }
}
