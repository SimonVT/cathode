package net.simonvt.trakt.sync.task;

import com.squareup.tape.Task;

import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.sync.PriorityTraktTaskQueue;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.TraktTaskService;
import net.simonvt.trakt.util.LogWrapper;

import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;

public abstract class TraktTask implements Task<TraktTaskService> {

    private static final String TAG = "TraktTask";

    protected static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Inject transient TraktTaskQueue mQueue;

    @Inject transient PriorityTraktTaskQueue mPriorityQueue;

    transient TraktTaskService mService;

    @Override
    public final void execute(final TraktTaskService service) {
        TraktApp.inject(service, this);
        mService = service;

        new Thread(new Runnable() {
            @Override
            public void run() {
                doTask();
            }
        }).start();
    }

    protected abstract void doTask();

    protected void queueTask(final TraktTask task) {
        LogWrapper.v(TAG, "Queueing task: " + task.getClass().getSimpleName());
        mQueue.add(task);
    }

    protected void queuePriorityTask(final TraktTask task) {
        LogWrapper.v(TAG, "Queueing priority task: " + task.getClass().getSimpleName());
        mPriorityQueue.add(task);
    }

    protected void postOnSuccess() {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mService.onSuccess();
            }
        });
    }

    protected void postOnFailure() {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mService.onFailure();
            }
        });
    }

    public interface TaskCallback {

        void onSuccess();

        void onFailure();
    }
}
