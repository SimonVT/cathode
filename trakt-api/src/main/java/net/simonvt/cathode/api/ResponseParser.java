package net.simonvt.cathode.api;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Response;
import retrofit.RetrofitError;

public class ResponseParser {

  private static final String TAG = "ResponseParser";

  @Inject @Trakt Gson mGson;

  /**
   * Attempts to parse a RetrofitError into a {@link net.simonvt.cathode.api.entity.Response}.
   *
   * @param e The error
   * @return Return a {@link net.simonvt.cathode.api.entity.Response} if parsing was successful, null if not
   */
  public Response tryParse(RetrofitError e) {
    try {
      InputStream is = e.getResponse().getBody().in();
      return mGson.fromJson(new JsonReader(new InputStreamReader(is)), Response.class);
    } catch (Throwable t) {
      // Ignore
    }

    return null;
  }
}
