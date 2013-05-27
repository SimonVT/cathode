package net.simonvt.trakt.api.body;

public class CreateAccountBody {

    private String username;

    private String password;

    private String email;

    public CreateAccountBody(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
