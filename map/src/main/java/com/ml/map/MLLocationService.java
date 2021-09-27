package com.ml.map;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MLLocationService extends Service {
    public MLLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}