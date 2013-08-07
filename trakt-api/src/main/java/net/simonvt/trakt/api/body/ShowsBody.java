package net.simonvt.trakt.api.body;

import com.google.gson.annotations.SerializedName;

public class ShowsBody {

  private static final String TAG = "ShowsBody";

  private Show[] shows;

  public ShowsBody(Integer... tvdbIds) {
    final int length = tvdbIds.length;
    shows = new Show[length];
    for (int i = 0; i < length; i++) {
      shows[i] = new Show(tvdbIds[i]);
    }
  }

  public static class Show {

    @SerializedName("tvdb_id") int tvdbId;

    public Show(int tvdbId) {
      this.tvdbId = tvdbId;
    }
  }
}
