/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.sync.movies;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CastMember;
import net.simonvt.cathode.api.entity.CrewMember;
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.PersonWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncMovieCrew extends TraktTask {

  @Inject transient MoviesService moviesService;

  private long traktId;

  public SyncMovieCrew(long traktId) {
    this.traktId = traktId;
  }

  @Override protected void doTask() {
    People people = moviesService.getPeople(traktId);

    final long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    ContentProviderOperation op =
        ContentProviderOperation.newDelete(MovieCast.fromMovie(movieId)).build();
    ops.add(op);
    op = ContentProviderOperation.newDelete(MovieCrew.fromMovie(movieId)).build();
    ops.add(op);

    for (CastMember castMember : people.getCast()) {
      final long personId =
          PersonWrapper.updateOrInsert(getContentResolver(), castMember.getPerson());
      op = ContentProviderOperation.newInsert(MovieCast.MOVIE_CAST)
          .withValue(MovieCastColumns.MOVIE_ID, movieId)
          .withValue(MovieCastColumns.PERSON_ID, personId)
          .withValue(MovieCastColumns.CHARACTER, castMember.getCharacter())
          .build();
      ops.add(op);
    }

    People.Crew crew = people.getCrew();
    insertCrew(ops, movieId, "production", crew.getProduction());
    insertCrew(ops, movieId, "art", crew.getArt());
    insertCrew(ops, movieId, "crew", crew.getCrew());
    insertCrew(ops, movieId, "costume & make-up", crew.getCostumeAndMakeUp());
    insertCrew(ops, movieId, "directing", crew.getDirecting());
    insertCrew(ops, movieId, "writing", crew.getWriting());
    insertCrew(ops, movieId, "sound", crew.getSound());
    insertCrew(ops, movieId, "camera", crew.getCamera());

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, "Updating movie crew failed");
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating movie crew failed");
      postOnFailure();
    }
  }

  private void insertCrew(ArrayList<ContentProviderOperation> ops, long movieId, String category,
      List<CrewMember> crew) {
    if (crew == null) {
      return;
    }
    for (CrewMember crewMember : crew) {
      final long personId =
          PersonWrapper.updateOrInsert(getContentResolver(), crewMember.getPerson());
      ContentProviderOperation op = ContentProviderOperation.newInsert(MovieCrew.MOVIE_CREW)
          .withValue(MovieCrewColumns.MOVIE_ID, movieId)
          .withValue(MovieCrewColumns.PERSON_ID, personId)
          .withValue(MovieCrewColumns.CATEGORY, category)
          .withValue(MovieCrewColumns.JOB, crewMember.getJob())
          .build();
      ops.add(op);
    }
  }
}
