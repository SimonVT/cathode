package net.simonvt.trakt.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CalendarService extends Service {

    private static CalendarSyncAdapter sSyncAdapter;

    @Override
    public IBinder onBind(Intent intent) {
        return getSyncAdapter().getSyncAdapterBinder();
    }

    private CalendarSyncAdapter getSyncAdapter() {
        if (sSyncAdapter == null) {
            sSyncAdapter = new CalendarSyncAdapter(getApplicationContext());
        }
        return sSyncAdapter;
    }
}
