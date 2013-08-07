package net.simonvt.trakt.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class SettingsUtils {

  private static final String TAG = "SettingsUtils";

  private SettingsUtils() {
  }

  public static boolean isLoggedIn(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getString(Settings.USERNAME, null) != null
        && settings.getString(Settings.PASSWORD, null) != null;
  }

  public static String getUsername(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getString(Settings.USERNAME, null);
  }

  public static String getPassword(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getString(Settings.PASSWORD, null);
  }
}
