package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.event.AuthFailedEvent;

public abstract class BaseActivity extends FragmentActivity {

  @Inject Bus bus;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(this);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (BuildConfig.DEBUG) {
      menu.add(0, 1, 0, "AuthFailedEvent");
      return true;
    }

    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (BuildConfig.DEBUG) {
      switch (item.getItemId()) {
        case 1:
          bus.post(new AuthFailedEvent());
          return true;
      }
    }

    return super.onOptionsItemSelected(item);
  }
}
