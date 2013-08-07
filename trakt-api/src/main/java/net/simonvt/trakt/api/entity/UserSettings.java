package net.simonvt.trakt.api.entity;

import net.simonvt.trakt.api.enumeration.Status;

public class UserSettings {

  private Status status;

  private String message;

  private Profile profile;

  private Account account;

  private Viewing viewing;

  private Connections connections;

  private SharingText sharingText;
}
