package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.ui.dialog.AboutDialog;

public abstract class BaseActivity extends FragmentActivity {

  private static final String DIALOG_ABOUT = "net.simonvt.cathode.ui.BaseActivity.aboutDialog";

  @Inject Bus bus;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(this);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_base, menu);

    if (BuildConfig.DEBUG) {
      menu.add(0, 1, 0, "AuthFailedEvent");
    }

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_about:
        new AboutDialog().show(getSupportFragmentManager(), DIALOG_ABOUT);
        return true;

      case 1:
        bus.post(new AuthFailedEvent());
        return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
