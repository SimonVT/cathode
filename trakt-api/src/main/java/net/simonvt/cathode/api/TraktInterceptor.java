package net.simonvt.cathode.api;

import android.util.Base64;
import javax.inject.Inject;
import retrofit.RequestInterceptor;

public class TraktInterceptor implements RequestInterceptor {

  private static final String HEADER_AUTHORIZATION = "Authorization";

  private String auth = null;

  private final String apiKey;

  private UserCredentials credentials;

  @Inject public TraktInterceptor(@ApiKey String apiKey, UserCredentials credentials) {
    if (apiKey == null) {
      throw new IllegalArgumentException("apiKey must not be null");
    }
    if (credentials == null) {
      throw new IllegalArgumentException("credentials must not be null");
    }

    this.apiKey = apiKey;
    this.credentials = credentials;
    credentials.setListener(new UserCredentials.OnCredentialsChangedListener() {
      @Override public void onCredentialsChanged(String username, String password) {
        setCredentials(username, password);
      }
    });
    setCredentials(credentials.getUsername(), credentials.getPassword());
  }

  private void setCredentials(String user, String pass) {
    auth = "Basic " + base64encode(user + ":" + pass);
  }

  @Override public void intercept(RequestFacade requestFacade) {
    if (auth != null) {
      requestFacade.addHeader(HEADER_AUTHORIZATION, auth);
    }

    requestFacade.addPathParam("apikey", apiKey);
    String username = credentials.getUsername();
    requestFacade.addPathParam("username", username != null ? username : "");
  }

  private String base64encode(String source) {
    return new String(Base64.encode(source.getBytes(), Base64.NO_WRAP));
  }
}
