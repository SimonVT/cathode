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
package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;

public class ShowsCollectionFragment extends ShowsFragment {

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    setEmptyText(R.string.empty_show_collection);
    setTitle(R.string.title_shows_collection);
  }

  @Override protected LibraryType getLibraryType() {
    return LibraryType.COLLECTION;
  }

  @Override protected int getLoaderId() {
    return Loaders.LOADER_SHOWS_COLLECTION;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = Shows.SHOWS_COLLECTION;
    CursorLoader cl =
        new CursorLoader(getActivity(), contentUri, ShowsWithNextAdapter.PROJECTION, null, null,
            Shows.DEFAULT_SORT);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }
}
