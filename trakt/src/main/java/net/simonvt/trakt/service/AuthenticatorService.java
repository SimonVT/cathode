package net.simonvt.trakt.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new AccountAuthenticator(getApplicationContext()).getIBinder();
    }
}
