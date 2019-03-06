/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.search;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.provider.DatabaseContract.RecentQueriesColumns;
import net.simonvt.cathode.provider.ProviderSchematic.RecentQueries;

public class SearchViewModel extends AndroidViewModel {

  private LiveData<List<String>> recents;

  public SearchViewModel(@NonNull Application application) {
    super(application);
    recents = new MappedCursorLiveData<>(application, RecentQueries.RECENT_QUERIES, new String[] {
        RecentQueriesColumns.QUERY,
    }, null, null, RecentQueriesColumns.QUERIED_AT + " DESC LIMIT 3",
        new MappedCursorLiveData.CursorMapper<List<String>>() {
          @Override public List<String> map(Cursor cursor) {
            List<String> recentQueries = new ArrayList<>();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
              final String query = Cursors.getString(cursor, RecentQueriesColumns.QUERY);
              recentQueries.add(query);
            }
            return recentQueries;
          }
        });
  }

  public LiveData<List<String>> getRecents() {
    return recents;
  }
}