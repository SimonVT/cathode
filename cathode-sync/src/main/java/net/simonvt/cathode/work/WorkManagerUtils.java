package net.simonvt.cathode.work;

import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;

// Wrapper around workmanager.kt with default values
public final class WorkManagerUtils {

  private WorkManagerUtils() {
  }

  public static void enqueueNow(WorkManager workManager,
      Class<? extends ListenableWorker> workerClass) {
    WorkmanagerKt.enqueueNow(workManager, workerClass, null, true);
  }

  public static void enqueueUniqueNow(WorkManager workManager, String tag,
      Class<? extends ListenableWorker> workerClass) {
    WorkmanagerKt.enqueueUniqueNow(workManager, tag, workerClass, ExistingWorkPolicy.APPEND, null,
        true);
  }

  public static void enqueueDelayed(WorkManager workManager,
      Class<? extends ListenableWorker> workerClass, long delayMs) {
    WorkmanagerKt.enqueueDelayed(workManager, workerClass, delayMs, null, true);
  }
}
