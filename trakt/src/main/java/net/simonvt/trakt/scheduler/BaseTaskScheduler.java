package net.simonvt.trakt.scheduler;

import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.remote.PriorityTraktTaskQueue;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.TraktTask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class BaseTaskScheduler {

    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    };

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Inject TraktTaskQueue mQueue;

    @Inject PriorityTraktTaskQueue mPriorityQueue;

    protected Context mContext;

    public BaseTaskScheduler(Context context) {
        TraktApp.inject(context, this);
        mContext = context;
    }

    protected final void postTask(final TraktTask task) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mQueue.add(task);
            }
        });
    }

    protected final void postPriorityTask(final TraktTask task) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                mPriorityQueue.add(task);
            }
        });
    }

    protected void execute(Runnable r) {
        SERIAL_EXECUTOR.execute(r);
    }

    private static final SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

    private static class SerialExecutor implements Executor {

        final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
        Runnable active;

        public synchronized void execute(final Runnable r) {
            tasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                EXECUTOR.execute(active);
            }
        }
    }
}
