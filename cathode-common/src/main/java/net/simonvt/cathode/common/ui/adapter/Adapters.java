/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

import android.support.v7.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Adapters {

  private Adapters() {
  }

  private static final List<WeakReference<RecyclerView.Adapter>> ADAPTERS = new ArrayList<>();

  public static void registerAdapter(RecyclerView.Adapter adapter) {
    for (int i = ADAPTERS.size() - 1; i >= 0; i--) {
      WeakReference<RecyclerView.Adapter> ref = ADAPTERS.get(i);
      RecyclerView.Adapter a = ref.get();
      if (a == null) {
        ADAPTERS.remove(ref);
      }

      if (adapter == a) {
        throw new IllegalStateException("Adapter already registered");
      }
    }

    ADAPTERS.add(new WeakReference<>(adapter));
  }

  public static void unregisterAdapter(RecyclerView.Adapter adapter) {
    for (int i = ADAPTERS.size() - 1; i >= 0; i--) {
      WeakReference<RecyclerView.Adapter> ref = ADAPTERS.get(i);
      RecyclerView.Adapter a = ref.get();
      if (a == null || a == adapter) {
        ADAPTERS.remove(ref);
      }
    }
  }

  public static void notifyAdapters() {
    for (int i = ADAPTERS.size() - 1; i >= 0; i--) {
      WeakReference<RecyclerView.Adapter> ref = ADAPTERS.get(i);
      RecyclerView.Adapter adapter = ref.get();

      if (adapter != null) {
        adapter.notifyDataSetChanged();
      }
    }
  }
}
