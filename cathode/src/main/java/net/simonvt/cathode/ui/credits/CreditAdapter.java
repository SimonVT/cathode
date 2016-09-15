/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.credits;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.RemoteImageView;

public class CreditAdapter extends RecyclerView.Adapter<CreditAdapter.ViewHolder> {

  public interface OnCreditClickListener {

    void onPersonClicked(long personId);
  }

  private List<Credit> credits;

  private OnCreditClickListener listener;

  public CreditAdapter(List<Credit> credits, OnCreditClickListener listener) {
    this.credits = credits;
    this.listener = listener;
  }

  public void setCredits(final List<Credit> credits) {
    final List<Credit> oldCredits = this.credits;
    this.credits = credits;

    final int oldSize = oldCredits == null ? 0 : oldCredits.size();
    final int newSize = credits.size();
    if (oldSize == 0) {
      if (credits.size() > 0) {
        notifyItemRangeInserted(0, newSize);
      }
    } else {
      DiffUtil.calculateDiff(new DiffUtil.Callback() {
        @Override public int getOldListSize() {
          return oldSize;
        }

        @Override public int getNewListSize() {
          return newSize;
        }

        @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
          final Credit oldItem = oldCredits.get(oldItemPosition);
          final Credit newItem = credits.get(newItemPosition);
          return oldItem.getPersonId() == newItem.getPersonId();
        }

        @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
          final Credit oldItem = oldCredits.get(oldItemPosition);
          final Credit newItem = credits.get(newItemPosition);
          return oldItem.equals(newItem);
        }
      }).dispatchUpdatesTo(this);
    }
  }

  @Override public int getItemCount() {
    return credits == null ? 0 : credits.size();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.credit_item_credit_grid, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Credit credit = credits.get(position);
          listener.onPersonClicked(credit.getPersonId());
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    Credit credit = credits.get(position);

    holder.headshot.setImage(credit.getHeadshot());
    holder.name.setText(credit.getName());
    if (credit.getJob() != null) {
      holder.job.setText(credit.getJob());
    } else {
      holder.job.setText(credit.getCharacter());
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.headshot) RemoteImageView headshot;
    @BindView(R.id.name) TextView name;
    @BindView(R.id.job) TextView job;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
