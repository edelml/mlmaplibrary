package com.ml.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DbHelper {

    private static final String HAS_ASKED_FOR_CAMERA_KEY          = "has_asked_for_camera";
    private static final String HAS_ASKED_FOR_LOCATION_KEY        = "has_asked_for_location";
    private static final String HAS_ASKED_FOR_AUDIO_RECORDING_KEY = "has_asked_for_audio_recording";
    private static final String HAS_ASKED_FOR_BOOT_KEY            = "has_asked_for_boot";
    private static final String HAS_ASKED_FOR_CALENDAR_KEY        = "has_asked_for_calendar";
    private static final String HAS_ASKED_FOR_CONTACTS_KEY        = "has_asked_for_contacts";
    private static final String HAS_ASKED_FOR_CALLING_KEY         = "has_asked_for_calling";
    private static final String HAS_ASKED_FOR_STORAGE_KEY         = "has_asked_for_storage";
    private static final String HAS_ASKED_FOR_BODY_SENSORS_KEY    = "has_asked_for_body_sensors";
    private static final String HAS_ASKED_FOR_SMS_KEY             = "has_asked_for_sms";
    private static final String HAS_ASKED_FOR_VIBRATE_KEY             = "has_asked_for_vibrate";
    private static final String IS_FIRST_TIME_KEY                 = "is_first_time";

    private static DbHelper          mInstance;
    private final  SharedPreferences preferences;

    public static void init(Context context) {
        mInstance = new DbHelper(context);
    }

    public void saveValue(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }


    public String getValue(String key) {
        return preferences.getString(key, null);
    }

    private DbHelper(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static DbHelper get() {
        return mInstance;
    }

    public boolean isFirstTime(String id) {
        if(preferences.getBoolean(IS_FIRST_TIME_KEY+id, true)){
            preferences.edit()
                    .putBoolean(IS_FIRST_TIME_KEY+id, false)
                    .apply();
            return true;
        }
        return false;
    }

    public void setIsFirstTime(String id, boolean value) {
            preferences.edit()
                    .putBoolean(IS_FIRST_TIME_KEY+id, value)
                    .apply();
    }

    // ==== CAMERA =================================================================================

    public void setCameraPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_CAMERA_KEY, true)
                .apply();
    }

    public boolean isCameraPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_CAMERA_KEY, false);
    }

    // ===== LOCATION ==============================================================================

    public void setLocationPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_LOCATION_KEY, true)
                .apply();
    }

    public boolean isLocationPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_LOCATION_KEY, false);
    }

    // ===== MICROPHONE ============================================================================

    public void setMicrophonePermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_AUDIO_RECORDING_KEY, true)
                .apply();
    }

    // ===== BOOT ============================================================================

    public void setBootPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_BOOT_KEY, true)
                .apply();
    }

    public boolean isAudioPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_AUDIO_RECORDING_KEY, false);
    }

    // ===== CALENDAR ==============================================================================

    public void setCalendarPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_CALENDAR_KEY, true)
                .apply();
    }

    public boolean isCalendarPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_CALENDAR_KEY, false);
    }

    // ===== CONTACTS ==============================================================================

    public void setContactsPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_CONTACTS_KEY, true)
                .apply();
    }

    public boolean isContactsPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_CONTACTS_KEY, false);
    }

    // ===== PHONE ==============================================================================

    public void setPhonePermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_CALLING_KEY, true)
                .apply();
    }

    public boolean isPhonePermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_CALLING_KEY, false);
    }

    // ===== STORAGE ===============================================================================

    public void setStoragePermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_STORAGE_KEY, true)
                .apply();
    }

    public boolean isStoragePermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_STORAGE_KEY, false);
    }

    // ===== BODY SENSORS ==============================================================================

    public void setBodySensorsPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_BODY_SENSORS_KEY, true)
                .apply();
    }

    public boolean isBodySensorsPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_BODY_SENSORS_KEY, false);
    }

    // ===== SMS ==============================================================================

    public void setSmsPermissionsAsked() {
        preferences.edit()
                .putBoolean(HAS_ASKED_FOR_SMS_KEY, true)
                .apply();
    }

    public boolean isSmsPermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_SMS_KEY, false);
    }

    public boolean setVibratePermissionsAsked() {
        return preferences.getBoolean(HAS_ASKED_FOR_VIBRATE_KEY, false);
    }
}
