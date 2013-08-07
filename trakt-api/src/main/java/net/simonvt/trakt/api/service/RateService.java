package net.simonvt.trakt.api.service;

import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import retrofit.http.Body;
import retrofit.http.POST;

public interface RateService {

  @POST("/rate/episode/{apikey}") TraktResponse rateEpisode(@Body RateBody rateBody);

  //    rate/episodes
  //            POST

  @POST("/rate/movie/{apikey}") TraktResponse rateMovie(@Body RateBody rateBody);

  //    rate/movies
  //            POST

  @POST("/rate/show/{apikey}") TraktResponse rateShow(@Body RateBody rateBody);

  //    rate/shows
  //            POST
}
