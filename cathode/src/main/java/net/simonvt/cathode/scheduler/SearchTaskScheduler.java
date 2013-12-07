package net.simonvt.cathode.scheduler;

import android.content.Context;
import net.simonvt.cathode.provider.SearchWrapper;

public class SearchTaskScheduler extends BaseTaskScheduler {

  public SearchTaskScheduler(Context context) {
    super(context);
  }

  public void insertShowQuery(final String query) {
    execute(new Runnable() {
      @Override public void run() {
        SearchWrapper.insertShowQuery(context.getContentResolver(), query);
      }
    });
  }

  public void insertMovieQuery(final String query) {
    execute(new Runnable() {
      @Override public void run() {
        SearchWrapper.insertMovieQuery(context.getContentResolver(), query);
      }
    });
  }
}
