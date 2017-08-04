/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.trakt;

import android.content.Context;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.api.body.ListInfoBody;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.common.event.ErrorEvent;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.jobs.R;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class UserList {

  @Inject Context context;
  @Inject UsersService usersServie;
  @Inject JobManager jobManager;

  public UserList() {
    Injector.obtain().inject(this);
  }

  public boolean create(String name, String description, Privacy privacy, boolean displayNumbers,
      boolean allowComments) {
    try {
      Call<CustomList> call = usersServie.createList(ListInfoBody.name(name)
          .description(description)
          .privacy(privacy)
          .displayNumbers(displayNumbers)
          .allowComments(allowComments));
      Response<CustomList> response = call.execute();
      if (response.isSuccessful()) {
        CustomList userList = response.body();
        ListWrapper.updateOrInsert(context.getContentResolver(), userList);
        jobManager.addJob(new SyncUserActivity());
        return true;
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to create list %s", name);
    }

    ErrorEvent.post(context.getString(R.string.list_create_error, name));
    return false;
  }

  public boolean update(long traktId, String name, String description, Privacy privacy,
      boolean displayNumbers, boolean allowComments) {
    try {
      Call<CustomList> call = usersServie.updateList(traktId, ListInfoBody.name(name)
          .description(description)
          .privacy(privacy)
          .displayNumbers(displayNumbers)
          .allowComments(allowComments));
      Response<CustomList> response = call.execute();
      if (response.isSuccessful()) {
        CustomList userList = response.body();
        ListWrapper.updateOrInsert(context.getContentResolver(), userList);
        jobManager.addJob(new SyncUserActivity());
        return true;
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to create list %s", name);
    }

    ErrorEvent.post(context.getString(R.string.list_update_error, name));
    return false;
  }

  public boolean delete(long traktId, String name) {
    try {
      Call<ResponseBody> call = usersServie.deleteList(traktId);
      Response<ResponseBody> response = call.execute();
      if (response.isSuccessful()) {
        ResponseBody body = response.body();
        jobManager.addJob(new SyncUserActivity());
        return true;
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to delete list %s", name);
    }

    ErrorEvent.post(context.getString(R.string.list_delete_error, name));
    return false;
  }
}
