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
package net.simonvt.cathode.ui.credits;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.fragment.RefreshableToolbarFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;

public class CreditsFragment extends RefreshableToolbarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.credits.CreditsFragment";

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.credits.CreditsFragment.itemType";
  private static final String ARG_ITEM_ID = "net.simonvt.cathode.ui.credits.CreditsFragment.itemId";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.credits.CreditsFragment.title";

  private static final int LOADER_CREDITS = 1;

  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;
  @Inject PersonTaskScheduler personScheduler;

  private NavigationListener navigationListener;

  private ItemType itemType;

  private long itemId;

  private String title;

  private Credits credits;

  private int itemCount;

  @BindView(R.id.cast_header) LinearLayout castHeader;
  @BindView(R.id.cast_items) LinearLayout castItems;

  @BindView(R.id.production_header) LinearLayout productionHeader;
  @BindView(R.id.production_items) LinearLayout productionItems;

  @BindView(R.id.art_header) LinearLayout artHeader;
  @BindView(R.id.art_items) LinearLayout artItems;

  @BindView(R.id.crew_header) LinearLayout crewHeader;
  @BindView(R.id.crew_items) LinearLayout crewItems;

  @BindView(R.id.costume_makeup_header) LinearLayout costumeMakeupHeader;
  @BindView(R.id.costume_makeup_items) LinearLayout costumeMakeupItems;

  @BindView(R.id.directing_header) LinearLayout directingHeader;
  @BindView(R.id.directing_items) LinearLayout directingItems;

  @BindView(R.id.writing_header) LinearLayout writingHeader;
  @BindView(R.id.writing_items) LinearLayout writingItems;

  @BindView(R.id.sound_header) LinearLayout soundHeader;
  @BindView(R.id.sound_items) LinearLayout soundItems;

  @BindView(R.id.camera_header) LinearLayout cameraHeader;
  @BindView(R.id.camera_items) LinearLayout cameraItems;

  public static String getTag(long itemId) {
    return TAG + "/" + itemId + "/" + Ids.newId();
  }

  public static Bundle getArgs(ItemType itemType, long itemId, String title) {
    Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0");

    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, itemType);
    args.putLong(ARG_ITEM_ID, itemId);
    args.putString(ARG_TITLE, title);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.inject(this);

    Bundle args = getArguments();
    itemId = args.getLong(ARG_ITEM_ID);
    title = args.getString(ARG_TITLE);
    itemType = (ItemType) args.getSerializable(ARG_TYPE);

    setTitle(title);

    itemCount = getResources().getInteger(R.integer.creditColumns);

    getLoaderManager().initLoader(LOADER_CREDITS, null, creditsLoader);
  }

  @Override public void onRefresh() {
    if (itemType == ItemType.SHOW) {
      showScheduler.syncCredits(itemId, onDoneListener);
    } else {
      movieScheduler.syncCredits(itemId, onDoneListener);
    }
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override
  protected View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_credits, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    updateView(credits);
  }

  @OnClick(R.id.cast_header) void onDisplayCastCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAST);
  }

  @OnClick(R.id.production_header) void onDisplayProductionCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.PRODUCTION);
  }

  @OnClick(R.id.art_header) void onDisplayArtCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.ART);
  }

  @OnClick(R.id.crew_header) void onDisplayCrewCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CREW);
  }

  @OnClick(R.id.costume_makeup_header) void onDisplayCostumeMakeUpCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.COSTUME_AND_MAKEUP);
  }

  @OnClick(R.id.directing_header) void onDisplayDirectingCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.DIRECTING);
  }

  @OnClick(R.id.writing_header) void onDisplayWritingCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.WRITING);
  }

  @OnClick(R.id.sound_header) void onDisplaySoundCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.SOUND);
  }

  @OnClick(R.id.camera_header) void onDisplayCameraCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAMERA);
  }

  private void updateView(Credits credits) {
    this.credits = credits;
    if (credits != null && getView() != null) {
      updateItems(castHeader, castItems, credits.getCast());
      updateItems(productionHeader, productionItems, credits.getProduction());
      updateItems(artHeader, artItems, credits.getArt());
      updateItems(crewHeader, crewItems, credits.getCrew());
      updateItems(costumeMakeupHeader, costumeMakeupItems, credits.getCostumeAndMakeUp());
      updateItems(directingHeader, directingItems, credits.getDirecting());
      updateItems(writingHeader, writingItems, credits.getWriting());
      updateItems(soundHeader, soundItems, credits.getSound());
      updateItems(cameraHeader, cameraItems, credits.getCamera());
    }
  }

  private void updateItems(View header, ViewGroup items, List<Credit> credits) {
    items.removeAllViews();

    final int size = credits != null ? credits.size() : 0;
    if (size > 0) {
      header.setVisibility(View.VISIBLE);
      items.setVisibility(View.VISIBLE);

      for (int i = 0; i < size && i < itemCount; i++) {
        final Credit credit = credits.get(i);

        View view =
            LayoutInflater.from(getContext()).inflate(R.layout.credit_item_credit, items, false);

        RemoteImageView headshot = ButterKnife.findById(view, R.id.headshot);
        TextView name = ButterKnife.findById(view, R.id.name);
        TextView job = ButterKnife.findById(view, R.id.job);

        headshot.setImage(credit.getHeadshot());
        name.setText(credit.getName());
        if (credit.getJob() != null) {
          job.setText(credit.getJob());
        } else {
          job.setText(credit.getCharacter());
        }

        view.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            navigationListener.onDisplayPerson(credit.getPersonId());
          }
        });

        items.addView(view);
      }
    } else {
      header.setVisibility(View.GONE);
      items.setVisibility(View.GONE);
    }
  }

  private LoaderManager.LoaderCallbacks<Credits> creditsLoader =
      new LoaderManager.LoaderCallbacks<Credits>() {
        @Override public Loader<Credits> onCreateLoader(int id, Bundle args) {
          return new CreditsLoader(getContext(), itemType, itemId);
        }

        @Override public void onLoadFinished(Loader<Credits> loader, Credits data) {
          updateView(data);
        }

        @Override public void onLoaderReset(Loader<Credits> loader) {
        }
      };
}
