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
import androidx.annotation.NonNull;
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
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler;

public class CreateListFragment extends DialogFragment {

  @Inject ListsTaskScheduler listsTaskScheduler;

  private Unbinder unbinder;

  @BindView(R.id.toolbar) Toolbar toolbar;

  @BindView(R.id.name) EditText name;

  @BindView(R.id.description) EditText description;

  @BindView(R.id.privacy) Spinner privacy;

  @BindView(R.id.displayNumbers) CheckBox displayNumbers;

  @BindView(R.id.allowComments) CheckBox allowComments;

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    if (getShowsDialog()) {
      setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    return inflater.inflate(R.layout.dialog_list_create, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    unbinder = ButterKnife.bind(this, view);

    toolbar.setTitle(R.string.action_list_create);
    toolbar.inflateMenu(R.menu.fragment_list_create);
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.menu_create:
            Privacy privacy = Privacy.PRIVATE;
            final int selectedPrivacy = CreateListFragment.this.privacy.getSelectedItemPosition();
            if (selectedPrivacy == 1) {
              privacy = Privacy.FRIENDS;
            } else if (selectedPrivacy == 2) {
              privacy = Privacy.PUBLIC;
            }

            listsTaskScheduler.createList(name.getText().toString(),
                description.getText().toString(), privacy, displayNumbers.isChecked(),
                allowComments.isChecked());

            dismiss();
            return true;
        }

        return false;
      }
    });

    if (TraktLinkSettings.isLinked(requireContext())) {
      ArrayAdapter<CharSequence> adapter =
          ArrayAdapter.createFromResource(requireContext(), R.array.list_privacy,
              android.R.layout.simple_spinner_item);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      privacy.setAdapter(adapter);
    } else {
      privacy.setVisibility(View.GONE);
      allowComments.setVisibility(View.GONE);
    }
  }

  @Override public void onDestroyView() {
    unbinder.unbind();
    unbinder = null;
    super.onDestroyView();
  }
}
