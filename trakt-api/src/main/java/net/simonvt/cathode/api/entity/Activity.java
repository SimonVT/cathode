package net.simonvt.cathode.api.entity;

import java.util.List;

public class Activity {

  private Timestamp timestamp;

  private List<ActivityItem> activity;

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public List<ActivityItem> getActivity() {
    return activity;
  }
}
