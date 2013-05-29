package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.ui.MoviesNavigationListener;
import net.simonvt.trakt.ui.adapter.MoviesAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public abstract class MoviesFragment extends AbsAdapterFragment<MoviesAdapter>
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private MoviesNavigationListener mNavigationListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mNavigationListener = (MoviesNavigationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MoviesNavigationListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);
        setHasOptionsMenu(true);

        getLoaderManager().initLoader(getLoaderId(), null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_cards, container, false);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(getLoaderId());
        super.onDestroy();
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        mNavigationListener.onDisplayMovie(id);
    }

    void setCursor(Cursor cursor) {
        if (getAdapter() == null) {
            setAdapter(new MoviesAdapter(getActivity(), cursor));
        } else {
            getAdapter().changeCursor(cursor);
        }
    }

    protected abstract int getLoaderId();

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setAdapter(null);
    }
}
