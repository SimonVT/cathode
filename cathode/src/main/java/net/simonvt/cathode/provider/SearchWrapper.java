package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public final class SearchWrapper {

  private SearchWrapper() {
  }

  public static void insertShowQuery(ContentResolver resolver, String query) {
    Cursor c = resolver.query(CathodeContract.SearchSuggestions.SHOW_URI, null,
        CathodeContract.SearchSuggestions.QUERY + "=?", new String[] {
        query,
    }, null);
    if (c.getCount() == 0) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.SearchSuggestions.QUERY, query);
      resolver.insert(CathodeContract.SearchSuggestions.SHOW_URI, cv);
    }
  }

  public static void insertMovieQuery(ContentResolver resolver, String query) {
    Cursor c = resolver.query(CathodeContract.SearchSuggestions.MOVIE_URI, null,
        CathodeContract.SearchSuggestions.QUERY + "=?", new String[] {
        query,
    }, null);
    if (c.getCount() == 0) {
      ContentValues cv = new ContentValues();
      cv.put(CathodeContract.SearchSuggestions.QUERY, query);
      resolver.insert(CathodeContract.SearchSuggestions.MOVIE_URI, cv);
    }
  }
}
