package net.simonvt.cathode.api.entity;

public class CheckinResponse extends Response {

  public static class Timestamps {
    public Long start;
    public Long end;
    public Integer activeFor;
  }

  private int wait;

  private Timestamps timestamps;

  public int getWait() {
    return wait;
  }

  public Timestamps getTimestamps() {
    return timestamps;
  }
}
