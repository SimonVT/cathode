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

  public static final int LOADER_SHOWS_UPCOMING = 100;
  public static final int LOADER_SHOWS_WATCHED = 101;
  public static final int LOADER_SHOWS_COLLECTION = 102;
  public static final int LOADER_SHOWS_WATCHLIST = 103;
  public static final int LOADER_EPISODES_WATCHLIST = 104;
  public static final int LOADER_SHOWS_TRENDING = 105;
  public static final int LOADER_MOVIES_WATCHED = 200;
  public static final int LOADER_MOVIES_COLLECTION = 201;
  public static final int LOADER_MOVIES_WATCHLIST = 202;
  public static final int LOADER_MOVIES_TRENDING = 203;

  public static final int LOADER_SHOW = 300;
  public static final int LOADER_SHOW_WATCH = 301;
  public static final int LOADER_SHOW_COLLECT = 302;
  public static final int LOADER_SHOW_GENRES = 303;
  public static final int LOADER_SHOW_SEASONS = 304;

  public static final int LOADER_MOVIE = 400;
  public static final int LOADER_MOVIE_ACTORS = 401;

  public static final int LOADER_SEASON = 500;
  public static final int LOADER_SEARCH_SHOWS = 600;
  public static final int LOADER_SEARCH_MOVIES = 700;
  public static final int LOADER_EPISODE = 800;

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
