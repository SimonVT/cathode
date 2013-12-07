package net.simonvt.cathode.ui.dialog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.simonvt.cathode.R;

public class AboutDialog extends DialogFragment {

  private static final String DIALOG_LICENSES =
      "net.simonvt.cathode.ui.dialog.AboutDialog.licenses";

  @InjectView(R.id.version) TextView version;
  @InjectView(R.id.licenses) View licenses;
  @InjectView(R.id.sourceCode) View sourceCode;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.dialog_about, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    ButterKnife.inject(this, view);
    getDialog().setTitle(R.string.app_name);

    String versionName;
    try {
      versionName = getActivity().getPackageManager()
          .getPackageInfo(getActivity().getPackageName(), 0).versionName;
      version.setText(getString(R.string.version_x, versionName));
    } catch (PackageManager.NameNotFoundException e) {
      version.setText(R.string.version_unknown);
    }

    licenses.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        new LicensesDialog().show(getFragmentManager(), DIALOG_LICENSES);
      }
    });
    sourceCode.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Uri uri = Uri.parse(getString(R.string.sourceUrl));
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
      }
    });
  }

  @Override public void onDestroyView() {
    ButterKnife.reset(this);
    super.onDestroyView();
  }
}
