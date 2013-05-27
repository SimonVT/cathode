package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class NavigationFragment extends AbsAdapterFragment {

    private static final String STATE_SELECTED_ID = "net.simonvt.trakt.ui.fragment.NavigationFragment.selectedId";

    public interface OnMenuClickListener {

        void onMenuItemClicked(int id);

        void onActiveViewChanged(int position, View activeView);
    }

    private List<NavigationItem> mMenuItems = new ArrayList<NavigationItem>();

    {
        mMenuItems.add(new NavigationItem(R.string.navigation_title_shows));
        mMenuItems.add(new MenuItem(R.id.menu_shows_upcoming, R.string.navigation_shows_upcoming, 0));
        mMenuItems.add(new MenuItem(R.id.menu_shows_watched, R.string.navigation_shows_watched, 0));
        mMenuItems.add(new MenuItem(R.id.menu_shows_collection, R.string.navigation_shows_collection, 0));
        mMenuItems.add(new MenuItem(R.id.menu_shows_watchlist, R.string.navigation_shows_watchlist, 0));
        mMenuItems.add(new MenuItem(R.id.menu_episodes_watchlist, R.string.navigation_episodes_watchlist, 0));
        // mMenuItems.add(new MenuItem(R.id.menu_shows_ratings, R.string.navigation_shows_ratings, 0));
        // mMenuItems.add(new MenuItem(R.id.menu_shows_charts, R.string.navigation_shows_charts, 0));
        // mMenuItems.add(new NavigationItem(R.string.navigation_title_movies));
        // mMenuItems.add(new MenuItem(R.id.menu_movies_library, R.string.navigation_movies_library, 0));
        // mMenuItems.add(new MenuItem(R.id.menu_movies_watchlist, R.string.navigation_movies_watchlist, 0));
        // mMenuItems.add(new MenuItem(R.id.menu_movies_ratings, R.string.navigation_movies_ratings, 0));
        // mMenuItems.add(new MenuItem(R.id.menu_movies_charts, R.string.navigation_movies_charts, 0));
    }

    private OnMenuClickListener mListener;

    private int mSelectedPosition = 2;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnMenuClickListener) activity;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        if (state != null) {
            mSelectedPosition = state.getInt(STATE_SELECTED_ID);
        }

        setAdapter(new NavigationAdapter(getActivity(), mMenuItems));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_ID, mSelectedPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        NavigationItem item = (NavigationItem) getAdapter().getItem(position);
        mListener.onMenuItemClicked(item.mId);

        mSelectedPosition = position;
        mListener.onActiveViewChanged(position, v);
    }

    private static class NavigationItem {

        int mId = -1;

        int mTitle;

        NavigationItem(int title) {
            mTitle = title;
        }

        NavigationItem(int id, int title) {
            mTitle = title;
            mId = id;
        }

        protected boolean isCategory() {
            return true;
        }
    }

    private static class MenuItem extends NavigationItem {

        int mIconRes;

        MenuItem(int id, int title, int iconRes) {
            super(id, title);
            mIconRes = iconRes;
        }

        @Override
        protected boolean isCategory() {
            return false;
        }
    }

    private class NavigationAdapter extends BaseAdapter {

        private static final int TYPE_CATEGORY = 0;
        private static final int TYPE_ITEM = 1;

        private Context mContext;
        private List<NavigationItem> mItems;

        NavigationAdapter(Context context, List<NavigationItem> items) {
            mContext = context;
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof MenuItem;
        }

        @Override
        public int getItemViewType(int position) {
            return (getItem(position) instanceof MenuItem) ? TYPE_ITEM : TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) convertView;
            NavigationItem item = (NavigationItem) getItem(position);

            if (item.isCategory()) {
                if (v == null) {
                    v = (TextView) LayoutInflater.from(mContext).inflate(R.layout.menu_home_category, parent, false);
                }

                v.setText(item.mTitle);

            } else {
                if (v == null) {
                    v = (TextView) LayoutInflater.from(mContext).inflate(R.layout.menu_home_item, parent, false);
                }

                MenuItem menuItem = (MenuItem) item;

                v.setText(menuItem.mTitle);
                v.setCompoundDrawablesWithIntrinsicBounds(menuItem.mIconRes, 0, 0, 0);
            }

            if (position == mSelectedPosition) {
                mListener.onActiveViewChanged(position, v);
            }

            v.setTag(R.id.mdActiveViewPosition, position);

            return v;
        }
    }
}
