package net.simonvt.trakt.api.service;

import net.simonvt.trakt.api.entity.ServerTime;
import retrofit.http.GET;

public interface ServerService {

  @GET("/server/time.json/{apikey}") ServerTime time();
}
