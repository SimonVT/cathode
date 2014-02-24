package net.simonvt.cathode.api.entity;

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
