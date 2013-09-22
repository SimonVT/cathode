package net.simonvt.cathode.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.UserCredentials;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.remote.TraktTaskService;
import net.simonvt.cathode.settings.ActivityWrapper;
import net.simonvt.cathode.settings.Settings;

public class LogoutDialog extends DialogFragment {

  @Inject UserCredentials credentials;
  @Inject Bus bus;

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle(R.string.logout_title)
        .setMessage(R.string.logout_message)
        .setPositiveButton(R.string.logout_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            final Context context = getActivity().getApplicationContext();
            CathodeApp.inject(context, LogoutDialog.this);

            credentials.setCredentials(null, null);
            ActivityWrapper.clear(context);

            Intent intent = new Intent(context, TraktTaskService.class);
            intent.setAction(TraktTaskService.ACTION_LOGOUT);
            context.startService(intent);

            bus.post(new LogoutEvent());
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .create();
  }
}
