package net.simonvt.trakt.api.entity;

import com.google.gson.annotations.SerializedName;

public class Account {

  private String timezone;

  private Boolean use24hr;

  @SerializedName("protected") private Boolean isProtected;

  public String getTimezone() {
    return timezone;
  }

  public Boolean use24hr() {
    return use24hr;
  }

  public Boolean isProtected() {
    return isProtected;
  }
}
