package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktProvider;
import net.simonvt.trakt.util.LogWrapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SyncShowsCollectionTask extends TraktTask {

    private static final String TAG = "SyncShowsCollectionTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            ContentResolver resolver = mService.getContentResolver();
            List<TvShow> shows = mUserService.libraryShowsCollection(DetailLevel.MIN);

            Cursor c = mService.getContentResolver().query(TraktContract.Episodes.CONTENT_URI, new String[] {
                    TraktContract.Episodes._ID,
            }, TraktContract.Episodes.IN_COLLECTION, null, null);

            final int episodeIdIndex = c.getColumnIndex(TraktContract.Episodes._ID);

            List<Long> episodeIds = new ArrayList<Long>(c.getCount());
            while (c.moveToNext()) {
                episodeIds.add(c.getLong(episodeIdIndex));
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            for (TvShow show : shows) {
                final int tvdbId = show.getTvdbId();
                final long showId = ShowWrapper.getShowId(resolver, tvdbId);

                if (showId == -1) {
                    queueTask(new SyncShowTask(tvdbId));

                } else {
                    List<Season> seasons = show.getSeasons();
                    for (Season season : seasons) {
                        final int number = season.getSeason();
                        Season.Episodes episodes = season.getEpisodes();
                        List<Integer> inCollection = episodes.getNumbers();
                        for (int episodeNumber : inCollection) {
                            final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, number, episodeNumber);
                            if (episodeId != -1) {
                                episodeIds.remove(episodeId);

                                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                                        TraktContract.Episodes.buildFromId(episodeId));
                                builder.withValue(TraktContract.Episodes.IN_COLLECTION, true);
                                ops.add(builder.build());

                            } else {
                                queueTask(new SyncEpisodeTask(tvdbId, number, episodeNumber));
                            }
                        }
                    }

                    for (Season season : show.getSeasons()) {
                        final long seasonId = SeasonWrapper.getSeasonId(resolver, tvdbId,
                                season.getSeason());
                        queueTask(new UpdateSeasonCountTask(seasonId));
                    }
                    queueTask(new UpdateShowCountTask(showId));
                }
            }

            for (long episodeId : episodeIds) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                        TraktContract.Episodes.buildFromId(episodeId));
                builder.withValue(TraktContract.Episodes.IN_COLLECTION, false);
                ops.add(builder.build());
            }

            resolver.applyBatch(TraktProvider.AUTHORITY, ops);

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
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
