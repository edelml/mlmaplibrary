package com.ml.map;

import android.app.Application;

public class SApplication extends Application {
    private static SApplication instance;

    public SApplication() {
        instance = this;
    }

    /**
     * Returns a singleton instance of this class
     */
    public static SApplication getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        GoblobLocationManager.getInstance().init(this);
    }
}