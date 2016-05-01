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

package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.util.Cursors;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.RemoteImageView;

public class CommentsAdapter extends RecyclerCursorAdapter<CommentsAdapter.ViewHolder> {

  private static final String STATE_REVEALED_SPOILERS =
      "net.simonvt.cathode.ui.adapter.CommentsAdapter";

  private static final int COMMENT = 0;
  private static final int REPLY = 1;
  private static final int USER_COMMENT = 2;
  private static final int USER_REPLY = 3;

  public interface OnCommentClickListener {

    void onCommentClick(long commentId, String comment, boolean spoiler, boolean isUserComment);
  }

  public static final String[] PROJECTION = {
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.ID),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.COMMENT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.SPOILER),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REVIEW),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.CREATED_AT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REPLIES),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKES),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.USER_RATING),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKED),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.IS_USER_COMMENT),
      SqlColumn.table(Tables.COMMENTS).column(LastModifiedColumns.LAST_MODIFIED),
      SqlColumn.table(Tables.USERS).column(UserColumns.USERNAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.NAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR),
  };

  @Inject CommentsTaskScheduler commentsScheduler;

  private boolean showsReplies;

  private OnCommentClickListener listener;

  private Set<Long> revealedSpoilers = new HashSet<>();

  private int tintColor;

  private int likedTintColor;

  public CommentsAdapter(Context context, Cursor cursor, boolean showsReplies,
      OnCommentClickListener listener) {
    super(context, cursor);
    this.showsReplies = showsReplies;
    this.listener = listener;

    CathodeApp.inject(context, this);

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

  @Override public int getItemViewType(int position) {
    if (showsReplies && position > 0) {
      return REPLY;
    }

    return COMMENT;
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
            listener.onCommentClick(holder.getItemId(), holder.comment, holder.isSpoiler,
                holder.isUserComment);
          }
        }
      });
    }

    holder.infoPane.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final boolean liked = holder.liked;

        if (liked) {
          commentsScheduler.unlike(holder.getItemId());

          holder.likeCount--;
          DrawableCompat.setTint(holder.likeDrawable, tintColor);
        } else {
          commentsScheduler.like(holder.getItemId());

          holder.likeCount++;
          DrawableCompat.setTint(holder.likeDrawable, likedTintColor);
        }

        holder.likes.setText(String.valueOf(holder.likeCount));

        holder.liked = !liked;
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(final ViewHolder holder, Cursor cursor, int position) {
    final long commentId = Cursors.getLong(cursor, CommentColumns.ID);
    final String comment = Cursors.getString(cursor, CommentColumns.COMMENT);
    final boolean spoiler = Cursors.getBoolean(cursor, CommentColumns.SPOILER);
    final boolean review = Cursors.getBoolean(cursor, CommentColumns.REVIEW);
    final long createdAt = Cursors.getLong(cursor, CommentColumns.CREATED_AT);
    final int likes = Cursors.getInt(cursor, CommentColumns.LIKES);
    final int replies = Cursors.getInt(cursor, CommentColumns.REPLIES);
    final int userRating = Cursors.getInt(cursor, CommentColumns.USER_RATING);
    final boolean liked = Cursors.getBoolean(cursor, CommentColumns.LIKED);
    final boolean isUserComment = Cursors.getBoolean(cursor, CommentColumns.IS_USER_COMMENT);

    final String username = Cursors.getString(cursor, UserColumns.USERNAME);
    final String name = Cursors.getString(cursor, UserColumns.NAME);
    final String avatar = Cursors.getString(cursor, UserColumns.AVATAR);

    final boolean revealed = revealedSpoilers.contains(commentId);

    DateFormat format = DateFormat.getDateTimeInstance();
    final String dateString = format.format(new Date(createdAt));

    String visibleName;
    if (!TextUtils.isEmpty(name)) {
      visibleName = name;
    } else {
      visibleName = username;
    }

    holder.avatar.setImage(avatar);
    String usernameText = getContext().getResources()
        .getString(R.string.comments_username_rating, visibleName, userRating);
    holder.username.setText(usernameText);
    holder.date.setText(dateString);
    holder.commentText.setText(comment);
    holder.likes.setText(String.valueOf(likes));
    if (holder.replies != null) {
      holder.replies.setText(String.valueOf(replies));
    }

    holder.likeCount = likes;

    if (liked) {
      DrawableCompat.setTint(holder.likeDrawable, likedTintColor);
    } else {
      DrawableCompat.setTint(holder.likeDrawable, tintColor);
    }

    holder.isRevealed = revealed;

    if (spoiler && !revealed) {
      holder.commentText.setVisibility(View.GONE);
      holder.spoilerOverlay.setVisibility(View.VISIBLE);
      holder.spoilerOverlay.setAlpha(1.0f);
    } else {
      holder.commentText.setVisibility(View.VISIBLE);
      holder.commentText.setAlpha(1.0f);
      holder.spoilerOverlay.setVisibility(View.GONE);
    }

    if (showsReplies) {
      if (spoiler && !revealed) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
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
          }
        });
      } else {
        holder.itemView.setOnClickListener(null);
      }
    }

    holder.spoiler.setVisibility(spoiler ? View.VISIBLE : View.INVISIBLE);

    holder.comment = comment;
    holder.isSpoiler = spoiler;
    holder.isUserComment = isUserComment;
    holder.liked = liked;
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
