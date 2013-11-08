package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;

public class NavigationFragment extends AbsAdapterFragment {

  private static final String STATE_SELECTED_ID =
      "net.simonvt.cathode.ui.fragment.NavigationFragment.selectedId";

  public interface OnMenuClickListener {

    void onMenuItemClicked(int id);
  }

  private List<NavigationItem> menuItems = new ArrayList<NavigationItem>();

  {
    menuItems.add(new NavigationItem(R.string.navigation_title_shows));
    menuItems.add(new MenuItem(R.id.menu_shows_upcoming, R.string.navigation_shows_upcoming, 0));
    menuItems.add(new MenuItem(R.id.menu_shows_watched, R.string.navigation_shows_watched, 0));
    menuItems.add(
        new MenuItem(R.id.menu_shows_collection, R.string.navigation_shows_collection, 0));
    menuItems.add(new MenuItem(R.id.menu_shows_watchlist, R.string.navigation_shows_watchlist, 0));
    menuItems.add(
        new MenuItem(R.id.menu_episodes_watchlist, R.string.navigation_episodes_watchlist, 0));
    menuItems.add(new MenuItem(R.id.menu_shows_trending, R.string.navigation_shows_trending, 0));
    menuItems.add(
        new MenuItem(R.id.menu_shows_recommendations, R.string.navigation_shows_recommendations,
            0));

    menuItems.add(new NavigationItem(R.string.navigation_title_movies));
    menuItems.add(new MenuItem(R.id.menu_movies_watched, R.string.navigation_movies_watched, 0));
    menuItems.add(
        new MenuItem(R.id.menu_movies_collection, R.string.navigation_movies_collection, 0));
    menuItems.add(
        new MenuItem(R.id.menu_movies_watchlist, R.string.navigation_movies_watchlist, 0));
    menuItems.add(new MenuItem(R.id.menu_movies_trending, R.string.navigation_movies_trending, 0));
    menuItems.add(
        new MenuItem(R.id.menu_movies_recommendations, R.string.navigation_movies_recommendations,
            0));
  }

  private OnMenuClickListener listener;

  private int selectedPosition = 1;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (OnMenuClickListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);

    if (inState != null) {
      selectedPosition = inState.getInt(STATE_SELECTED_ID);
    }

    setAdapter(new NavigationAdapter(getActivity(), menuItems));
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_SELECTED_ID, selectedPosition);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_navigation, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    getAdapterView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    getAdapterView().setItemChecked(selectedPosition, true);
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    NavigationItem item = (NavigationItem) getAdapter().getItem(position);
    listener.onMenuItemClicked(item.id);

    selectedPosition = position;
    getAdapterView().setItemChecked(selectedPosition, true);
  }

  private static class NavigationItem {

    int id = -1;

    int title;

    NavigationItem(int title) {
      this.title = title;
    }

    NavigationItem(int id, int title) {
      this.title = title;
      this.id = id;
    }

    protected boolean isCategory() {
      return true;
    }
  }

  private static class MenuItem extends NavigationItem {

    int iconRes;

    MenuItem(int id, int title, int iconRes) {
      super(id, title);
      this.iconRes = iconRes;
    }

    @Override protected boolean isCategory() {
      return false;
    }
  }

  private class NavigationAdapter extends BaseAdapter {

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_ITEM = 1;

    private Context context;
    private List<NavigationItem> items;

    NavigationAdapter(Context context, List<NavigationItem> items) {
      this.context = context;
      this.items = items;
    }

    @Override public int getCount() {
      return items.size();
    }

    @Override public Object getItem(int position) {
      return items.get(position);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean areAllItemsEnabled() {
      return false;
    }

    @Override public boolean isEnabled(int position) {
      return getItem(position) instanceof MenuItem;
    }

    @Override public int getItemViewType(int position) {
      return (getItem(position) instanceof MenuItem) ? TYPE_ITEM : TYPE_CATEGORY;
    }

    @Override public int getViewTypeCount() {
      return 2;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      TextView v = (TextView) convertView;
      NavigationItem item = (NavigationItem) getItem(position);

      if (item.isCategory()) {
        if (v == null) {
          v = (TextView) LayoutInflater.from(context)
              .inflate(R.layout.menu_home_category, parent, false);
        }

        v.setText(item.title);
      } else {
        if (v == null) {
          v = (TextView) LayoutInflater.from(context)
              .inflate(R.layout.menu_home_item, parent, false);
        }

        MenuItem menuItem = (MenuItem) item;

        v.setText(menuItem.title);
        v.setCompoundDrawablesWithIntrinsicBounds(menuItem.iconRes, 0, 0, 0);
      }

      v.setTag(R.id.mdActiveViewPosition, position);

      return v;
    }
  }
}
