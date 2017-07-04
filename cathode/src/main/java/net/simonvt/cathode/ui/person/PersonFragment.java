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

package net.simonvt.cathode.ui.person;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
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
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.Intents;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.widget.RemoteImageView;

public class PersonFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.person.PersonFragment";

  private static final String ARG_PERSON_ID =
      "net.simonvt.cathode.ui.person.PersonFragment.personId";

  private static final int LOADER_PERSON = 1;

  @Inject PersonTaskScheduler personScheduler;
  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  private long personId;

  private Person person;

  private int itemCount;

  @BindView(R.id.headshot) RemoteImageView headshot;
  @BindView(R.id.bornTitle) View bornTitle;
  @BindView(R.id.birthday) TextView birthday;
  @BindView(R.id.birthplace) TextView birthplace;
  @BindView(R.id.deathTitle) View deathTitle;
  @BindView(R.id.death) TextView death;
  @BindView(R.id.biography) TextView biography;

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

  @BindView(R.id.viewOnTrakt) View viewOnTrakt;

  private NavigationListener navigationListener;

  public static String getTag(long personId) {
    return TAG + "/" + personId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long personId) {
    Preconditions.checkArgument(personId >= 0, "personId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_PERSON_ID, personId);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.obtain().inject(this);

    Bundle args = getArguments();
    personId = args.getLong(ARG_PERSON_ID);

    itemCount = getResources().getInteger(R.integer.personCreditColumns);

    getLoaderManager().initLoader(LOADER_PERSON, null, personLoader);
  }

  @Override public void onRefresh() {
    personScheduler.sync(personId, onDoneListener);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override
  protected View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_person, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    updateView(person);
  }

  @OnClick(R.id.cast_header) void onDisplayCastCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CAST);
  }

  @OnClick(R.id.production_header) void onDisplayProductionCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.PRODUCTION);
  }

  @OnClick(R.id.art_header) void onDisplayArtCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.ART);
  }

  @OnClick(R.id.crew_header) void onDisplayCrewCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CREW);
  }

  @OnClick(R.id.costume_makeup_header) void onDisplayCostumeMakeUpCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.COSTUME_AND_MAKEUP);
  }

  @OnClick(R.id.directing_header) void onDisplayDirectingCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.DIRECTING);
  }

  @OnClick(R.id.writing_header) void onDisplayWritingCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.WRITING);
  }

  @OnClick(R.id.sound_header) void onDisplaySoundCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.SOUND);
  }

  @OnClick(R.id.camera_header) void onDisplayCameraCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CAMERA);
  }

  private void updateView(final Person person) {
    this.person = person;
    if (person != null && getView() != null) {
      setTitle(person.getName());
      setBackdrop(person.getScreenshot());
      headshot.setImage(person.getHeadshot());

      if (!TextUtils.isEmpty(person.getBirthday())) {
        bornTitle.setVisibility(View.VISIBLE);
        birthday.setVisibility(View.VISIBLE);
        birthplace.setVisibility(View.VISIBLE);

        birthday.setText(person.getBirthday());
        birthplace.setText(person.getBirthplace());
      } else {
        bornTitle.setVisibility(View.GONE);
        birthday.setVisibility(View.GONE);
        birthplace.setVisibility(View.GONE);
      }

      if (!TextUtils.isEmpty(person.getDeath())) {
        deathTitle.setVisibility(View.VISIBLE);
        death.setVisibility(View.VISIBLE);
        death.setText(person.getDeath());
      } else {
        deathTitle.setVisibility(View.GONE);
        death.setVisibility(View.GONE);
      }

      biography.setText(person.getBiography());

      updateItems(castHeader, castItems, person.getCredits().getCast());
      updateItems(productionHeader, productionItems, person.getCredits().getProduction());
      updateItems(artHeader, artItems, person.getCredits().getArt());
      updateItems(crewHeader, crewItems, person.getCredits().getCrew());
      updateItems(costumeMakeupHeader, costumeMakeupItems,
          person.getCredits().getCostumeAndMakeUp());
      updateItems(directingHeader, directingItems, person.getCredits().getDirecting());
      updateItems(writingHeader, writingItems, person.getCredits().getWriting());
      updateItems(soundHeader, soundItems, person.getCredits().getSound());
      updateItems(cameraHeader, cameraItems, person.getCredits().getCamera());

      viewOnTrakt.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getTraktPersonUrl(person.getTraktId()));
        }
      });

      if (TraktTimestamps.shouldSyncPerson(person.getLastSync())) {
        personScheduler.sync(personId);
      }
    }
  }

  private void updateItems(View header, ViewGroup items, List<PersonCredit> credits) {
    items.removeAllViews();

    final int size = credits != null ? credits.size() : 0;
    if (size > 0) {
      header.setVisibility(View.VISIBLE);
      items.setVisibility(View.VISIBLE);

      for (int i = 0; i < size && i < itemCount; i++) {
        final PersonCredit credit = credits.get(i);

        View view =
            LayoutInflater.from(getContext()).inflate(R.layout.person_item_credit, items, false);

        RemoteImageView poster = ButterKnife.findById(view, R.id.poster);
        TextView title = ButterKnife.findById(view, R.id.title);
        TextView job = ButterKnife.findById(view, R.id.job);

        poster.setImage(credit.getPoster());
        title.setText(credit.getTitle());

        if (credit.getJob() != null) {
          job.setText(credit.getJob());
        } else {
          job.setText(credit.getCharacter());
        }

        view.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            if (credit.getItemType() == ItemType.SHOW) {
              navigationListener.onDisplayShow(credit.getItemId(), credit.getTitle(),
                  credit.getOverview(), LibraryType.WATCHED);
            } else {
              navigationListener.onDisplayMovie(credit.getItemId(), credit.getTitle(),
                  credit.getOverview());
            }
          }
        });

        items.addView(view);
      }
    } else {
      header.setVisibility(View.GONE);
      items.setVisibility(View.GONE);
    }
  }

  private LoaderManager.LoaderCallbacks<Person> personLoader =
      new LoaderManager.LoaderCallbacks<Person>() {
        @Override public Loader<Person> onCreateLoader(int id, Bundle args) {
          return new PersonLoader(getContext(), personId);
        }

        @Override public void onLoadFinished(Loader<Person> loader, Person data) {
          updateView(data);
        }

        @Override public void onLoaderReset(Loader<Person> loader) {
        }
      };
}
