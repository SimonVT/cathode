package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.NetworkBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.entity.UserProfile;
import retrofit.http.Body;
import retrofit.http.POST;

public interface NetworkService {

  @POST("/network/approve/{apikey}") Response approve(@Body NetworkBody body);

  @POST("/network/deny/{apikey}") Response deny(@Body NetworkBody body);

  @POST("/network/follow/{apikey}") Response follow(@Body NetworkBody body);

  @POST("/network/requests/{apikey}") List<UserProfile> requests();

  @POST("/network/unfollow/{apikey}") Response unfollow(@Body NetworkBody body);
}
