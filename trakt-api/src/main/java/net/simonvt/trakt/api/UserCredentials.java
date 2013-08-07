package net.simonvt.trakt.api;

public class UserCredentials {

  interface OnCredentialsChangedListener {

    void onCredentialsChanged(String username, String password);
  }

  private String mUsername;

  private String mPassword;

  OnCredentialsChangedListener mListener;

  public UserCredentials(String username, String password) {
    mUsername = username;
    mPassword = password;
  }

  void setListener(OnCredentialsChangedListener listener) {
    mListener = listener;
  }

  public void setCredentials(String username, String password) {
    mUsername = username;
    mPassword = password;

    if (mListener != null) mListener.onCredentialsChanged(username, password);
  }

  public String getUsername() {
    return mUsername;
  }

  public String getPassword() {
    return mPassword;
  }
}
