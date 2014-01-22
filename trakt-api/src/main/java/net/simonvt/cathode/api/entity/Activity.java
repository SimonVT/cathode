package net.simonvt.cathode.api.entity;

import java.util.List;

public class Activity {

  private Timestamp timestamps;

  private List<ActivityItem> activity;

  public Timestamp getTimestamps() {
    return timestamps;
  }

  public List<ActivityItem> getActivity() {
    return activity;
  }
}
