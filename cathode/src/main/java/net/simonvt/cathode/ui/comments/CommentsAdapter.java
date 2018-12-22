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
package net.simonvt.cathode.ui.comments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.CircleTransformation;
import net.simonvt.cathode.common.widget.RemoteImageView;

public class CommentsAdapter extends BaseAdapter<Comment, CommentsAdapter.ViewHolder> {

  private static final String STATE_REVEALED_SPOILERS =
      "net.simonvt.cathode.ui.comments.CommentsAdapter";

  private static final int COMMENT = 0;
  private static final int REPLY = 1;
  private static final int USER_COMMENT = 2;
  private static final int USER_REPLY = 3;

  public interface CommentCallbacks {

    void onCommentClick(long commentId, String comment, boolean spoiler, boolean isUserComment);

    void onLikeComment(long commentId);

    void onUnlikeComment(long commentId);
  }

  private boolean showsReplies;

  private CommentCallbacks callbacks;

  private Set<Long> revealedSpoilers = new HashSet<>();

  private int tintColor;

  private int likedTintColor;

  public CommentsAdapter(Context context, boolean showsReplies, CommentCallbacks callbacks) {
    super(context);
    this.showsReplies = showsReplies;
    this.callbacks = callbacks;
    setHasStableIds(true);

    tintColor = context.getResources().getColor(R.color.commentIconTint);
    likedTintColor = context.getResources().getColor(R.color.commentLikedTint);
  }

  public void restoreState(Bundle state) {
    long[] revealed = state.getLongArray(STATE_REVEALED_SPOILERS);
    for (long id : revealed) {
      revealedSpoilers.add(id);
    }
  }

  public Bundle saveState() {
    Bundle state = new Bundle();

    long[] revealed = new long[revealedSpoilers.size()];
    int i = 0;
    for (Long id : revealedSpoilers) {
      revealed[i++] = id;
    }

    state.putLongArray(STATE_REVEALED_SPOILERS, revealed);

    return state;
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override public int getItemViewType(int position) {
    if (showsReplies && position > 0) {
      return REPLY;
    }

    return COMMENT;
  }

  @Override protected boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v;
    if (viewType == COMMENT) {
      v = LayoutInflater.from(getContext()).inflate(R.layout.comment_post, parent, false);
    } else {
      v = LayoutInflater.from(getContext()).inflate(R.layout.comment_reply, parent, false);
    }

    final ViewHolder holder = new ViewHolder(v);
    holder.avatar.addTransformation(new CircleTransformation());

    Drawable likes = getContext().getResources().getDrawable(R.drawable.ic_thumb_up);
    holder.likeDrawable = DrawableCompat.wrap(likes);
    holder.likes.setCompoundDrawablesWithIntrinsicBounds(holder.likeDrawable, null, null, null);

    if (holder.replies != null) {
      Drawable replies = getContext().getResources().getDrawable(R.drawable.ic_comment);
      holder.repliesDrawable = DrawableCompat.wrap(replies);
      DrawableCompat.setTint(holder.repliesDrawable, tintColor);
      holder.replies.setCompoundDrawablesWithIntrinsicBounds(holder.repliesDrawable, null, null,
          null);
    }

    if (!showsReplies) {
      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          if (holder.isSpoiler && !holder.isRevealed) {
            revealedSpoilers.add(holder.getItemId());
            holder.isRevealed = true;

            holder.setIsRecyclable(false);
            holder.commentText.setVisibility(View.VISIBLE);
            holder.commentText.setAlpha(0.0f);
            holder.commentText.animate().alpha(1.0f);
            holder.spoilerOverlay.animate().alpha(0.0f).withEndAction(new Runnable() {
              @Override public void run() {
                holder.setIsRecyclable(true);
                holder.spoilerOverlay.setVisibility(View.GONE);
              }
            });
          } else {
            callbacks.onCommentClick(holder.getItemId(), holder.comment, holder.isSpoiler,
                holder.isUserComment);
          }
        }
      });
    }

    holder.infoPane.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final boolean liked = holder.liked;

        if (liked) {
          callbacks.onLikeComment(holder.getItemId());

          holder.likeCount--;
          DrawableCompat.setTint(holder.likeDrawable, tintColor);
        } else {
          callbacks.onUnlikeComment(holder.getItemId());

          holder.likeCount++;
          DrawableCompat.setTint(holder.likeDrawable, likedTintColor);
        }

        holder.likes.setText(String.valueOf(holder.likeCount));

        holder.liked = !liked;
      }
    });

    return holder;
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    super.onViewRecycled(holder);
    if (holder.commentAnimator != null) {
      holder.commentAnimator.cancel();
    }
    if (holder.spoilerAnimator != null) {
      holder.spoilerAnimator.cancel();
    }
  }

  @Override public void onBindViewHolder(final ViewHolder holder, int position) {
    Comment comment = getList().get(position);

    final boolean revealed = revealedSpoilers.contains(comment.getId());

    DateFormat format = DateFormat.getDateTimeInstance();
    final String dateString = format.format(new Date(comment.getCreatedAt()));

    String visibleName;
    if (!TextUtils.isEmpty(comment.getUser().getName())) {
      visibleName = comment.getUser().getName();
    } else {
      visibleName = comment.getUser().getUsername();
    }

    holder.avatar.setImage(comment.getUser().getAvatar());
    String usernameText = getContext().getResources()
        .getString(R.string.comments_username_rating, visibleName, comment.getUserRating());
    holder.username.setText(usernameText);
    holder.date.setText(dateString);
    holder.commentText.setText(comment.getComment());
    holder.likes.setText(String.valueOf(comment.getLikes()));
    if (holder.replies != null) {
      holder.replies.setText(String.valueOf(comment.getReplies()));
    }

    holder.likeCount = comment.getLikes();

    if (comment.isLiked()) {
      DrawableCompat.setTint(holder.likeDrawable, likedTintColor);
    } else {
      DrawableCompat.setTint(holder.likeDrawable, tintColor);
    }

    holder.isRevealed = revealed;

    if (comment.isSpoiler() && !revealed) {
      holder.commentText.setVisibility(View.GONE);
      holder.spoilerOverlay.setVisibility(View.VISIBLE);
      holder.spoilerOverlay.setAlpha(1.0f);
    } else {
      holder.commentText.setVisibility(View.VISIBLE);
      holder.commentText.setAlpha(1.0f);
      holder.spoilerOverlay.setVisibility(View.GONE);
    }

    if (showsReplies) {
      if (comment.isSpoiler() && !revealed) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            revealedSpoilers.add(holder.getItemId());
            holder.isRevealed = true;

            holder.commentText.setVisibility(View.VISIBLE);
            holder.commentText.setAlpha(0.0f);
            holder.commentAnimator = holder.commentText.animate().alpha(1.0f);
            holder.spoilerAnimator =
                holder.spoilerOverlay.animate().alpha(0.0f).withEndAction(new Runnable() {
                  @Override public void run() {
                    holder.spoilerOverlay.setVisibility(View.GONE);
                  }
                });
          }
        });
      } else {
        holder.itemView.setOnClickListener(null);
      }
    }

    holder.spoiler.setVisibility(comment.isSpoiler() ? View.VISIBLE : View.INVISIBLE);

    holder.comment = comment.getComment();
    holder.isSpoiler = comment.isSpoiler();
    holder.isUserComment = comment.isUserComment();
    holder.liked = comment.isLiked();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.infoPane) View infoPane;
    @BindView(R.id.avatar) RemoteImageView avatar;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.commentText) TextView commentText;
    @BindView(R.id.likes) TextView likes;
    @BindView(R.id.replies) @Nullable TextView replies;
    @BindView(R.id.spoiler) View spoiler;
    @BindView(R.id.spoilerOverlay) View spoilerOverlay;

    ViewPropertyAnimator commentAnimator;
    ViewPropertyAnimator spoilerAnimator;

    Drawable likeDrawable;
    Drawable repliesDrawable;

    int likeCount;

    String comment;
    boolean isUserComment;
    boolean isSpoiler;
    boolean isRevealed;
    boolean liked;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
