package net.simonvt.trakt.api.service;

import java.util.List;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.TvEntity;
import net.simonvt.trakt.api.entity.TvShow;
import retrofit.http.GET;
import retrofit.http.Path;

public interface SearchService {

  @GET("/search/episodes.json/{apikey}/{query}")
  List<TvEntity> episodes(@Path("query") String query);

  @GET("/search/movies.json/{apikey}/{query}") List<Movie> movies(@Path("query") String query);

  @GET("/search/people.json/{apikey}/{query}") List<Person> people(@Path("query") String query);

  @GET("/search/shows.json/{apikey}/{query}") List<TvShow> shows(@Path("query") String query);

  //@GET("/search/users.json/{apikey}/{query}")
  //List<UserProfile> users(@Path("query") String query);
}
