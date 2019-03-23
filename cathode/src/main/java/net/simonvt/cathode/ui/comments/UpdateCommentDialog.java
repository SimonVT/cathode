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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.snackbar.Snackbar;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.common.util.TextUtils;
import net.simonvt.cathode.sync.scheduler.CommentsTaskScheduler;

public class UpdateCommentDialog extends DialogFragment {

  private static final String ARG_COMMENT_ID =
      "net.simonvt.cathode.ui.dialog.RatingDialog.commentId";
  private static final String ARG_COMMENT =
      "net.simonvt.cathode.ui.comments.UpdateCommentDialog.comment";
  private static final String ARG_SPOILER =
      "net.simonvt.cathode.ui.comments.UpdateCommentDialog.spoiler";

  @Inject CommentsTaskScheduler commentsScheduler;

  private long commentId;

  private String comment;

  private boolean spoiler;

  public static UpdateCommentDialog newInstance(long commentId, String comment, boolean spoiler) {
    UpdateCommentDialog dialog = new UpdateCommentDialog();

    Bundle args = new Bundle();
    args.putLong(ARG_COMMENT_ID, commentId);
    args.putString(ARG_COMMENT, comment);
    args.putBoolean(ARG_SPOILER, spoiler);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    commentId = args.getLong(ARG_COMMENT_ID);
    comment = args.getString(ARG_COMMENT);
    spoiler = args.getBoolean(ARG_SPOILER);
  }

  @NonNull @SuppressWarnings("InflateParams") @Override
  public Dialog onCreateDialog(@Nullable Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

    View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_comment, null);
    final EditText commentView = v.findViewById(R.id.comment);
    final CheckBox spoilerView = v.findViewById(R.id.spoiler);
    commentView.setText(comment);
    spoilerView.setChecked(spoiler);

    builder.setView(v);
    builder.setTitle(R.string.title_comment_update);
    builder.setPositiveButton(R.string.comment_update, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        final String comment = commentView.getText().toString();
        final boolean spoiler = spoilerView.isChecked();

        if (TextUtils.wordCount(comment) >= CommentsService.MIN_WORD_COUNT) {
          commentsScheduler.updateComment(commentId, comment, spoiler);
        } else {
          Snackbar.make(requireActivity().findViewById(android.R.id.content),
              R.string.comment_too_short, Snackbar.LENGTH_LONG).show();
        }
      }
    });
    builder.setNegativeButton(R.string.comment_delete, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int which) {
        commentsScheduler.deleteComment(commentId);
      }
    });

    return builder.create();
  }
}
