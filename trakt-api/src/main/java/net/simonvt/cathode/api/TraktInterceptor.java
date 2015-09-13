package net.simonvt.cathode.api;

import javax.inject.Inject;
import retrofit.RequestInterceptor;

public class TraktInterceptor implements RequestInterceptor {

  public static final String HEADER_AUTHORIZATION = "Authorization";

  public static final String HEADER_API_KEY = "trakt-api-key";

  public static final String HEADER_API_VERSION = "trakt-api-version";

  public static final String PATH_USERNAME = "username";

  private TraktSettings settings;

  @Inject public TraktInterceptor(TraktSettings settings) {
    this.settings = settings;
  }

  @Override public void intercept(RequestFacade requestFacade) {
    if (settings.getAccessToken() != null) {
      requestFacade.addHeader(HEADER_AUTHORIZATION, "Bearer " + settings.getAccessToken());
    }

    requestFacade.addHeader(HEADER_API_KEY, settings.getClientId());
    requestFacade.addHeader(HEADER_API_VERSION, "2");

    requestFacade.addPathParam(PATH_USERNAME, "me");
  }
}
