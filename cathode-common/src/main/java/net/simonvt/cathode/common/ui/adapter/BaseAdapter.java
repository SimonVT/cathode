/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<Type, VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH> {

  private DiffUtil.ItemCallback<Type> diffCallback = new DiffUtil.ItemCallback<Type>() {
    @Override public boolean areItemsTheSame(@NonNull Type oldItem, @NonNull Type newItem) {
      return BaseAdapter.this.areItemsTheSame(oldItem, newItem);
    }

    @Override public boolean areContentsTheSame(@NonNull Type oldItem, @NonNull Type newItem) {
      return BaseAdapter.this.areContentsTheSame(oldItem, newItem);
    }
  };

  private AsyncListDiffer<Type> asyncDiffer = new AsyncListDiffer<>(this, diffCallback);

  private Context context;

  public BaseAdapter(Context context) {
    this.context = context;
    Adapters.registerAdapter(this);
  }

  public Context getContext() {
    return context;
  }

  public void setList(List<Type> list) {
    asyncDiffer.submitList(list);
  }

  public void setList(List<Type> list, Runnable commitCallback) {
    asyncDiffer.submitList(list, commitCallback);
  }

  public void removeItem(Type item) {
    final int position = getList().indexOf(item);
    if (position >= 0) {
      List<Type> newList = new ArrayList<>(getList());
      newList.remove(position);
      setList(newList);
    }
  }

  @Override public int getItemCount() {
    return asyncDiffer.getCurrentList().size();
  }

  protected List<Type> getList() {
    return asyncDiffer.getCurrentList();
  }

  protected abstract boolean areItemsTheSame(@NonNull Type oldItem, @NonNull Type newItem);

  protected boolean areContentsTheSame(@NonNull Type oldItem, @NonNull Type newItem) {
    return oldItem.equals(newItem);
  }
}
