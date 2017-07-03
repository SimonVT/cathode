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

package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CastMember;
import net.simonvt.cathode.api.entity.CrewMember;
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.PersonDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncShowCredits extends CallJob<People> {

  @Inject transient ShowsService showsService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient PersonDatabaseHelper personHelper;

  private long traktId;

  public SyncShowCredits(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncShowCredits" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public Call<People> getCall() {
    return showsService.getPeople(traktId, Extended.FULL);
  }

  @Override public boolean handleResponse(People people) {
    List<CastMember> characters = people.getCast();

    final long showId = showHelper.getId(traktId);

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    ops.add(ContentProviderOperation.newDelete(ShowCast.fromShow(showId)).build());
    ops.add(ContentProviderOperation.newDelete(ShowCrew.fromShow(showId)).build());

    if (characters != null) {
      for (CastMember character : characters) {
        Person person = character.getPerson();
        final long personId = personHelper.updateOrInsert(person);

        ContentProviderOperation op = ContentProviderOperation.newInsert(ShowCast.SHOW_CAST)
            .withValue(ShowCastColumns.SHOW_ID, showId)
            .withValue(ShowCastColumns.PERSON_ID, personId)
            .withValue(ShowCastColumns.CHARACTER, character.getCharacter())
            .build();
        ops.add(op);
      }
    }

    People.Crew crew = people.getCrew();
    if (crew != null) {
      insertCrew(ops, showId, Department.PRODUCTION, crew.getProduction());
      insertCrew(ops, showId, Department.ART, crew.getArt());
      insertCrew(ops, showId, Department.CREW, crew.getCrew());
      insertCrew(ops, showId, Department.COSTUME_AND_MAKEUP, crew.getCostumeAndMakeUp());
      insertCrew(ops, showId, Department.DIRECTING, crew.getDirecting());
      insertCrew(ops, showId, Department.WRITING, crew.getWriting());
      insertCrew(ops, showId, Department.SOUND, crew.getSound());
      insertCrew(ops, showId, Department.CAMERA, crew.getCamera());
    }

    return applyBatch(ops);
  }

  private void insertCrew(List<ContentProviderOperation> ops, long showId, Department department,
      List<CrewMember> crew) {
    if (crew != null) {
      for (CrewMember member : crew) {
        Person person = member.getPerson();
        final long personId = personHelper.updateOrInsert(person);

        ContentProviderOperation op = ContentProviderOperation.newInsert(ShowCrew.SHOW_CREW)
            .withValue(ShowCrewColumns.SHOW_ID, showId)
            .withValue(ShowCrewColumns.CATEGORY, department.toString())
            .withValue(ShowCrewColumns.PERSON_ID, personId)
            .withValue(ShowCrewColumns.JOB, member.getJob())
            .build();
        ops.add(op);
      }
    }
  }
}
