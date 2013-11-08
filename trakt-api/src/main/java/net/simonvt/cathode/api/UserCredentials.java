package net.simonvt.cathode.api;

public class UserCredentials {

  interface OnCredentialsChangedListener {

    void onCredentialsChanged(String username, String password);
  }

  private String username;

  private String password;

  OnCredentialsChangedListener listener;

  public UserCredentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  void setListener(OnCredentialsChangedListener listener) {
    this.listener = listener;
  }

  public void setCredentials(String username, String password) {
    this.username = username;
    this.password = password;

    if (listener != null) listener.onCredentialsChanged(username, password);
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
