package net.simonvt.trakt.api;

import retrofit.RequestInterceptor;

import android.util.Base64;

import javax.inject.Inject;

public class TraktInterceptor implements RequestInterceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private String mAuth = null;

    private final String mApiKey;

    private UserCredentials mCredentials;

    @Inject
    public TraktInterceptor(@ApiKey String apiKey, UserCredentials credentials) {
        if (apiKey == null) {
            throw new IllegalArgumentException("apiKey must not be null");
        }
        if (credentials == null) {
            throw new IllegalArgumentException("credentials must not be null");
        }

        mApiKey = apiKey;
        mCredentials = credentials;
        credentials.setListener(new UserCredentials.OnCredentialsChangedListener() {
            @Override
            public void onCredentialsChanged(String username, String password) {
                setCredentials(username, password);
            }
        });
        setCredentials(credentials.getUsername(), credentials.getPassword());
    }

    private void setCredentials(String user, String pass) {
        mAuth = "Basic " + base64encode(user + ":" + pass);
    }

    @Override
    public void intercept(RequestFacade requestFacade) {
        if (mAuth != null) {
            requestFacade.addHeader(HEADER_AUTHORIZATION, mAuth);
        }

        requestFacade.addPathParam("apikey", mApiKey);
        requestFacade.addPathParam("username", mCredentials.getUsername());
    }

    private String base64encode(String source) {
        return new String(Base64.encode(source.getBytes(), Base64.DEFAULT));
    }
}
