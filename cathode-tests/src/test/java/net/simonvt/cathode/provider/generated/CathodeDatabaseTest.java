package net.simonvt.cathode.provider.generated;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class CathodeDatabaseTest {

  @Test public void testSeasonDeleteTrigger() throws Exception {
    CathodeDatabase helper = CathodeDatabase.getInstance(Robolectric.application);

    SQLiteDatabase db = helper.getWritableDatabase();

    ContentValues seasonCV = new ContentValues();
    seasonCV.put(SeasonColumns.SEASON, 1);

    final long seasonId = db.insert(Tables.SEASONS, null, seasonCV);

    Cursor seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(1);

    ContentValues episodeCV = new ContentValues();
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId);

    db.insert(Tables.EPISODES, null, episodeCV);

    Cursor episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(1);

    db.delete(Tables.SEASONS, SeasonColumns.ID + "=" + seasonId, null);

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(0);

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(0);
  }

  @Test public void testShowDeleteTrigger() throws Exception {
    CathodeDatabase helper = CathodeDatabase.getInstance(Robolectric.application);

    SQLiteDatabase db = helper.getWritableDatabase();

    ContentValues showCV = new ContentValues();
    showCV.put(ShowColumns.TRAKT_ID, 1);

    final long showId = db.insert(Tables.SHOWS, null, showCV);

    ContentValues seasonCV = new ContentValues();
    seasonCV.put(SeasonColumns.SEASON, 1);
    seasonCV.put(SeasonColumns.SHOW_ID, showId);

    final long seasonId = db.insert(Tables.SEASONS, null, seasonCV);

    Cursor seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(1);

    ContentValues episodeCV = new ContentValues();
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId);
    episodeCV.put(EpisodeColumns.SHOW_ID, showId);

    db.insert(Tables.EPISODES, null, episodeCV);

    Cursor episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(1);

    db.delete(Tables.SHOWS, ShowColumns.ID + "=" + showId, null);

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(0);

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(0);
  }
}
