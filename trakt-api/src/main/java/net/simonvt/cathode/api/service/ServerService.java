package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.entity.ServerTime;
import retrofit.http.GET;

public interface ServerService {

  @GET("/server/time.json/{apikey}") ServerTime time();
}
