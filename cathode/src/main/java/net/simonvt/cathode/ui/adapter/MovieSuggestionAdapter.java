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
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieSearchSuggestionsColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieSearchSuggestions;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;

public class MovieSuggestionAdapter extends SuggestionsAdapter {

  public MovieSuggestionAdapter(Context context) {
    super(context);
    new FireAndForget(context, callback).execute();
  }

  private FireAndForget.Callback callback = new FireAndForget.Callback() {
    @Override public void deliverResult(List<Suggestion> queries, List<Suggestion> movies) {
      MovieSuggestionAdapter.this.queries = queries;
      MovieSuggestionAdapter.this.known = movies;
      notifyDataSetChanged();
    }
  };

  private static class FireAndForget extends AsyncTask<Void, Void, FireAndForget.Results> {

    public interface Callback {
      void deliverResult(List<Suggestion> queries, List<Suggestion> movies);
    }

    public static class Results {
      List<Suggestion> queries;
      List<Suggestion> movies;

      private Results(List<Suggestion> queries, List<Suggestion> movies) {
        this.queries = queries;
        this.movies = movies;
      }
    }

    private Context context;

    private WeakReference<Callback> callbackRef;

    private FireAndForget(Context context, Callback callback) {
      this.context = context.getApplicationContext();
      this.callbackRef = new WeakReference<>(callback);
    }

    @Override protected Results doInBackground(Void... params) {
      Cursor previousQueries = context.getContentResolver()
          .query(MovieSearchSuggestions.MOVIE_SUGGESTIONS, null, null, null, null);

      final int queryIndex = previousQueries.getColumnIndex(MovieSearchSuggestionsColumns.QUERY);

      List<Suggestion> queries = new ArrayList<>();
      while (previousQueries.moveToNext()) {
        queries.add(new Suggestion(previousQueries.getString(queryIndex), null, null));
      }
      previousQueries.close();

      Cursor allMovies = context.getContentResolver().query(Movies.MOVIES, new String[] {
          MovieColumns.ID, MovieColumns.TITLE, MovieColumns.OVERVIEW,
      }, MovieColumns.IN_COLLECTION + "=1 OR " + MovieColumns.WATCHED + "=1", null, null);

      final int titleIndex = allMovies.getColumnIndex(MovieColumns.TITLE);
      final int overviewIndex = allMovies.getColumnIndex(MovieColumns.OVERVIEW);
      final int idIndex = allMovies.getColumnIndex(MovieColumns.ID);

      List<Suggestion> movies = new ArrayList<>();
      while (allMovies.moveToNext()) {
        movies.add(
            new Suggestion(allMovies.getString(titleIndex), allMovies.getString(overviewIndex),
                allMovies.getLong(idIndex)));
      }
      allMovies.close();

      return new Results(queries, movies);
    }

    @Override protected void onPostExecute(Results results) {
      Callback callback = callbackRef.get();
      if (callback != null) callback.deliverResult(results.queries, results.movies);
    }
  }
}
