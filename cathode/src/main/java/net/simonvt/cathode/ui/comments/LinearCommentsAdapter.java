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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.widget.CircleTransformation;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Comment;

public final class LinearCommentsAdapter {

  private LinearCommentsAdapter() {
  }

  public static void updateComments(Context context, ViewGroup parent, List<Comment> userComments,
      List<Comment> comments) {
    parent.removeAllViews();

    if (userComments != null) {
      for (Comment comment : userComments) {

        String visibleName;
        if (!TextUtils.isEmpty(comment.getUser().getName())) {
          visibleName = comment.getUser().getName();
        } else {
          visibleName = comment.getUser().getUsername();
        }

        View post =
            LayoutInflater.from(context).inflate(R.layout.section_comments_item, parent, false);
        RemoteImageView avatarView = post.findViewById(R.id.avatar);
        avatarView.addTransformation(new CircleTransformation());
        TextView usernameView = post.findViewById(R.id.username);
        TextView date = post.findViewById(R.id.date);
        TextView commentText = post.findViewById(R.id.commentText);

        avatarView.setImage(comment.getUser().getAvatar());
        usernameView.setText(visibleName);
        DateFormat format = DateFormat.getDateTimeInstance();
        final String dateString = format.format(new Date(comment.getCreatedAt()));
        date.setText(dateString);
        commentText.setText(comment.getComment());

        parent.addView(post);
      }
    }

    if (comments != null) {
      for (Comment comment : comments) {
        String visibleName;
        if (!TextUtils.isEmpty(comment.getUser().getName())) {
          visibleName = comment.getUser().getName();
        } else {
          visibleName = comment.getUser().getUsername();
        }

        View post =
            LayoutInflater.from(context).inflate(R.layout.section_comments_item, parent, false);
        RemoteImageView avatarView = post.findViewById(R.id.avatar);
        avatarView.addTransformation(new CircleTransformation());
        TextView usernameView = post.findViewById(R.id.username);
        TextView date = post.findViewById(R.id.date);
        TextView commentText = post.findViewById(R.id.commentText);

        avatarView.setImage(comment.getUser().getAvatar());
        String usernameText = context.getResources()
            .getString(R.string.comments_username_rating, visibleName, comment.getUserRating());
        usernameView.setText(usernameText);
        DateFormat format = DateFormat.getDateTimeInstance();
        final String dateString = format.format(new Date(comment.getCreatedAt()));
        date.setText(dateString);
        commentText.setText(comment.getComment());

        parent.addView(post);
      }
    }

    final int count = parent.getChildCount();
    if (count == 0) {
      LayoutInflater.from(context).inflate(R.layout.section_comments_none, parent, true);
    }
  }
}
