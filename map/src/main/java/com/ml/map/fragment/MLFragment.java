package com.ml.map.fragment;

import android.location.Location;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.ml.map.EventBusHook;
import com.ml.map.ServiceEvents;
import com.ml.map.permissionsmanager.PermissionsManager;
import com.ml.map.permissionsmanager.PermissionsResult;

import de.greenrobot.event.EventBus;

public abstract class MLFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionsManager.get().requestGoblobMapPermission().observe(this, new Observer<PermissionsResult>() {
            @Override
            public void onChanged(PermissionsResult permissionsResult) {
                if(permissionsResult.isGranted()){
                    init();
                }
            }
        });
        registerEventBus();
    }

    protected abstract void init();

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.SatellitesVisible satellitesVisible){
        setSatelliteCount(satellitesVisible.satelliteCount);
    }

    protected abstract void setSatelliteCount(int satelliteCount);

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        onWaitingForLocation(waitingForLocation.waiting);
    }

    protected abstract void onWaitingForLocation(boolean waiting);

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        setLoggingStatus(loggingStatus.loggingStarted);
    }

    protected abstract void setLoggingStatus(boolean loggingStarted);

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationUpdate){
        displayLocationInfo(locationUpdate.location);
    }

    protected abstract void displayLocationInfo(Location location);
}
