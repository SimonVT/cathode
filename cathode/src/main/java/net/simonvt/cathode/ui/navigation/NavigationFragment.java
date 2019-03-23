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
package net.simonvt.cathode.ui.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.AbsAdapterFragment;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.settings.ProfileSettings;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.StartPage;
import net.simonvt.cathode.settings.TraktLinkSettings;

public class NavigationFragment extends AbsAdapterFragment {

  public static final String TAG = "net.simonvt.cathode.ui.navigation.NavigationFragment";

  private static final String STATE_SELECTED_POSITION =
      "net.simonvt.cathode.ui.navigation.NavigationFragment.selectedPosition";

  public interface OnMenuClickListener {

    boolean onMenuItemClicked(int id);
  }

  private List<NavigationItem> menuItems = new ArrayList<>();

  {
    menuItems.add(new MenuItem(R.id.menu_dashboard, R.string.navigation_dashboard,
        R.drawable.ic_nav_dashboard_24dp));

    menuItems.add(new Divider());

    menuItems.add(new CategoryItem(R.string.navigation_title_shows));
    menuItems.add(new MenuItem(R.id.menu_shows_upcoming, R.string.navigation_shows_upcoming,
        R.drawable.ic_nav_upcoming_24dp));
    menuItems.add(new MenuItem(R.id.menu_shows_watched, R.string.navigation_shows_watched,
        R.drawable.ic_nav_shows_watched_24dp));
    menuItems.add(new MenuItem(R.id.menu_shows_collection, R.string.navigation_shows_collection,
        R.drawable.ic_nav_shows_collected_24dp));
    menuItems.add(new MenuItem(R.id.menu_shows_watchlist, R.string.navigation_shows_watchlist,
        R.drawable.ic_nav_watchlist_24dp));
    menuItems.add(new MenuItem(R.id.menu_shows_suggestions, R.string.navigation_shows_suggestions,
        R.drawable.ic_nav_suggestions_black_24dp));

    menuItems.add(new Divider());

    menuItems.add(new CategoryItem(R.string.navigation_title_movies));
    menuItems.add(new MenuItem(R.id.menu_movies_watched, R.string.navigation_movies_watched,
        R.drawable.ic_nav_movies_watched_24dp));
    menuItems.add(new MenuItem(R.id.menu_movies_collection, R.string.navigation_movies_collection,
        R.drawable.ic_nav_movies_collected_24dp));
    menuItems.add(new MenuItem(R.id.menu_movies_watchlist, R.string.navigation_movies_watchlist,
        R.drawable.ic_nav_watchlist_24dp));
    menuItems.add(new MenuItem(R.id.menu_movies_suggestions, R.string.navigation_movies_suggestions,
        R.drawable.ic_nav_suggestions_black_24dp));

    menuItems.add(new Divider());

    menuItems.add(
        new MenuItem(R.id.menu_lists, R.string.navigation_lists, R.drawable.ic_nav_list_24dp));

    menuItems.add(new Divider());

    menuItems.add(
        new MenuItem(R.id.menu_stats, R.string.navigation_stats, R.drawable.ic_nav_stats_24dp));
    menuItems.add(new MenuItem(R.id.menu_settings, R.string.navigation_settings,
        R.drawable.ic_nav_settings_24dp));
  }

  private OnMenuClickListener listener;

  private int selectedPosition = 1;

  private NavigationAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (OnMenuClickListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);

    if (inState != null) {
      selectedPosition = inState.getInt(STATE_SELECTED_POSITION);
    } else {
      final String startPagePref =
          Settings.get(requireContext()).getString(Settings.START_PAGE, null);
      StartPage startPage = StartPage.fromValue(startPagePref, StartPage.DASHBOARD);
      selectedPosition = getPositionForId(startPage.getMenuId());
    }

    ProfileSettings.get(requireContext())
        .registerOnSharedPreferenceChangeListener(profileSettingsListener);
    String username =
        ProfileSettings.get(requireContext()).getString(ProfileSettings.USERNAME, null);
    String avatar = ProfileSettings.get(requireContext()).getString(ProfileSettings.AVATAR, null);

    adapter = new NavigationAdapter(requireContext(), menuItems);
    adapter.setUsername(username);
    adapter.setAvatar(avatar);
    setAdapter(adapter);
  }

  private int getPositionForId(long itemId) {
    for (int i = 0; i < menuItems.size(); i++) {
      NavigationItem item = menuItems.get(i);
      if (item instanceof MenuItem && ((MenuItem) item).id == itemId) {
        return 1 + i;
      }
    }

    return 1;
  }

  private SharedPreferences.OnSharedPreferenceChangeListener profileSettingsListener =
      new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
          if (ProfileSettings.USERNAME.equals(key)) {
            final String username = settings.getString(ProfileSettings.USERNAME, null);
            adapter.setUsername(username);
          }
        }
      };

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_SELECTED_POSITION, selectedPosition);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    return inflater.inflate(R.layout.fragment_navigation, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    getAdapterView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    getAdapterView().setItemChecked(selectedPosition, true);
  }

  @Override public void onDestroy() {
    ProfileSettings.get(requireContext())
        .unregisterOnSharedPreferenceChangeListener(profileSettingsListener);
    super.onDestroy();
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    MenuItem item = (MenuItem) getAdapter().getItem(position);

    if (listener.onMenuItemClicked(item.id)) {
      selectedPosition = position;
    }

    getAdapterView().setItemChecked(selectedPosition, true);
  }

  public void setSelectedId(long id) {
    selectedPosition = getPositionForId(id);
    adapter.notifyDataSetChanged();

    if (getAdapterView() != null) {
      getAdapterView().setItemChecked(selectedPosition, true);
    }
  }

  private abstract static class NavigationItem {

    final int type;

    NavigationItem(int type) {
      this.type = type;
    }
  }

  private static class CategoryItem extends NavigationItem {

    final int title;

    CategoryItem(int title) {
      super(NavigationAdapter.TYPE_CATEGORY);
      this.title = title;
    }
  }

  private static class MenuItem extends NavigationItem {

    final int id;

    final int iconRes;

    final int title;

    MenuItem(int id, int title, int iconRes) {
      super(NavigationAdapter.TYPE_ITEM);
      this.id = id;
      this.title = title;
      this.iconRes = iconRes;
    }
  }

  private static class Divider extends NavigationItem {

    Divider() {
      super(NavigationAdapter.TYPE_DIVIDER);
    }
  }

  private class NavigationAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CATEGORY = 1;
    private static final int TYPE_ITEM = 2;
    private static final int TYPE_DIVIDER = 3;

    private Context context;
    private List<NavigationItem> items;

    private String username;
    private String avatar;

    NavigationAdapter(Context context, List<NavigationItem> items) {
      this.context = context;
      this.items = items;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public void setAvatar(String avatar) {
      this.avatar = avatar;
    }

    @Override public int getCount() {
      return 1 + items.size();
    }

    @Override public NavigationItem getItem(int position) {
      return items.get(position - 1);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean areAllItemsEnabled() {
      return false;
    }

    @Override public boolean isEnabled(int position) {
      if (position == 0) {
        return false;
      }

      return getItem(position) instanceof MenuItem;
    }

    @Override public int getItemViewType(int position) {
      if (position == 0) {
        return TYPE_HEADER;
      }

      return getItem(position).type;
    }

    @Override public int getViewTypeCount() {
      return 4;
    }

    private void setupCircularOutline(final RemoteImageView imageView) {
      imageView.setOutlineProvider(new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline outline) {
          final int width = view.getWidth();
          final int height = view.getHeight();
          float radius = Math.min(width / 2, height / 2);
          outline.setRoundRect(view.getPaddingStart(), view.getPaddingEnd(),
              width - view.getPaddingEnd(), height - view.getPaddingBottom(), radius);
          outline.setAlpha(imageView.getFraction());
        }
      });
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (position == 0) {
        if (v == null) {
          v = LayoutInflater.from(context)
              .inflate(R.layout.fragment_navigation_header, parent, false);
          v.setTag(R.id.username, v.findViewById(R.id.username));
        }

        TextView username = (TextView) v.getTag(R.id.username);

        if (TraktLinkSettings.isLinked(requireContext())) {
          username.setText(this.username);
        } else {
          username.setText(R.string.not_logged_in);
        }

        return v;
      }

      NavigationItem item = getItem(position);

      if (item.type == TYPE_CATEGORY) {
        if (v == null) {
          v = LayoutInflater.from(context).inflate(R.layout.navigation_category, parent, false);
        }

        CategoryItem categoryItem = (CategoryItem) item;

        ((TextView) v).setText(categoryItem.title);
      } else if (item.type == TYPE_ITEM) {
        if (v == null) {
          v = LayoutInflater.from(context).inflate(R.layout.navigation_item, parent, false);
        }

        MenuItem menuItem = (MenuItem) item;

        ((TextView) v).setText(menuItem.title);
        VectorDrawableCompat d =
            VectorDrawableCompat.create(getResources(), menuItem.iconRes, null);
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(((TextView) v), d, null,
            null, null);
      } else {
        v = LayoutInflater.from(context).inflate(R.layout.navigation_divider, parent, false);
      }

      return v;
    }
  }
}
