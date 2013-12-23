/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.provider.CathodeContract;

public class ShowSuggestionAdapter extends SuggestionsAdapter {

  public ShowSuggestionAdapter(Context context) {
    super(context);
    new FireAndForget(context, callback).execute();
  }

  private FireAndForget.Callback callback = new FireAndForget.Callback() {
    @Override public void deliverResult(List<Suggestion> queries, List<Suggestion> shows) {
      ShowSuggestionAdapter.this.queries = queries;
      ShowSuggestionAdapter.this.known = shows;
      notifyDataSetChanged();
    }
  };

  private static class FireAndForget extends AsyncTask<Void, Void, FireAndForget.Results> {

    public interface Callback {
      void deliverResult(List<Suggestion> queries, List<Suggestion> shows);
    }

    public static class Results {
      List<Suggestion> queries;
      List<Suggestion> shows;

      private Results(List<Suggestion> queries, List<Suggestion> shows) {
        this.queries = queries;
        this.shows = shows;
      }
    }

    private Context context;

    private WeakReference<Callback> callbackRef;

    private FireAndForget(Context context, Callback callback) {
      this.context = context.getApplicationContext();
      this.callbackRef = new WeakReference<Callback>(callback);
    }

    @Override protected Results doInBackground(Void... params) {
      Cursor previousQueries = context.getContentResolver()
          .query(CathodeContract.SearchSuggestions.SHOW_URI, null, null, null, null);

      final int queryIndex =
          previousQueries.getColumnIndex(CathodeContract.SearchSuggestions.QUERY);

      List<Suggestion> queries = new ArrayList<Suggestion>();
      while (previousQueries.moveToNext()) {
        queries.add(new Suggestion(previousQueries.getString(queryIndex), null));
      }

      Cursor allShows =
          context.getContentResolver().query(CathodeContract.Shows.CONTENT_URI, new String[] {
              CathodeContract.Shows._ID, CathodeContract.Shows.TITLE,
          }, CathodeContract.Shows.IN_COLLECTION_COUNT
              + ">0 OR "
              + CathodeContract.Shows.WATCHED_COUNT
              + ">0", null, null);

      final int titleIndex = allShows.getColumnIndex(CathodeContract.Shows.TITLE);
      final int idIndex = allShows.getColumnIndex(CathodeContract.Shows._ID);

      List<Suggestion> shows = new ArrayList<Suggestion>();
      while (allShows.moveToNext()) {
        shows.add(new Suggestion(allShows.getString(titleIndex), allShows.getLong(idIndex)));
      }

      return new Results(queries, shows);
    }

    @Override protected void onPostExecute(Results results) {
      Callback callback = callbackRef.get();
      if (callback != null) callback.deliverResult(results.queries, results.shows);
    }
  }
}
