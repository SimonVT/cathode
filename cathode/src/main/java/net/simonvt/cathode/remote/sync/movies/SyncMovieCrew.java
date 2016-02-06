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
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.PersonWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;
import timber.log.Timber;

public class SyncMovieCrew extends CallJob<People> {

  @Inject transient MoviesService moviesService;

  @Inject transient MovieDatabaseHelper movieHelper;

  private long traktId;

  public SyncMovieCrew(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncMovieCrew" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public Call<People> getCall() {
    return moviesService.getPeople(traktId, Extended.FULL_IMAGES);
  }

  @Override public void handleResponse(People people) {
    final long movieId = movieHelper.getId(traktId);
    if (movieId == -1L) {
      return;
    }

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    ContentProviderOperation op =
        ContentProviderOperation.newDelete(MovieCast.fromMovie(movieId)).build();
    ops.add(op);
    op = ContentProviderOperation.newDelete(MovieCrew.fromMovie(movieId)).build();
    ops.add(op);

    List<CastMember> cast = people.getCast();
    if (cast != null) {
      for (CastMember castMember : cast) {
        Person person = castMember.getPerson();
        final long personId = PersonWrapper.updateOrInsert(getContentResolver(), person);

        op = ContentProviderOperation.newInsert(MovieCast.MOVIE_CAST)
            .withValue(MovieCastColumns.MOVIE_ID, movieId)
            .withValue(MovieCastColumns.PERSON_ID, personId)
            .withValue(MovieCastColumns.CHARACTER, castMember.getCharacter())
            .build();
        ops.add(op);
      }
    }

    People.Crew crew = people.getCrew();
    if (crew != null) {
      insertCrew(ops, movieId, "production", crew.getProduction());
      insertCrew(ops, movieId, "art", crew.getArt());
      insertCrew(ops, movieId, "crew", crew.getCrew());
      insertCrew(ops, movieId, "costume & make-up", crew.getCostumeAndMakeUp());
      insertCrew(ops, movieId, "directing", crew.getDirecting());
      insertCrew(ops, movieId, "writing", crew.getWriting());
      insertCrew(ops, movieId, "sound", crew.getSound());
      insertCrew(ops, movieId, "camera", crew.getCamera());
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Updating movie crew failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating movie crew failed");
      throw new JobFailedException(e);
    }
  }

  private void insertCrew(ArrayList<ContentProviderOperation> ops, long movieId, String category,
      List<CrewMember> crew) {
    if (crew == null) {
      return;
    }
    for (CrewMember crewMember : crew) {
      Person person = crewMember.getPerson();
      final long personId = PersonWrapper.updateOrInsert(getContentResolver(), person);

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
