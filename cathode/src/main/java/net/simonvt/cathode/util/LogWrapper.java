package net.simonvt.cathode.util;

import android.util.Log;
import net.simonvt.cathode.CathodeApp;

public final class LogWrapper {

  private static final String TAG = "Cathode";
  private static final boolean DEBUG = CathodeApp.DEBUG;

  private LogWrapper() {
  }

  public static void d(String tag, String message) {
    if (DEBUG) Log.d(TAG, tag + ": " + message);
  }

  public static void d(String tag, String message, Exception e) {
    if (DEBUG) Log.d(TAG, tag + ": " + message, e);
  }

  public static void i(String tag, String message) {
    if (DEBUG) Log.i(TAG, tag + ": " + message);
  }

  public static void v(String tag, String message) {
    if (DEBUG) Log.v(TAG, tag + ": " + message);
  }

  public static void e(String tag, String message, Throwable t) {
    if (DEBUG) Log.e(TAG, tag + ": " + message, t);
  }
}
