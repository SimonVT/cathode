/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.scheduler;

import android.content.Context;
import javax.inject.Inject;
import net.simonvt.cathode.provider.helper.SearchDatabaseHelper;

public class SearchTaskScheduler extends BaseTaskScheduler {

  @Inject SearchDatabaseHelper searchHelper;

  public SearchTaskScheduler(Context context) {
    super(context);
  }

  public void insertRecentQuery(final String query) {
    execute(new Runnable() {
      @Override public void run() {
        searchHelper.insertRecentQuery(query);
      }
    });
  }
}
