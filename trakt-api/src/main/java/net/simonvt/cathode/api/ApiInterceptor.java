package net.simonvt.cathode.api;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiInterceptor implements Interceptor {

  public static final String HEADER_API_KEY = "trakt-api-key";

  public static final String HEADER_API_VERSION = "trakt-api-version";

  private TraktSettings settings;

  public ApiInterceptor(TraktSettings settings) {
    this.settings = settings;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request()
        .newBuilder()
        .addHeader(HEADER_API_KEY, settings.getClientId())
        .addHeader(HEADER_API_VERSION, "2")
        .build();
    return chain.proceed(request);
  }
}
