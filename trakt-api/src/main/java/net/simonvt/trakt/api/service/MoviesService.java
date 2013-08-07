package net.simonvt.trakt.api.service;

import java.util.List;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.UpdatedMovies;
import retrofit.http.GET;
import retrofit.http.Path;

public interface MoviesService {

  @GET("/movies/trending.json/{apikey}") List<Movie> trending();

  @GET("/movies/updated.json/{apikey}/{since}") UpdatedMovies updated(@Path("since") long since);
}
