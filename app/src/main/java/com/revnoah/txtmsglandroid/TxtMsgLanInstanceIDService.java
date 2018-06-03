package com.revnoah.txtmsglandroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TxtMsgLanInstanceIDService extends Service {
    public TxtMsgLanInstanceIDService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
