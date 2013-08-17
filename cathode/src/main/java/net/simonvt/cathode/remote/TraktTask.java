package net.simonvt.cathode.remote;

import android.os.Handler;
import android.os.Looper;
import com.squareup.tape.Task;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.util.LogWrapper;

public abstract class TraktTask implements Task<TraktTaskService> {

  private static final String TAG = "TraktTask";

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject protected transient TraktTaskQueue queue;

  @Inject protected transient PriorityTraktTaskQueue priorityQueue;

  protected transient TraktTaskService service;

  private transient boolean canceled;

  @Override
  public final void execute(final TraktTaskService service) {
    CathodeApp.inject(service, this);
    this.service = service;

    new Thread(new Runnable() {
      @Override
      public void run() {
        doTask();
      }
    }).start();
  }

  protected abstract void doTask();

  public void cancel() {
    synchronized (this) {
      canceled = true;
    }
  }

  protected void queueTask(final TraktTask task) {
    LogWrapper.v(TAG, "Queueing task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) queue.add(task);
    }
  }

  protected void queuePriorityTask(final TraktTask task) {
    LogWrapper.v(TAG, "Queueing priority task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) priorityQueue.add(task);
    }
  }

  protected void postOnSuccess() {
    MAIN_HANDLER.post(new Runnable() {
      @Override
      public void run() {
        service.onSuccess();
      }
    });
  }

  protected void postOnFailure() {
    MAIN_HANDLER.post(new Runnable() {
      @Override
      public void run() {
        service.onFailure();
      }
    });
  }

  public interface TaskCallback {

    void onSuccess();

    void onFailure();
  }
}
