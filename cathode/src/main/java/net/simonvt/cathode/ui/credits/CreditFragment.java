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
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.ui.NavigationListener;

public class CreditFragment extends ToolbarGridFragment<CreditAdapter.ViewHolder>
    implements CreditAdapter.OnCreditClickListener {

  private static final String TAG = "net.simonvt.cathode.ui.person.CreditsFragment";

  private static final String ARG_ITEM_TYPE =
      "net.simonvt.cathode.ui.credits.CreditFragment.itemType";
  private static final String ARG_ITEM_ID = "net.simonvt.cathode.ui.credits.CreditFragment.itemId";
  private static final String ARG_DEPARTMENT =
      "net.simonvt.cathode.ui.credits.CreditFragment.department";

  private static final int LOADER_CREDIT = 1;

  private ItemType itemType;

  private long itemId;

  private Department department;

  private CreditAdapter adapter;

  private NavigationListener navigationListener;

  private int itemCount;

  public static String getTag(long itemId) {
    return TAG + "/" + itemId + "/" + Ids.newId();
  }

  public static Bundle getArgs(ItemType itemType, long itemId, Department department) {
    Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0");

    Bundle args = new Bundle();
    args.putSerializable(ARG_ITEM_TYPE, itemType);
    args.putLong(ARG_ITEM_ID, itemId);
    args.putSerializable(ARG_DEPARTMENT, department);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Bundle args = getArguments();
    itemType = (ItemType) args.getSerializable(ARG_ITEM_TYPE);
    itemId = args.getLong(ARG_ITEM_ID);
    department = (Department) args.getSerializable(ARG_DEPARTMENT);

    itemCount = getResources().getInteger(R.integer.creditColumns);

    switch (department) {
      case CAST:
        setTitle(R.string.person_department_cast);
        break;

      case PRODUCTION:
        setTitle(R.string.person_department_production);
        break;

      case ART:
        setTitle(R.string.person_department_art);
        break;

      case CREW:
        setTitle(R.string.person_department_crew);
        break;

      case COSTUME_AND_MAKEUP:
        setTitle(R.string.person_department_costume_makeup);
        break;

      case DIRECTING:
        setTitle(R.string.person_department_directing);
        break;

      case WRITING:
        setTitle(R.string.person_department_writing);
        break;

      case SOUND:
        setTitle(R.string.person_department_sound);
        break;

      case CAMERA:
        setTitle(R.string.person_department_camera);
        break;
    }

    getLoaderManager().initLoader(LOADER_CREDIT, null, creditsLoader);
  }

  @Override protected int getColumnCount() {
    return itemCount;
  }

  @Override public void onPersonClicked(long personId) {
    navigationListener.onDisplayPerson(personId);
  }

  private void setCredits(List<Credit> credits) {
    if (adapter == null) {
      adapter = new CreditAdapter(credits, this);
      setAdapter(adapter);
    } else {
      adapter.setCredits(credits);
    }
  }

  private LoaderManager.LoaderCallbacks<Credits> creditsLoader =
      new LoaderManager.LoaderCallbacks<Credits>() {
        @Override public Loader<Credits> onCreateLoader(int id, Bundle args) {
          return new CreditsLoader(getContext(), itemType, itemId);
        }

        @Override public void onLoadFinished(Loader<Credits> loader, Credits data) {
          switch (department) {
            case CAST:
              setCredits(data.getCast());
              break;

            case PRODUCTION:
              setCredits(data.getProduction());
              break;

            case ART:
              setCredits(data.getArt());
              break;

            case CREW:
              setCredits(data.getCrew());
              break;

            case COSTUME_AND_MAKEUP:
              setCredits(data.getCostumeAndMakeUp());
              break;

            case DIRECTING:
              setCredits(data.getDirecting());
              break;

            case WRITING:
              setCredits(data.getWriting());
              break;

            case SOUND:
              setCredits(data.getSound());
              break;

            case CAMERA:
              setCredits(data.getCamera());
              break;
          }
        }

        @Override public void onLoaderReset(Loader<Credits> loader) {
        }
      };
}
