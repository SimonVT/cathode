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
package net.simonvt.cathode.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import java.util.ArrayList;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.util.FragmentStack.StackEntry;
import net.simonvt.cathode.ui.comments.CommentsFragment;
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment;
import net.simonvt.cathode.ui.show.EpisodeFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment;
import net.simonvt.cathode.ui.show.ShowFragment;
import timber.log.Timber;

public class EpisodeDetailsActivity extends NavigationListenerActivity {

  public static final String EXTRA_ID = "net.simonvt.cathode.ui.DetailsActivity.id";
  public static final String EXTRA_SHOW_ID = "net.simonvt.cathode.ui.DetailsActivity.showId";
  public static final String EXTRA_SHOW_TITLE = "net.simonvt.cathode.ui.DetailsActivity.showTitle";
  public static final String EXTRA_SHOW_OVERVIEW =
      "net.simonvt.cathode.ui.DetailsActivity.showOverview";

  private long id;

  private long showId;

  private String showTitle;

  private String showOverview;

  @Override protected void onCreate(Bundle inState) {
    setTheme(R.style.Theme);
    super.onCreate(inState);
    setContentView(R.layout.activity_details);

    Intent intent = getIntent();

    if (CalendarContract.ACTION_HANDLE_CUSTOM_EVENT.equals(intent.getAction())) {
      String uriString = intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI);
      if (TextUtils.isEmpty(uriString)) {
        finish();
      } else {
        Uri uri = Uri.parse(uriString);
        String idSegment = uri.getPathSegments().get(0);
        if (!TextUtils.isEmpty(idSegment)) {
          id = Long.parseLong(idSegment);
        } else {
          finish();
        }
      }
    } else {
      id = intent.getLongExtra(EXTRA_ID, -1L);
      showId = intent.getLongExtra(EXTRA_SHOW_ID, -1L);
      showTitle = intent.getStringExtra(EXTRA_SHOW_TITLE);
      showOverview = intent.getStringExtra(EXTRA_SHOW_OVERVIEW);
    }

    if (id == -1L) {
      Timber.e(new Exception("ID was " + id));
      finish();
    } else {
      if (inState == null) {
        Bundle args = EpisodeFragment.getArgs(id, showTitle);
        EpisodeFragment f = new EpisodeFragment();
        f.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .add(R.id.content, f, EpisodeFragment.getTag(id))
            .commitNow();
      }
    }
  }

  public static Uri createUri(long episodeId) {
    return Uri.parse("cathode://episode/" + episodeId);
  }

  @Override public void upFromEpisode(long showId, String showTitle, long seasonId) {
    this.showId = showId;
    this.showTitle = showTitle;
    onHomeClicked();
  }

  @Override public void onHomeClicked() {
    ArrayList<StackEntry> stack = new ArrayList<>();

    StackEntry showEntry = new StackEntry(ShowFragment.class, ShowFragment.getTag(showId),
        ShowFragment.getArgs(showId, showTitle, showOverview, LibraryType.WATCHED));
    stack.add(showEntry);

    Intent i = new Intent(this, HomeActivity.class);
    i.setAction(HomeActivity.ACTION_REPLACE_STACK);
    i.putParcelableArrayListExtra(HomeActivity.EXTRA_STACK_ENTRIES, stack);

    startActivity(i);
    finish();
  }

  @Override public void onDisplayComments(ItemType type, long itemId) {
    displayOnTop(CommentsFragment.class, CommentsFragment.TAG,
        CommentsFragment.getArgs(ItemType.EPISODE, itemId));
  }

  @Override public void onDisplayEpisodeHistory(long episodeId, String showTitle) {
    displayOnTop(EpisodeHistoryFragment.class, EpisodeHistoryFragment.getTag(episodeId),
        EpisodeHistoryFragment.getArgs(episodeId, showTitle));
  }

  @Override public void onSelectEpisodeWatchedDate(long episodeId, String title) {
    displayOnTop(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE, episodeId,
            title));
  }

  private void displayOnTop(Class fragment, String tag, Bundle args) {
    ArrayList<StackEntry> stack = new ArrayList<>();

    StackEntry episodeEntry = new StackEntry(EpisodeFragment.class, EpisodeFragment.getTag(id),
        EpisodeFragment.getArgs(id, showTitle));
    stack.add(episodeEntry);

    StackEntry entryOnTop = new StackEntry(fragment, tag, args);
    stack.add(entryOnTop);

    Intent i = new Intent(this, HomeActivity.class);
    i.setAction(HomeActivity.ACTION_REPLACE_STACK);
    i.putParcelableArrayListExtra(HomeActivity.EXTRA_STACK_ENTRIES, stack);

    startActivity(i);
    finish();
  }
}
