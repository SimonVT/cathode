package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.DismissBody;
import net.simonvt.cathode.api.body.RecommendationsBody;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.entity.TvShow;
import retrofit.http.Body;
import retrofit.http.POST;

public interface RecommendationsService {

  @POST("/recommendations/movies/{apikey}") List<Movie> movies();

  @POST("/recommendations/movies/{apikey}") List<Movie> movies(@Body RecommendationsBody body);

  @POST("/recommendations/movies/dismiss/{apikey}") Response dismissMovie(@Body DismissBody body);

  @POST("/recommendations/shows/{apikey}") List<TvShow> shows();

  @POST("/recommendations/shows/{apikey}") List<TvShow> shows(@Body RecommendationsBody body);

  @POST("/recommendations/shows/dismiss/{apikey}") Response dismissShow(@Body DismissBody body);
}
