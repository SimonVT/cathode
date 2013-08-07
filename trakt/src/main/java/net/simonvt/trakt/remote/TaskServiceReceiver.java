package net.simonvt.trakt.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import net.simonvt.trakt.util.LogWrapper;

public class TaskServiceReceiver extends BroadcastReceiver {

  private static final String TAG = "TaskServiceReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    LogWrapper.v(TAG, "[onReceive]");
    TraktTaskService.acquireLock(context);
    context.startService(new Intent(context, TraktTaskService.class));
  }
}
