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
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.PeopleService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncPersonMovieCredits extends CallJob<Credits> {

  @Inject transient PeopleService peopleService;
  @Inject transient MovieDatabaseHelper movieHelper;
  @Inject transient PersonDatabaseHelper personHelper;

  private long traktId;

  public SyncPersonMovieCredits(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncPersonMovieCredits" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<Credits> getCall() {
    return peopleService.movies(traktId, Extended.FULL);
  }

  @Override public boolean handleResponse(Credits credits) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    final long personId = personHelper.getId(traktId);

    Cursor oldCastCursor = getContentResolver().query(MovieCast.withPerson(personId), new String[] {
        Tables.MOVIE_CAST + "." + MovieCastColumns.ID, MovieCastColumns.MOVIE_ID,
    }, null, null, null);
    List<Long> oldCast = new ArrayList<>();
    LongSparseArray<Long> movieToCastIdMap = new LongSparseArray<>();
    while (oldCastCursor.moveToNext()) {
      final long id = Cursors.getLong(oldCastCursor, MovieCastColumns.ID);
      final long movieId = Cursors.getLong(oldCastCursor, MovieCastColumns.MOVIE_ID);
      oldCast.add(movieId);
      movieToCastIdMap.put(movieId, id);
    }
    oldCastCursor.close();

    List<Credit> cast = credits.getCast();
    if (cast != null) {
      for (Credit credit : cast) {
        Movie movie = credit.getMovie();
        final long movieId = movieHelper.partialUpdate(movie);

        if (oldCast.remove(movieId)) {
          final long castId = movieToCastIdMap.get(movieId);
          ContentProviderOperation op = ContentProviderOperation.newUpdate(MovieCast.withId(castId))
              .withValue(MovieCastColumns.MOVIE_ID, movieId)
              .withValue(MovieCastColumns.PERSON_ID, personId)
              .withValue(MovieCastColumns.CHARACTER, credit.getCharacter())
              .build();
          ops.add(op);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(MovieCast.MOVIE_CAST)
              .withValue(MovieCastColumns.MOVIE_ID, movieId)
              .withValue(MovieCastColumns.PERSON_ID, personId)
              .withValue(MovieCastColumns.CHARACTER, credit.getCharacter())
              .build();
          ops.add(op);
        }
      }
    }

    for (long movieId : oldCast) {
      final long castId = movieToCastIdMap.get(movieId);
      ContentProviderOperation op =
          ContentProviderOperation.newDelete(MovieCast.withId(castId)).build();
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
    Cursor oldCrewCursor = getContentResolver().query(MovieCrew.withPerson(personId), new String[] {
        Tables.MOVIE_CREW + "." + MovieCrewColumns.ID, MovieCrewColumns.MOVIE_ID,
    }, MovieCrewColumns.CATEGORY + "=?", new String[] {
        department.toString(),
    }, null);
    List<Long> oldCrew = new ArrayList<>();
    LongSparseArray<Long> movieToCrewIdMap = new LongSparseArray<>();
    while (oldCrewCursor.moveToNext()) {
      final long id = Cursors.getLong(oldCrewCursor, MovieCrewColumns.ID);
      final long movieId = Cursors.getLong(oldCrewCursor, MovieCrewColumns.MOVIE_ID);
      oldCrew.add(movieId);
      movieToCrewIdMap.put(movieId, id);
    }
    oldCrewCursor.close();

    if (crew != null) {
      for (Credit credit : crew) {
        Movie movie = credit.getMovie();
        final long movieId = movieHelper.partialUpdate(movie);

        if (oldCrew.remove(movieId)) {
          final long crewId = movieToCrewIdMap.get(movieId);
          ContentProviderOperation op = ContentProviderOperation.newUpdate(MovieCrew.withId(crewId))
              .withValue(MovieCrewColumns.MOVIE_ID, movieId)
              .withValue(MovieCrewColumns.PERSON_ID, personId)
              .withValue(MovieCrewColumns.CATEGORY, department.toString())
              .withValue(MovieCrewColumns.JOB, credit.getJob())
              .build();
          ops.add(op);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(MovieCrew.MOVIE_CREW)
              .withValue(MovieCrewColumns.MOVIE_ID, movieId)
              .withValue(MovieCrewColumns.PERSON_ID, personId)
              .withValue(MovieCrewColumns.CATEGORY, department.toString())
              .withValue(MovieCrewColumns.JOB, credit.getJob())
              .build();
          ops.add(op);
        }
      }
    }

    for (long movieId : oldCrew) {
      final long crewId = movieToCrewIdMap.get(movieId);

      ContentProviderOperation op =
          ContentProviderOperation.newDelete(MovieCrew.withId(crewId)).build();
      ops.add(op);
    }
  }
}
