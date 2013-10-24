package net.simonvt.cathode.util;

import android.content.Context;
import com.crashlytics.android.Crashlytics;

public class ErrorReporting {

  private ErrorReporting() {
  }

  public static void init(Context context) {
    Crashlytics.start(context);
  }
}
