package net.simonvt.cathode.api;

public class UserToken {

  interface OnCredentialsChangedListener {

    void onTokenChanged(String token);
  }

  private String token;

  OnCredentialsChangedListener listener;

  public UserToken(String token) {
    this.token = token;
  }

  void setListener(OnCredentialsChangedListener listener) {
    this.listener = listener;
  }

  public void setToken(String token) {
    this.token = token;

    if (listener != null) listener.onTokenChanged(token);
  }

  public String getToken() {
    return token;
  }
}
