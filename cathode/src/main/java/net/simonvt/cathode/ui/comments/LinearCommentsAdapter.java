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
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public final class LinearCommentsAdapter {

  private LinearCommentsAdapter() {
  }

  public static void updateComments(Context context, ViewGroup parent, Cursor userComments,
      Cursor comments) {
    parent.removeAllViews();

    if (userComments != null) {
      userComments.moveToPosition(-1);
      while (userComments.moveToNext()) {
        final long commentId = Cursors.getLong(userComments, DatabaseContract.CommentColumns.ID);
        final String comment =
            Cursors.getString(userComments, DatabaseContract.CommentColumns.COMMENT);
        final boolean spoiler =
            Cursors.getBoolean(userComments, DatabaseContract.CommentColumns.SPOILER);
        final boolean review =
            Cursors.getBoolean(userComments, DatabaseContract.CommentColumns.REVIEW);
        final long createdAt =
            Cursors.getLong(userComments, DatabaseContract.CommentColumns.CREATED_AT);
        final long likes = Cursors.getLong(userComments, DatabaseContract.CommentColumns.LIKES);
        final int userRating =
            Cursors.getInt(userComments, DatabaseContract.CommentColumns.USER_RATING);

        final String username =
            Cursors.getString(userComments, DatabaseContract.UserColumns.USERNAME);
        final String name = Cursors.getString(userComments, DatabaseContract.UserColumns.NAME);
        final String avatar = Cursors.getString(userComments, DatabaseContract.UserColumns.AVATAR);

        String visibleName;
        if (!TextUtils.isEmpty(name)) {
          visibleName = name;
        } else {
          visibleName = username;
        }

        View post =
            LayoutInflater.from(context).inflate(R.layout.comment_post_short, parent, false);
        RemoteImageView avatarView = (RemoteImageView) post.findViewById(R.id.avatar);
        avatarView.addTransformation(new CircleTransformation());
        TextView usernameView = (TextView) post.findViewById(R.id.username);
        TextView date = (TextView) post.findViewById(R.id.date);
        TextView commentText = (TextView) post.findViewById(R.id.commentText);

        avatarView.setImage(avatar);
        usernameView.setText(visibleName);
        DateFormat format = DateFormat.getDateTimeInstance();
        final String dateString = format.format(new Date(createdAt));
        date.setText(dateString);
        commentText.setText(comment);

        parent.addView(post);
      }
    }

    if (comments != null) {
      comments.moveToPosition(-1);
      while (comments.moveToNext()) {
        final long commentId = Cursors.getLong(comments, DatabaseContract.CommentColumns.ID);
        final String comment = Cursors.getString(comments, DatabaseContract.CommentColumns.COMMENT);
        final boolean spoiler =
            Cursors.getBoolean(comments, DatabaseContract.CommentColumns.SPOILER);
        final boolean review = Cursors.getBoolean(comments, DatabaseContract.CommentColumns.REVIEW);
        final long createdAt =
            Cursors.getLong(comments, DatabaseContract.CommentColumns.CREATED_AT);
        final long likes = Cursors.getLong(comments, DatabaseContract.CommentColumns.LIKES);
        final int userRating =
            Cursors.getInt(comments, DatabaseContract.CommentColumns.USER_RATING);

        final String username = Cursors.getString(comments, DatabaseContract.UserColumns.USERNAME);
        final String name = Cursors.getString(comments, DatabaseContract.UserColumns.NAME);
        final String avatar = Cursors.getString(comments, DatabaseContract.UserColumns.AVATAR);

        String visibleName;
        if (!TextUtils.isEmpty(name)) {
          visibleName = name;
        } else {
          visibleName = username;
        }

        View post =
            LayoutInflater.from(context).inflate(R.layout.comment_post_short, parent, false);
        RemoteImageView avatarView = (RemoteImageView) post.findViewById(R.id.avatar);
        avatarView.addTransformation(new CircleTransformation());
        TextView usernameView = (TextView) post.findViewById(R.id.username);
        TextView date = (TextView) post.findViewById(R.id.date);
        TextView commentText = (TextView) post.findViewById(R.id.commentText);

        avatarView.setImage(avatar);
        String usernameText = context.getResources()
            .getString(R.string.comments_username_rating, visibleName, userRating);
        usernameView.setText(usernameText);
        DateFormat format = DateFormat.getDateTimeInstance();
        final String dateString = format.format(new Date(createdAt));
        date.setText(dateString);
        commentText.setText(comment);

        parent.addView(post);
      }
    }

    final int count = parent.getChildCount();
    if (count == 0) {
      LayoutInflater.from(context).inflate(R.layout.comments_none, parent, true);
    }
  }
}
