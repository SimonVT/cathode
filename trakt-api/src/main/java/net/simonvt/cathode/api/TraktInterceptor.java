package net.simonvt.cathode.api;

import javax.inject.Inject;
import retrofit.RequestInterceptor;

public class TraktInterceptor implements RequestInterceptor {

  public static final String HEADER_AUTHORIZATION = "Authorization";

  public static final String HEADER_API_KEY = "trakt-api-key";

  public static final String HEADER_API_VERSION = "trakt-api-version";

  public static final String PATH_USERNAME = "username";

  private String auth = null;

  private UserToken token;

  private String apiKey;

  @Inject public TraktInterceptor(UserToken token, @ApiKey String apiKey) {
    if (token == null) {
      throw new IllegalArgumentException("token must not be null");
    }

    this.token = token;
    token.setListener(new UserToken.OnCredentialsChangedListener() {
      @Override public void onTokenChanged(String token) {
        setToken(token);
      }
    });
    setToken(token.getToken());

    this.apiKey = apiKey;
  }

  private void setToken(String token) {
    if (token == null) {
      auth = null;
    } else {
      auth = "Bearer " + token;
    }
  }

  @Override public void intercept(RequestFacade requestFacade) {
    if (auth != null) {
      requestFacade.addHeader(HEADER_AUTHORIZATION, auth);
    }

    requestFacade.addHeader(HEADER_API_KEY, apiKey);
    requestFacade.addHeader(HEADER_API_VERSION, "2");

    requestFacade.addPathParam(PATH_USERNAME, "me");
  }
}
