package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.Status;

public class UserSettings extends Response {

  private Profile profile;

  private Account account;

  private Viewing viewing;

  private Connections connections;

  private SharingText sharingText;

  public Profile getProfile() {
    return profile;
  }

  public Account getAccount() {
    return account;
  }

  public Viewing getViewing() {
    return viewing;
  }

  public Connections getConnections() {
    return connections;
  }

  public SharingText getSharingText() {
    return sharingText;
  }
}
