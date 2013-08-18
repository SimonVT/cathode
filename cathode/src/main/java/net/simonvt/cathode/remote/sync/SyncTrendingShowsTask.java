package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncTrendingShowsTask extends TraktTask {

  @Inject ShowsService showsService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = service.getContentResolver();

      List<TvShow> shows = showsService.trending();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(CathodeContract.Shows.SHOWS_TRENDING, null, null, null, null);
      while (c.moveToNext()) {
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Shows._ID));
        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Shows.TRENDING_INDEX, -1);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Shows.buildShowUri(showId))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      for (int i = 0, count = Math.min(shows.size(), 50); i < count; i++) {
        TvShow show = shows.get(i);
        long showId = ShowWrapper.getShowId(resolver, show);
        if (showId == -1L) {
          queueTask(new SyncShowTask(show.getTvdbId()));
          showId = ShowWrapper.insertShow(resolver, show);
        }

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Shows.TRENDING_INDEX, i);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Shows.buildShowUri(showId))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RetrofitError error) {
      error.printStackTrace();
      postOnFailure();
    } catch (RemoteException e) {
      e.printStackTrace();
      postOnFailure();
    } catch (OperationApplicationException e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
