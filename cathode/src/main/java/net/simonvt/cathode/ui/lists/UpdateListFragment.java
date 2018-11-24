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
package net.simonvt.cathode.ui.lists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler;

public class UpdateListFragment extends DialogFragment {

  private static final String ARG_LIST_ID =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.listId";
  private static final String ARG_NAME = "net.simonvt.cathode.ui.lists.UpdateListFragment.name";
  private static final String ARG_DESCRIPTION =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.description";
  private static final String ARG_PRIVACY =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.privacy";
  private static final String ARG_DISPLAY_NUMBERS =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.displayNumbers";
  private static final String ARG_ALLOW_COMMENTS =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.allowComments";

  @Inject ListsTaskScheduler listsTaskScheduler;

  private Unbinder unbinder;

  @BindView(R.id.toolbar) Toolbar toolbar;

  @BindView(R.id.name) EditText name;

  @BindView(R.id.description) EditText description;

  @BindView(R.id.privacy) Spinner privacy;

  @BindView(R.id.displayNumbers) CheckBox displayNumbers;

  @BindView(R.id.allowComments) CheckBox allowComments;

  public static Bundle getArgs(long listId, String name, String description, Privacy privacy,
      boolean displayNumbers, boolean allowComments) {
    Preconditions.checkArgument(listId >= 0, "listId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_LIST_ID, listId);
    args.putString(ARG_NAME, name);
    args.putString(ARG_DESCRIPTION, description);
    args.putString(ARG_PRIVACY, privacy.toString());
    args.putBoolean(ARG_DISPLAY_NUMBERS, displayNumbers);
    args.putBoolean(ARG_ALLOW_COMMENTS, allowComments);
    return args;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidSupportInjection.inject(this);

    if (getShowsDialog()) {
      setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.lists_fragment_update, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);

    Bundle args = getArguments();
    final long listId = args.getLong(ARG_LIST_ID);

    toolbar.setTitle(R.string.action_list_update);
    toolbar.inflateMenu(R.menu.fragment_list_update);
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.menu_update:
            Privacy privacy = Privacy.PRIVATE;
            final int selectedPrivacy = UpdateListFragment.this.privacy.getSelectedItemPosition();
            if (selectedPrivacy == 1) {
              privacy = Privacy.FRIENDS;
            } else if (selectedPrivacy == 2) {
              privacy = Privacy.PUBLIC;
            }

            listsTaskScheduler.updateList(listId, name.getText().toString(),
                description.getText().toString(), privacy, displayNumbers.isChecked(),
                allowComments.isChecked());

            dismiss();
            return true;
        }

        return false;
      }
    });

    ArrayAdapter<CharSequence> adapter =
        ArrayAdapter.createFromResource(getActivity(), R.array.list_privacy,
            android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    privacy.setAdapter(adapter);

    String name = args.getString(ARG_NAME);
    this.name.setText(name);

    String description = args.getString(ARG_DESCRIPTION);
    this.description.setText(description);

    Privacy privacy = Privacy.fromValue(args.getString(ARG_PRIVACY));
    switch (privacy) {
      case FRIENDS:
        this.privacy.setSelection(1);
        break;

      case PUBLIC:
        this.privacy.setSelection(2);
        break;

      default:
        this.privacy.setSelection(0);
        break;
    }

    boolean displayNumbers = args.getBoolean(ARG_DISPLAY_NUMBERS);
    this.displayNumbers.setChecked(displayNumbers);

    boolean allowComments = args.getBoolean(ARG_ALLOW_COMMENTS);
    this.allowComments.setChecked(allowComments);
  }

  @Override public void onDestroyView() {
    unbinder.unbind();
    unbinder = null;
    super.onDestroyView();
  }
}
