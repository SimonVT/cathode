/*
 * Copyright (C) 2016 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.remote.sync.people;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import androidx.collection.LongSparseArray;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Credit;
import net.simonvt.cathode.api.entity.Credits;
import net.simonvt.cathode.api.entity.Crew;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.PeopleService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncPersonShowCredits extends CallJob<Credits> {

  @Inject transient PeopleService peopleService;
  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient PersonDatabaseHelper personHelper;

  private long traktId;

  public SyncPersonShowCredits(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncPersonShowCredits" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<Credits> getCall() {
    return peopleService.shows(traktId, Extended.FULL);
  }

  @Override public boolean handleResponse(Credits credits) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    final long personId = personHelper.getId(traktId);

    Cursor oldCastCursor = getContentResolver().query(ShowCast.withPerson(personId), new String[] {
        Tables.SHOW_CAST + "." + ShowCastColumns.ID, ShowCastColumns.SHOW_ID,
    }, null, null, null);
    List<Long> oldCast = new ArrayList<>();
    LongSparseArray<Long> showToCastIdMap = new LongSparseArray<>();
    while (oldCastCursor.moveToNext()) {
      final long id = Cursors.getLong(oldCastCursor, ShowCastColumns.ID);
      final long showId = Cursors.getLong(oldCastCursor, ShowCastColumns.SHOW_ID);
      oldCast.add(showId);
      showToCastIdMap.put(showId, id);
    }
    oldCastCursor.close();

    List<Credit> cast = credits.getCast();
    if (cast != null) {
      for (Credit credit : cast) {
        Show show = credit.getShow();
        final long showId = showHelper.partialUpdate(show);

        if (oldCast.remove(showId)) {
          final long castId = showToCastIdMap.get(showId);
          ContentProviderOperation op = ContentProviderOperation.newUpdate(ShowCast.withId(castId))
              .withValue(ShowCastColumns.SHOW_ID, showId)
              .withValue(ShowCastColumns.PERSON_ID, personId)
              .withValue(ShowCastColumns.CHARACTER, credit.getCharacter())
              .build();
          ops.add(op);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(ShowCast.SHOW_CAST)
              .withValue(ShowCastColumns.SHOW_ID, showId)
              .withValue(ShowCastColumns.PERSON_ID, personId)
              .withValue(ShowCastColumns.CHARACTER, credit.getCharacter())
              .build();
          ops.add(op);
        }
      }
    }

    for (long showId : oldCast) {
      final long castId = showToCastIdMap.get(showId);
      ContentProviderOperation op =
          ContentProviderOperation.newDelete(ShowCast.withId(castId)).build();
      ops.add(op);
    }

    Crew crew = credits.getCrew();
    if (crew != null) {
      insertCrew(ops, personId, Department.PRODUCTION, crew.getProduction());
      insertCrew(ops, personId, Department.ART, crew.getArt());
      insertCrew(ops, personId, Department.CREW, crew.getCrew());
      insertCrew(ops, personId, Department.COSTUME_AND_MAKEUP, crew.getCostume_and_make_up());
      insertCrew(ops, personId, Department.DIRECTING, crew.getDirecting());
      insertCrew(ops, personId, Department.WRITING, crew.getWriting());
      insertCrew(ops, personId, Department.SOUND, crew.getSound());
      insertCrew(ops, personId, Department.CAMERA, crew.getCamera());
    }

    return applyBatch(ops);
  }

  private void insertCrew(ArrayList<ContentProviderOperation> ops, long personId,
      Department department, List<Credit> crew) {
    Cursor oldCrewCursor = getContentResolver().query(ShowCrew.withPerson(personId), new String[] {
        Tables.SHOW_CREW + "." + ShowCrewColumns.ID, ShowCrewColumns.SHOW_ID,
    }, ShowCrewColumns.CATEGORY + "=?", new String[] {
        department.toString(),
    }, null);
    List<Long> oldCrew = new ArrayList<>();
    LongSparseArray<Long> showToCrewIdMap = new LongSparseArray<>();
    while (oldCrewCursor.moveToNext()) {
      final long id = Cursors.getLong(oldCrewCursor, ShowCrewColumns.ID);
      final long showId = Cursors.getLong(oldCrewCursor, ShowCrewColumns.SHOW_ID);
      oldCrew.add(showId);
      showToCrewIdMap.put(showId, id);
    }
    oldCrewCursor.close();

    if (crew != null) {
      for (Credit credit : crew) {
        Show show = credit.getShow();
        final long showId = showHelper.partialUpdate(show);

        if (oldCrew.remove(showId)) {
          final long crewId = showToCrewIdMap.get(showId);
          ContentProviderOperation op = ContentProviderOperation.newUpdate(ShowCrew.withId(crewId))
              .withValue(ShowCrewColumns.SHOW_ID, showId)
              .withValue(ShowCrewColumns.PERSON_ID, personId)
              .withValue(ShowCrewColumns.CATEGORY, department.toString())
              .withValue(ShowCrewColumns.JOB, credit.getJob())
              .build();
          ops.add(op);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(ShowCrew.SHOW_CREW)
              .withValue(ShowCrewColumns.SHOW_ID, showId)
              .withValue(ShowCrewColumns.PERSON_ID, personId)
              .withValue(ShowCrewColumns.CATEGORY, department.toString())
              .withValue(ShowCrewColumns.JOB, credit.getJob())
              .build();
          ops.add(op);
        }
      }
    }

    for (long showId : oldCrew) {
      final long crewId = showToCrewIdMap.get(showId);

      ContentProviderOperation op =
          ContentProviderOperation.newDelete(ShowCrew.withId(crewId)).build();
      ops.add(op);
    }
  }
}
