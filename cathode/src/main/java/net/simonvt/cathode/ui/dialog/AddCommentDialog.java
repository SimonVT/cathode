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
package net.simonvt.cathode.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.scheduler.CommentsTaskScheduler;
import net.simonvt.cathode.util.TextUtils;
import timber.log.Timber;

public class AddCommentDialog extends DialogFragment {

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.dialog.AddCommentDialog.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.dialog.AddCommentDialog.id";

  @Inject Bus bus;

  @Inject CommentsTaskScheduler commentsScheduler;

  private ItemType type;

  private long id;

  public static AddCommentDialog newInstance(ItemType type, long id) {
    AddCommentDialog dialog = new AddCommentDialog();

    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putLong(ARG_ID, id);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    type = (ItemType) args.getSerializable(ARG_TYPE);
    id = args.getLong(ARG_ID);
  }

  @Override public Dialog onCreateDialog(Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_comment, null);
    final EditText commentView = (EditText) v.findViewById(R.id.comment);
    final CheckBox spoilerView = (CheckBox) v.findViewById(R.id.spoiler);

    builder.setView(v);
    if (type == ItemType.COMMENT) {
      builder.setTitle(R.string.title_comment_reply);
    } else {
      builder.setTitle(R.string.title_comment_add);
    }
    builder.setPositiveButton(R.string.action_submit, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        final String comment = commentView.getText().toString();
        final boolean spoiler = spoilerView.isChecked();

        if (TextUtils.wordCount(comment) >= CommentsService.MIN_WORD_COUNT) {
          if (type == ItemType.COMMENT) {
            commentsScheduler.reply(id, comment, spoiler);
          } else {
            commentsScheduler.comment(type, id, comment, spoiler);
          }
        } else {

          Timber.d("Word count: " + TextUtils.wordCount(comment));
          bus.post(new MessageEvent(R.string.comment_too_short));
        }
      }
    });

    return builder.create();
  }
}
