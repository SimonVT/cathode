package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.RateBody;
import net.simonvt.cathode.api.entity.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface RateService {

  @POST("/rate/episode/{apikey}") Response rateEpisode(@Body RateBody rateBody);

  //    rate/episodes
  //            POST

  @POST("/rate/movie/{apikey}") Response rateMovie(@Body RateBody rateBody);

  //    rate/movies
  //            POST

  @POST("/rate/show/{apikey}") Response rateShow(@Body RateBody rateBody);

  //    rate/shows
  //            POST
}
