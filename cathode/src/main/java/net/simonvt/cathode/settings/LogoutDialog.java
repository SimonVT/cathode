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
package net.simonvt.cathode.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.LogoutJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.sync.jobscheduler.AuthJobHandlerJob;

public class LogoutDialog extends AppCompatDialogFragment {

  @Inject JobManager jobManager;

  @Override public Dialog onCreateDialog(Bundle inState) {
    AndroidSupportInjection.inject(this);

    return new AlertDialog.Builder(getActivity()).setTitle(R.string.logout_title)
        .setMessage(R.string.logout_message)
        .setPositiveButton(R.string.logout_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            Settings.get(getActivity())
                .edit()
                .putBoolean(TraktLinkSettings.TRAKT_LINKED, false)
                .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
                .apply();

            Settings.clearSettings(getActivity());
            SuggestionsTimestamps.clearRecommended(getActivity());
            TraktTimestamps.clear(getActivity());

            jobManager.addJob(new LogoutJob());
            jobManager.removeJobsWithFlag(Flags.REQUIRES_AUTH);

            AuthJobHandlerJob.cancel(getActivity());
            SyncUserActivity.cancel(getActivity());
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .create();
  }
}
