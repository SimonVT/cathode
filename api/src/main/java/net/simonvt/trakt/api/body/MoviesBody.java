package net.simonvt.trakt.api.body;

import com.google.gson.annotations.SerializedName;

public class MoviesBody {

    private Movie[] movies;

    public MoviesBody(Integer... tmdbIds) {
        final int length = tmdbIds.length;
        movies = new Movie[length];
        for (int i = 0; i < length; i++) {
            movies[i] = new Movie(tmdbIds[i]);
        }
    }

    public static class Movie {

        @SerializedName("tmdb_id") int tmdbId;

        public Movie(int tmdbId) {
            this.tmdbId = tmdbId;
        }
    }
}
