package net.simonvt.cathode.remote;

import android.content.Context;
import android.content.Intent;
import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import com.squareup.tape.TaskQueue;
import java.io.File;
import java.io.IOException;

public final class TraktTaskQueue extends TaskQueue<TraktTask> {

  private static final String TAG = "TraktTaskQueue";

  private final Context context;

  private TraktTaskQueue(ObjectQueue<TraktTask> delegate, Context context) {
    super(delegate);
    this.context = context;

    if (size() > 0) {
      startService();
    }
  }

  private void startService() {
    context.startService(new Intent(context, TraktTaskService.class));
  }

  @Override
  public void add(TraktTask entry) {
    synchronized (this) {
      super.add(entry);
      startService();
    }
  }

  @Override
  public TraktTask peek() {
    synchronized (this) {
      return super.peek();
    }
  }

  @Override
  public int size() {
    synchronized (this) {
      return super.size();
    }
  }

  @Override
  public void remove() {
    synchronized (this) {
      super.remove();
    }
  }

  public static TraktTaskQueue create(Context context, Gson gson) {
    FileObjectQueue.Converter<TraktTask> converter =
        new GsonConverter<TraktTask>(gson, TraktTask.class);
    File queueFile = new File(context.getFilesDir(), TAG);
    FileObjectQueue<TraktTask> delegate;
    try {
      delegate = new FileObjectQueue<TraktTask>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new TraktTaskQueue(delegate, context);
  }
}
