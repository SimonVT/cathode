package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.Status;

public class Response {

  private Status status;

  private String message;

  private String error;

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getError() {
    return error;
  }
}
