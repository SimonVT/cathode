package net.simonvt.trakt.api.service;

import retrofit.RetrofitError;
import retrofit.http.GET;
import retrofit.http.Path;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.UpdatedMovies;

import java.util.List;

public interface MoviesService {

    @GET("/movies/trending.json/{apikey}")
    List<Movie> trending();

    @GET("/movies/updated.json/{apikey}/{since}")
    UpdatedMovies updated(@Path("since") long since);
}
