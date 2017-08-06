/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.settings.Settings;
import timber.log.Timber;

public class CheckInDialog extends DialogFragment {

  public enum Type {
    SHOW, MOVIE
  }

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.dialog.CheckInDialog.type";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.dialog.CheckInDialog.title";
  private static final String ARG_ID = "net.simonvt.cathode.ui.dialog.CheckInDialog.id";

  private static final String DIALOG_TAG = "net.simonvt.cathode.ui.dialog.CheckInDialog.dialog";

  @Inject EpisodeTaskScheduler episodeScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  public static class Injections {
    @Inject EpisodeTaskScheduler episodeScheduler;
    @Inject MovieTaskScheduler movieScheduler;

    public Injections(Context context) {
      Injector.obtain().inject(this);
    }
  }

  public static boolean showDialogIfNecessary(FragmentActivity activity, Type type, String title,
      long id) {
    if (title == null) {
      // TODO: Remove eventually
      Timber.e(new Exception("Title is null"), "Type: %s", type.toString());
      return true;
    }

    final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
    final boolean facebookShare = settings.getBoolean(Settings.Profile.CONNECTION_FACEBOOK, false);
    final boolean twitterShare = settings.getBoolean(Settings.Profile.CONNECTION_TWITTER, false);
    final boolean tumblrShare = settings.getBoolean(Settings.Profile.CONNECTION_TUMBLR, false);

    if (facebookShare || twitterShare || tumblrShare) {
      newInstance(type, title, id).show(activity.getSupportFragmentManager(), DIALOG_TAG);
      return true;
    } else {
      Injections injections = new Injections(activity);

      if (type == Type.SHOW) {
        injections.episodeScheduler.checkin(id, null, false, false, false);
      } else {
        injections.movieScheduler.checkin(id, null, false, false, false);
      }

      return false;
    }
  }

  private static CheckInDialog newInstance(Type type, String title, long id) {
    CheckInDialog dialog = new CheckInDialog();

    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, type);
    args.putString(ARG_TITLE, title);
    args.putLong(ARG_ID, id);
    dialog.setArguments(args);

    return dialog;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.obtain().inject(this);
  }

  @NonNull @SuppressWarnings("InflateParams") @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    AlertDialog.Builder builder =
        new AlertDialog.Builder(getActivity()).setTitle(R.string.action_checkin);

    Bundle args = getArguments();
    final Type type = (Type) args.getSerializable(ARG_TYPE);
    final String titleArg = args.getString(ARG_TITLE);
    final long id = args.getLong(ARG_ID);

    View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_check_in, null);
    TextView title = (TextView) view.findViewById(R.id.title);
    final EditText message = (EditText) view.findViewById(R.id.message);
    final CheckBox facebook = (CheckBox) view.findViewById(R.id.facebook);
    final CheckBox twitter = (CheckBox) view.findViewById(R.id.twitter);
    final CheckBox tumblr = (CheckBox) view.findViewById(R.id.tumblr);

    title.setText(titleArg);
    final boolean facebookShare = settings.getBoolean(Settings.Profile.CONNECTION_FACEBOOK, false);
    final boolean twitterShare = settings.getBoolean(Settings.Profile.CONNECTION_TWITTER, false);
    final boolean tumblrShare = settings.getBoolean(Settings.Profile.CONNECTION_TUMBLR, false);
    facebook.setVisibility(facebookShare ? View.VISIBLE : View.GONE);
    twitter.setVisibility(twitterShare ? View.VISIBLE : View.GONE);
    tumblr.setVisibility(tumblrShare ? View.VISIBLE : View.GONE);

    if (facebookShare || twitterShare || tumblrShare) {
      view.findViewById(R.id.message_title).setVisibility(View.VISIBLE);
      message.setVisibility(View.VISIBLE);
      view.findViewById(R.id.share_title).setVisibility(View.VISIBLE);
    } else {
      view.findViewById(R.id.message_title).setVisibility(View.GONE);
      message.setVisibility(View.GONE);
      view.findViewById(R.id.share_title).setVisibility(View.GONE);
    }

    String shareMessage = settings.getString(Settings.Profile.SHARING_TEXT_WATCHING,
        getString(R.string.checkin_message_default));
    shareMessage = shareMessage.replace("[item]", titleArg);
    message.setText(shareMessage);

    builder.setView(view).setPositiveButton(R.string.action_checkin, new OnClickListener() {
      @Override public void onClick(DialogInterface dialog, int which) {
        final boolean facebookShare = facebook.isChecked();
        final boolean twitterShare = twitter.isChecked();
        final boolean tumblrShare = tumblr.isChecked();
        final String shareMessage = message.getText().toString();

        settings.edit()
            .putBoolean(Settings.Profile.CONNECTION_FACEBOOK, facebookShare)
            .putBoolean(Settings.Profile.CONNECTION_TWITTER, twitterShare)
            .putBoolean(Settings.Profile.CONNECTION_TUMBLR, tumblrShare)
            .putString(Settings.Profile.SHARING_TEXT_WATCHING, shareMessage)
            .apply();

        if (type == Type.SHOW) {
          episodeScheduler.checkin(id, shareMessage, facebookShare, twitterShare, tumblrShare);
        } else {
          movieScheduler.checkin(id, shareMessage, facebookShare, twitterShare, tumblrShare);
        }
      }
    });

    return builder.create();
  }
}
