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
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CastMember;
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.PersonWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCharacters;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import timber.log.Timber;

public class SyncShowCast extends Job {

  @Inject transient ShowsService showsService;

  private long traktId;

  public SyncShowCast(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncShowCast" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public void perform() {
    People people = showsService.getPeople(traktId, Extended.FULL_IMAGES);
    List<CastMember> characters = people.getCast();

    final long showId = ShowWrapper.getShowId(getContentResolver(), traktId);

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    ops.add(ContentProviderOperation.newDelete(ShowCharacters.fromShow(showId)).build());

    for (CastMember character : characters) {
      Person person = character.getPerson();
      final long personId = PersonWrapper.updateOrInsert(getContentResolver(), person);

      ContentProviderOperation op =
          ContentProviderOperation.newInsert(ShowCharacters.SHOW_CHARACTERS)
              .withValue(DatabaseContract.ShowCharacterColumns.SHOW_ID, showId)
              .withValue(DatabaseContract.ShowCharacterColumns.PERSON_ID, personId)
              .withValue(DatabaseContract.ShowCharacterColumns.CHARACTER, character.getCharacter())
              .build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Updating show characters failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating show characters failed");
      throw new JobFailedException(e);
    }
  }
}
