package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class ShowBody {

  @SerializedName("tvdb_id") private int tvdbId;

  public ShowBody(int tvdbId) {
    this.tvdbId = tvdbId;
  }
}
