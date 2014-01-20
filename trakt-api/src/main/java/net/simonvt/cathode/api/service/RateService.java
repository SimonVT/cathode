package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.RateBody;
import net.simonvt.cathode.api.entity.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface RateService {

  @POST("/rate/episode/{apikey}") Response episode(@Body RateBody rateBody);

  @POST("/rate/episodes/{apikey}") Response episodes(@Body RateBody rateBody);

  @POST("/rate/movie/{apikey}") Response movie(@Body RateBody rateBody);

  @POST("/rate/movies/{apikey}") Response movies(@Body RateBody rateBody);

  @POST("/rate/show/{apikey}") Response show(@Body RateBody rateBody);

  @POST("/rate/shows/{apikey}") Response shows(@Body RateBody rateBody);
}
