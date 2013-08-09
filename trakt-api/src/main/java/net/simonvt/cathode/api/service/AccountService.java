package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.CreateAccountBody;
import net.simonvt.cathode.api.entity.TraktResponse;
import net.simonvt.cathode.api.entity.UserSettings;
import retrofit.RetrofitError;
import retrofit.http.Body;
import retrofit.http.POST;

public interface AccountService {

  @POST("/account/create/{apikey}") TraktResponse create(@Body CreateAccountBody body)
      throws RetrofitError;

  @POST("/account/settings/{apikey}") UserSettings settings() throws RetrofitError;

  @POST("/account/test/{apikey}") TraktResponse test() throws RetrofitError;
}
