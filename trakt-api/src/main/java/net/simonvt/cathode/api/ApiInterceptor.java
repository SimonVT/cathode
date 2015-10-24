package net.simonvt.cathode.api;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;

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
