/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.action.lists;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CreateListBody;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.settings.Settings;

public class CreateList extends Job {

  @Inject transient UsersService usersServie;

  private Long listId;

  private String name;

  private String description;

  private Privacy privacy;

  private Boolean displayNumbers;

  private Boolean allowComments;

  public CreateList(long listId, String name, String description, Privacy privacy,
      Boolean displayNumbers, Boolean allowComments) {
    super(Flags.REQUIRES_AUTH);
    this.listId = listId;
    this.name = name;
    this.description = description;
    this.privacy = privacy;
    this.displayNumbers = displayNumbers;
    this.allowComments = allowComments;
  }

  @Override public String key() {
    return "CreateList&name=" + name;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public void perform() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());

    CustomList list = usersServie.createList(CreateListBody.name(name)
        .description(description)
        .privacy(privacy)
        .displayNumbers(displayNumbers)
        .allowComments(allowComments));

    Cursor c = getContentResolver().query(Lists.LISTS, new String[] {
        ListsColumns.ID,
    }, ListsColumns.ID + "=?", new String[] {
        String.valueOf(listId),
    }, null);

    if (c.moveToFirst()) {
      ListWrapper.update(getContentResolver(), listId, list);
    } else {
      ListWrapper.updateOrInsert(getContentResolver(), list);
    }

    c.close();
  }
}
