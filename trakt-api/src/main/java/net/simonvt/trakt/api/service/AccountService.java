package net.simonvt.trakt.api.service;

import net.simonvt.trakt.api.body.CreateAccountBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.entity.UserSettings;
import retrofit.RetrofitError;
import retrofit.http.Body;
import retrofit.http.POST;

public interface AccountService {

  @POST("/account/create/{apikey}") TraktResponse create(@Body CreateAccountBody body)
      throws RetrofitError;

  @POST("/account/settings/{apikey}") UserSettings settings() throws RetrofitError;

  @POST("/account/test/{apikey}") TraktResponse test() throws RetrofitError;
}
