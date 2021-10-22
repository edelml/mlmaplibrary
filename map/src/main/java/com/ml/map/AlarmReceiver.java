package com.ml.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;


public class AlarmReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            LOG.debug("Alarm received");

            EventBus.getDefault().post(new CommandEvents.AutoSend(null));

            Intent serviceIntent = new Intent(GoblobLocationManager.getInstance().getApplicationContext(), GoblobLocationService.class);
            context.startService(serviceIntent);
        } catch (Exception ex) {
            LOG.error("AlarmReceiver", ex);
        }
    }
}