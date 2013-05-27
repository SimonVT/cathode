package net.simonvt.trakt.event;

public class LoginEvent {

    private String mUsername;

    private String mPassword;

    public LoginEvent(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }
}
