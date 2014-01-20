package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.entity.TvEntity;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.entity.UserProfile;
import retrofit.http.GET;
import retrofit.http.Query;

public interface SearchService {

  @GET("/search/episodes.json/{apikey}") List<TvEntity> episodes(@Query("query") String query);

  @GET("/search/movies.json/{apikey}") List<Movie> movies(@Query("query") String query);

  @GET("/search/people.json/{apikey}") List<Person> people(@Query("query") String query);

  @GET("/search/shows.json/{apikey}") List<TvShow> shows(@Query("query") String query);

  @GET("/search/users.json/{apikey}") List<UserProfile> users(@Query("query") String query);

  @GET("/search/users.json/{apikey}")
  List<UserProfile> users(@Query("query") String query, @Query("limit") int limit);
}
