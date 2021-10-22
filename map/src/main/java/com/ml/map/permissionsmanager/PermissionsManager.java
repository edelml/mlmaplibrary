package com.ml.map.permissionsmanager;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ADD_VOICEMAIL;
import static android.Manifest.permission.BODY_SENSORS;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.PROCESS_OUTGOING_CALLS;
import static android.Manifest.permission.READ_CALENDAR;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.RECEIVE_BOOT_COMPLETED;
import static android.Manifest.permission.RECEIVE_MMS;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.RECEIVE_WAP_PUSH;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.USE_SIP;
import static android.Manifest.permission.VIBRATE;
import static android.Manifest.permission.WRITE_CALENDAR;
import static android.Manifest.permission.WRITE_CALL_LOG;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_BODY_SENSOR_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_BOOT_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_CALENDAR_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_CAMERA_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_CONTACTS_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_LOCATION_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_MICROPHONE_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_PHONE_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_SMS_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_STORAGE_PERMISSION;
import static com.ml.map.permissionsmanager.PermissionsManager.Permission.REQUEST_VIBRATE_PERMISSION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.util.SparseArray;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ml.map.BuildConfig;
import com.ml.map.DbHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PermissionsManager is the point of entry to finding about and requesting permissions.
 * <p>
 * This class helper methods for the following permissions:
 * <ul>
 * <li>Camera</li>
 * <li>LocationParameters</li>
 * <li>Microphone</li>
 * <li>Calendar</li>
 * <li>Contacts</li>
 * <li>Storage</li>
 * <li>Phone Call</li>
 * <li>Body Sensors</li>
 * <li>SMS</li>
 * </ul>
 * <p>
 * These are the protected permissions in Android as defined in
 * <a href="https://developer.android.com/guide/topics/permissions/requesting.html#perm-groups">Android Permission Groups</a>
 * <p>
 * The method {@link #intentToAppSettings(Activity)} can be used to open the app's settings
 * to turn on a permission if the user checked "Never ask again".
 */
public class PermissionsManager {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_CAMERA_PERMISSION,
            REQUEST_LOCATION_PERMISSION,
            REQUEST_MICROPHONE_PERMISSION,
            REQUEST_CALENDAR_PERMISSION,
            REQUEST_CONTACTS_PERMISSION,
            REQUEST_STORAGE_PERMISSION,
            REQUEST_PHONE_PERMISSION,
            REQUEST_BODY_SENSOR_PERMISSION,
            REQUEST_SMS_PERMISSION,
            REQUEST_VIBRATE_PERMISSION,
            REQUEST_BOOT_PERMISSION,
    })
    public @interface Permission {
        int REQUEST_CAMERA_PERMISSION      = 0;
        int REQUEST_LOCATION_PERMISSION    = 1;
        int REQUEST_MICROPHONE_PERMISSION  = 2;
        int REQUEST_CALENDAR_PERMISSION    = 3;
        int REQUEST_CONTACTS_PERMISSION    = 4;
        int REQUEST_STORAGE_PERMISSION     = 5;
        int REQUEST_PHONE_PERMISSION       = 6;
        int REQUEST_BODY_SENSOR_PERMISSION = 7;
        int REQUEST_SMS_PERMISSION         = 8;
        int REQUEST_BOOT_PERMISSION         = 9;
        int REQUEST_VIBRATE_PERMISSION         = 10;
    }

    @SuppressLint("StaticFieldLeak")
    private static PermissionsManager    mInstance;
    private final  Context               mContext;

    private SparseArray<MutableLiveData<PermissionsResult>> producersMap = new SparseArray<>();

    /**
     * Instantiate the PermissionsManager. This should be done in the Application subclass or
     * another singleton.
     *
     * @param context this should be the Application Context
     */
    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new PermissionsManager(context);
        }
    }

    private PermissionsManager(Context context) {
        mContext = context;
    }

    /**
     * Entry point for the PermissionsManager. Gets a singleton that was instantiate in
     * the {@link #init} method.
     *
     * @return PermissionsManager instance
     */
    public static PermissionsManager get() {
        return mInstance;
    }

    // ==== CAMERA PERMISSION ======================================================================

    /**
     * @return if we have the camera permission
     */
    public boolean isCameraGranted() {
        return ContextCompat.checkSelfPermission(mContext, CAMERA)
                == PERMISSION_GRANTED;
    }

    private List<String> getCameraPermissions() {
        return Collections.singletonList(CAMERA);
    }

    /**
     * Request the CAMERA permission.
     * @return Observable that will kick off CAMERA permission.
     */
    public LiveData<PermissionsResult> requestCameraPermission() {
        if (isMicrophoneGranted()){
            return requestPermissions(REQUEST_CAMERA_PERMISSION);
        }
        return requestPermissions(REQUEST_CAMERA_PERMISSION, REQUEST_MICROPHONE_PERMISSION);
    }

    /**
     * @return if this app has previously asked for the camera permission.
     */
    public boolean hasAskedForCameraPermission() {
        return DbHelper.get()
                .isCameraPermissionsAsked();
    }

    /**
     * @param fragment calling this method
     * @return If the user has seen the permission before and denied it.
     */
    @VisibleForTesting
    protected boolean shouldShowCameraRationale(@NonNull Fragment fragment) {
        return (!isCameraGranted()
                && shouldShowRequestPermissionRationale(fragment, CAMERA));
    }

    /**
     * @param activity calling this method
     * @return If the user has seen the permission before and denied it.
     */
    private boolean shouldShowCameraRationale(@NonNull Activity activity) {
        return (!isCameraGranted()
                && shouldShowRequestPermissionRationale(activity, CAMERA));
    }

    /**
     * If true the user has checked the "Never ask again" option. We get this by checking two things.
     * Whether we've asked before, checked with {@link #hasAskedForCameraPermission()}, which returns
     * false if the user has never seen the dialog. We also check whether we should request the permission
     * rational for the system. If these two don't match up, the user has selected "Never ask again".
     * <p>
     * <b>Note: if we have the camera permission, and we call this method, it will return true.
     * This is intentional as we don't want to ask for permissions once we have them. If you
     * do that, you will loose the permission and dialog will come up again.</b>
     * <p>
     * <b>Another note: if the user selected "Never ask again", then they give you permissions in
     * the app settings page, and then remove them in the same page. This method will return true.
     * Even though at that point you can ask for permissions. I have not been able to figure out a
     * way around this.</b>
     *
     * @param fragment asking for permission
     * @return whether the user has checked "Never ask again" option
     */
    public boolean neverAskForCamera(@NonNull Fragment fragment) {
        return !(hasAskedForCameraPermission() == shouldShowCameraRationale(fragment));
    }

    /**
     * If true the user has checked the "Never ask again" option. We get this by checking two things.
     * Whether we've asked before, checked with {@link #hasAskedForCameraPermission()}, which returns
     * false if the user has never seen the dialog. We also check whether we should request the permission
     * rational for the system. If these two don't match up, the user has selected "Never ask again".
     * <p>
     * <b>Note: if we have the camera permission, and we call this method, it will return true.
     * This is intentional as we don't want to ask for permissions once we have them. If you
     * do that, you will loose the permission and dialog will come up again.</b>
     * <p>
     * <b>Another note: if the user selected "Never ask again", then they give you permissions in
     * the app settings page, and then remove them in the same page. This method will return true.
     * Even though at that point you can ask for permissions. I have not been able to figure out a
     * way around this.</b>
     *
     * @param activity asking for permission
     * @return whether the user has checked "Never ask again" option
     */
    public boolean neverAskForCamera(@NonNull Activity activity) {
        return !(hasAskedForCameraPermission() == shouldShowCameraRationale(activity));
    }

    // ==== LOCATION PERMISSION ====================================================================

    /**
     * @return if we have location permissions
     */
    public boolean isLocationGranted() {
        return isFineLocationGranted()
                || isCoarseLocationGranted();
    }

    @VisibleForTesting
    private boolean isFineLocationGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                ACCESS_FINE_LOCATION)
                == PERMISSION_GRANTED;
    }

    @VisibleForTesting
    private boolean isCoarseLocationGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                ACCESS_COARSE_LOCATION)
                == PERMISSION_GRANTED;
    }

    private List<String> getLocationPermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(ACCESS_FINE_LOCATION);
        perms.add(ACCESS_COARSE_LOCATION);
        perms.add(ACCESS_BACKGROUND_LOCATION);
        return perms;
    }

    /**
     * Request the LOCATION permission.
     * @return Observable that will kick off LOCATION permission.
     */
    public LiveData<PermissionsResult> requestLocationPermission() {
        return requestPermissions(REQUEST_LOCATION_PERMISSION);
    }

    /**
     * Request the BOOT permission.
     * @return Observable that will kick off BOOT permission.
     */
    public LiveData<PermissionsResult> requestBootPermission() {
        return requestPermissions(REQUEST_BOOT_PERMISSION);
    }

    /**
     * @return if location permission has been previously requested.
     */
    public boolean hasAskedForLocationPermission() {
        return DbHelper.get()
                .isLocationPermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowLocationRationale(@NonNull Fragment fragment) {
        return (!isLocationGranted()
                && shouldShowRequestPermissionRationale(fragment, ACCESS_FINE_LOCATION)
                && shouldShowRequestPermissionRationale(fragment, ACCESS_COARSE_LOCATION));
    }

    private boolean shouldShowLocationRationale(@NonNull Activity activity) {
        return (!isLocationGranted()
                && shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
                && shouldShowRequestPermissionRationale(activity, ACCESS_COARSE_LOCATION));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}, its the same logic, but for location
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForLocation(@NonNull Fragment fragment) {
        return !(hasAskedForLocationPermission() == shouldShowLocationRationale(
                fragment));
    }

    /**
     * See {@link #neverAskForCamera(Activity)}, its the same logic, but for location
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForLocation(@NonNull Activity activity) {
        return !(hasAskedForLocationPermission() == shouldShowLocationRationale(
                activity));
    }

    // ==== MICROPHONE PERMISSION =============================================================

    /**
     * @return if we have audio recording permissions
     */
    public boolean isMicrophoneGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                RECORD_AUDIO)
                == PERMISSION_GRANTED;
    }

    /**
     * @return if we have audio recording permissions
     */
    public boolean isBootGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                RECEIVE_BOOT_COMPLETED)
                == PERMISSION_GRANTED;
    }

    private List<String> getMicrophonePermissions() {
        return Collections.singletonList(RECORD_AUDIO);
    }

    private List<String> getBootPermissions() {
        return Collections.singletonList(RECEIVE_BOOT_COMPLETED);
    }

    /**
     * Request the MICROPHONE permission.
     * @return Observable that will kick off MICROPHONE permission.
     */
    public LiveData<PermissionsResult> requestMicrophonePermission() {
        return requestPermissions(REQUEST_MICROPHONE_PERMISSION);
    }

    /**
     * @return if audio recording permission has been previously requested.
     */
    public boolean hasAskedForMicrophonePermission() {
        return DbHelper.get()
                .isAudioPermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowMicrophoneRationale(@NonNull Fragment fragment) {
        return (!isMicrophoneGranted()
                && shouldShowRequestPermissionRationale(fragment, RECORD_AUDIO));
    }

    private boolean shouldShowMicrophoneRationale(@NonNull Activity activity) {
        return (!isMicrophoneGranted()
                && shouldShowRequestPermissionRationale(activity, RECORD_AUDIO));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForMicrophone(@NonNull Fragment fragment) {
        return !(hasAskedForMicrophonePermission() == shouldShowMicrophoneRationale(fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForMicrophone(@NonNull Activity activity) {
        return !(hasAskedForMicrophonePermission() == shouldShowMicrophoneRationale(activity));
    }

    // ==== CALENDAR ===============================================================================

    /**
     * @return if we have calendar permissions
     */
    public boolean isCalendarGranted() {
        return isReadCalendarGranted() || isWriteCalendarGranted();
    }

    private boolean isReadCalendarGranted() {
        return ContextCompat.checkSelfPermission(mContext, READ_CALENDAR)
                == PERMISSION_GRANTED;
    }

    private boolean isWriteCalendarGranted() {
        return ContextCompat.checkSelfPermission(mContext, WRITE_CALENDAR)
                == PERMISSION_GRANTED;
    }

    /**
     * Request the CALENDAR permission.
     * @return Observable that will kick off CALENDAR permission.
     */
    private List<String> getCalendarPermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(READ_CALENDAR);
        perms.add(WRITE_CALENDAR);
        return perms;
    }

    public LiveData<PermissionsResult> requestCalendarPermission() {
        return requestPermissions(REQUEST_CALENDAR_PERMISSION);
    }

    /**
     * @return if calendar permission has been previously requested.
     */
    public boolean hasAskedForCalendarPermission() {
        return DbHelper.get()
                .isCalendarPermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowCalendarRationale(@NonNull Fragment fragment) {
        return !isCalendarGranted()
                && shouldShowRequestPermissionRationale(fragment,
                READ_CALENDAR)
                && shouldShowRequestPermissionRationale(fragment,
                WRITE_CALENDAR);
    }

    private boolean shouldShowCalendarRationale(@NonNull Activity activity) {
        return !isCalendarGranted()
                && shouldShowRequestPermissionRationale(activity,
                READ_CALENDAR)
                && shouldShowRequestPermissionRationale(activity,
                WRITE_CALENDAR);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForCalendar(@NonNull Fragment fragment) {
        return !(hasAskedForCalendarPermission() == shouldShowCalendarRationale(fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForCalendar(@NonNull Activity activity) {
        return !(hasAskedForCalendarPermission() == shouldShowCalendarRationale(activity));
    }

    // ==== CONTACTS ===============================================================================

    /**
     * @return if we have contact permissions
     */
    public boolean isContactsGranted() {
        return isReadContactsPermissionGranted()
                || isWriteContactsPermissionGranted()
                || isGetAccountsPermissionGranted();
    }

    private boolean isReadContactsPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, READ_CONTACTS)
                == PERMISSION_GRANTED;
    }

    private boolean isWriteContactsPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, WRITE_CONTACTS)
                == PERMISSION_GRANTED;
    }

    private boolean isGetAccountsPermissionGranted() {
        return ContextCompat.checkSelfPermission(mContext, GET_ACCOUNTS)
                == PERMISSION_GRANTED;
    }

    private List<String> getContactsPermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(READ_CONTACTS);
        perms.add(WRITE_CONTACTS);
        perms.add(GET_ACCOUNTS);
        return perms;
    }

    /**
     * Request the CONTACTS permission.
     * @return Observable that will kick off CONTACTS permission.
     */
    public LiveData<PermissionsResult> requestContactsPermission() {
        return requestPermissions(REQUEST_CONTACTS_PERMISSION);
    }

    /**
     * @return if contacts permission has been previously requested.
     */
    public boolean hasAskedForContactsPermission() {
        return DbHelper.get()
                .isContactsPermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowContactsRationale(@NonNull Fragment fragment) {
        return !isContactsGranted()
                && shouldShowRequestPermissionRationale(fragment,
                READ_CONTACTS)
                && shouldShowRequestPermissionRationale(fragment,
                WRITE_CONTACTS)
                && shouldShowRequestPermissionRationale(fragment,
                GET_ACCOUNTS);
    }

    private boolean shouldShowContactsRationale(@NonNull Activity activity) {
        return !isContactsGranted()
                && shouldShowRequestPermissionRationale(activity,
                READ_CONTACTS)
                && shouldShowRequestPermissionRationale(activity,
                WRITE_CONTACTS)
                && shouldShowRequestPermissionRationale(activity,
                GET_ACCOUNTS);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForContacts(@NonNull Fragment fragment) {
        return !(hasAskedForContactsPermission()
                == shouldShowContactsRationale(fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForContacts(@NonNull Activity activity) {
        return !(hasAskedForContactsPermission()
                == shouldShowContactsRationale(activity));
    }

    // ==== PHONE ================================================================================

    /**
     * @return if storage permissions are granted.
     */
    public boolean isPhoneGranted() {
        return isOnePhoneGranted();
    }

    private boolean isOnePhoneGranted() {
        return isReadPhoneStateGranted()
                || isCallPhoneGranted()
                || isReadCallLogGranted()
                || isWriteCallLogGranted()
                || isAddVoicemailGranted()
                || isUseSipGranted()
                || isProcessOutgoingCallsGranted();
    }

    private boolean isReadPhoneStateGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                READ_PHONE_STATE)
                == PERMISSION_GRANTED;
    }

    private boolean isCallPhoneGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                CALL_PHONE)
                == PERMISSION_GRANTED;
    }

    private boolean isReadCallLogGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                READ_CALL_LOG)
                == PERMISSION_GRANTED;
    }

    private boolean isWriteCallLogGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                WRITE_CALL_LOG)
                == PERMISSION_GRANTED;
    }

    private boolean isAddVoicemailGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                ADD_VOICEMAIL)
                == PERMISSION_GRANTED;
    }

    private boolean isUseSipGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                USE_SIP)
                == PERMISSION_GRANTED;
    }

    private boolean isProcessOutgoingCallsGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                PROCESS_OUTGOING_CALLS)
                == PERMISSION_GRANTED;
    }

    private List<String> getPhonePermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(READ_PHONE_STATE);
        perms.add(CALL_PHONE);
        perms.add(READ_CALL_LOG);
        perms.add(WRITE_CALL_LOG);
        perms.add(ADD_VOICEMAIL);
        perms.add(USE_SIP);
        perms.add(PROCESS_OUTGOING_CALLS);
        return perms;
    }

    /**
     * Request the PHONE permission.
     * @return Observable that will kick off PHONE permission.
     */
    public LiveData<PermissionsResult> requestPhonePermission() {
        return requestPermissions(REQUEST_PHONE_PERMISSION);
    }

    /**
     * @return if storage permission has been previously requested.
     */
    public boolean hasAskedForPhonePermission() {
        return DbHelper.get()
                .isPhonePermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowRequestPhoneRationale(@NonNull Fragment fragment) {
        return !isOnePhoneGranted()
                && shouldShowRequestPermissionRationale(fragment, READ_PHONE_STATE)
                && shouldShowRequestPermissionRationale(fragment, CALL_PHONE)
                && shouldShowRequestPermissionRationale(fragment, READ_CALL_LOG)
                && shouldShowRequestPermissionRationale(fragment, WRITE_CALL_LOG)
                && shouldShowRequestPermissionRationale(fragment, ADD_VOICEMAIL)
                && shouldShowRequestPermissionRationale(fragment, USE_SIP)
                && shouldShowRequestPermissionRationale(fragment, PROCESS_OUTGOING_CALLS);
    }

    private boolean shouldShowRequestPhoneRationale(@NonNull Activity activity) {
        return !isOnePhoneGranted()
                && shouldShowRequestPermissionRationale(activity, READ_PHONE_STATE)
                && shouldShowRequestPermissionRationale(activity, CALL_PHONE)
                && shouldShowRequestPermissionRationale(activity, READ_CALL_LOG)
                && shouldShowRequestPermissionRationale(activity, WRITE_CALL_LOG)
                && shouldShowRequestPermissionRationale(activity, ADD_VOICEMAIL)
                && shouldShowRequestPermissionRationale(activity, USE_SIP)
                && shouldShowRequestPermissionRationale(activity, PROCESS_OUTGOING_CALLS);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForPhone(@NonNull Fragment fragment) {
        return !(hasAskedForPhonePermission() == shouldShowRequestPhoneRationale(
                fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForPhone(@NonNull Activity activity) {
        return !(hasAskedForPhonePermission() == shouldShowRequestPhoneRationale(
                activity));
    }

    // ==== STORAGE ================================================================================

    /**
     * @return if storage permissions are granted.
     */
    public boolean isStorageGranted() {
        return isOneStorageGranted();
    }

    private boolean isOneStorageGranted() {
        return isWriteStorageGranted()
                || isReadStorageGranted();
    }

    private boolean isReadStorageGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                READ_EXTERNAL_STORAGE)
                == PERMISSION_GRANTED;
    }

    private boolean isWriteStorageGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                WRITE_EXTERNAL_STORAGE)
                == PERMISSION_GRANTED;
    }

    private List<String> getStoragePermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(WRITE_EXTERNAL_STORAGE);
        perms.add(READ_EXTERNAL_STORAGE);
        return perms;
    }

    private List<String> getVibratePermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(VIBRATE);
        return perms;
    }

    /**
     * Request the STORAGE permission.
     * @return Observable that will kick off STORAGE permission.
     */
    public LiveData<PermissionsResult> requestStoragePermission() {
        return requestPermissions(REQUEST_STORAGE_PERMISSION);
    }

    /**
     * Request the STORAGE permission.
     * @return Observable that will kick off STORAGE permission.
     */
    public LiveData<PermissionsResult> requestGoblobMapPermission() {
        return requestPermissions(REQUEST_STORAGE_PERMISSION, REQUEST_LOCATION_PERMISSION);
    }

    /**
     * @return if storage permission has been previously requested.
     */
    public boolean hasAskedForStoragePermission() {
        return DbHelper.get()
                .isStoragePermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowRequestStorageRationale(@NonNull Fragment fragment) {
        return !isOneStorageGranted()
                && shouldShowRequestPermissionRationale(fragment, WRITE_EXTERNAL_STORAGE)
                && shouldShowRequestPermissionRationale(fragment, READ_EXTERNAL_STORAGE);
    }

    private boolean shouldShowRequestStorageRationale(@NonNull Activity activity) {
        return !isOneStorageGranted()
                && shouldShowRequestPermissionRationale(activity, WRITE_EXTERNAL_STORAGE)
                && shouldShowRequestPermissionRationale(activity, READ_EXTERNAL_STORAGE);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForStorage(@NonNull Fragment fragment) {
        return !(hasAskedForStoragePermission() == shouldShowRequestStorageRationale(
                fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForStorage(@NonNull Activity activity) {
        return !(hasAskedForStoragePermission() == shouldShowRequestStorageRationale(
                activity));
    }

    // ==== BODY SENSORS ===============================================================================

    /**
     * @return if we have calendar permissions
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public boolean isBodySensorGranted() {
        return ContextCompat.checkSelfPermission(mContext, BODY_SENSORS)
                == PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private List<String> getBodySensorPermissions() {
        return Collections.singletonList(BODY_SENSORS);
    }

    /**
     * Request the BODY SENSOR permission.
     * @return Observable that will kick off BODY SENSOR permission.
     */
    public LiveData<PermissionsResult> requestBodySensorPermission() {
        return requestPermissions(REQUEST_BODY_SENSOR_PERMISSION);
    }

    /**
     * @return if calendar permission has been previously requested.
     */
    public boolean hasAskedForBodySensorPermission() {
        return DbHelper.get()
                .isBodySensorsPermissionsAsked();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @VisibleForTesting
    boolean shouldShowBodySensorRationale(@NonNull Fragment fragment) {
        return !isBodySensorGranted() && shouldShowRequestPermissionRationale(fragment,
                BODY_SENSORS);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private boolean shouldShowBodySensorRationale(@NonNull Activity activity) {
        return !isBodySensorGranted() && shouldShowRequestPermissionRationale(activity,
                BODY_SENSORS);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public boolean neverAskForBodySensor(@NonNull Fragment fragment) {
        return !(hasAskedForBodySensorPermission() == shouldShowBodySensorRationale(fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public boolean neverAskForBodySensor(@NonNull Activity activity) {
        return !(hasAskedForBodySensorPermission() == shouldShowBodySensorRationale(activity));
    }

    // ==== SMS ================================================================================

    /**
     * @return if storage permissions are granted.
     */
    public boolean isSmsGranted() {
        return isOneSmsGranted();
    }

    private boolean isOneSmsGranted() {
        return isSendSmsGranted()
                || isReceiveSmsGranted()
                || isReadSmsGranted()
                || isReceiveWapPushGranted()
                || isReceiveMmsGranted();
    }

    private boolean isSendSmsGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                SEND_SMS)
                == PERMISSION_GRANTED;
    }

    private boolean isReceiveSmsGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                RECEIVE_SMS)
                == PERMISSION_GRANTED;
    }

    private boolean isVibrateGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                VIBRATE)
                == PERMISSION_GRANTED;
    }

    private boolean isReadSmsGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                READ_SMS)
                == PERMISSION_GRANTED;
    }

    private boolean isReceiveWapPushGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                RECEIVE_WAP_PUSH)
                == PERMISSION_GRANTED;
    }

    private boolean isReceiveMmsGranted() {
        return ContextCompat.checkSelfPermission(mContext,
                RECEIVE_MMS)
                == PERMISSION_GRANTED;
    }

    private List<String> getSmsPermissions() {
        final List<String> perms = new ArrayList<>();
        perms.add(SEND_SMS);
        perms.add(RECEIVE_SMS);
        perms.add(READ_SMS);
        perms.add(RECEIVE_WAP_PUSH);
        perms.add(RECEIVE_MMS);
        return perms;
    }

    /**
     * Request the SMS permission.
     * @return Observable that will kick off SMS permission.
     */
    public LiveData<PermissionsResult> requestSmsPermission() {
        return requestPermissions(REQUEST_SMS_PERMISSION);
    }

    /**
     * Request the SMS permission.
     * @return Observable that will kick off SMS permission.
     */
    public LiveData<PermissionsResult> requestVibratePermission() {
        return requestPermissions(REQUEST_VIBRATE_PERMISSION);
    }

    /**
     * @return if storage permission has been previously requested.
     */
    public boolean hasAskedForSmsPermission() {
        return DbHelper.get()
                .isSmsPermissionsAsked();
    }

    @VisibleForTesting
    boolean shouldShowRequestSmsRationale(@NonNull Fragment fragment) {
        return !isOneSmsGranted()
                && shouldShowRequestPermissionRationale(fragment, SEND_SMS)
                && shouldShowRequestPermissionRationale(fragment, RECEIVE_SMS)
                && shouldShowRequestPermissionRationale(fragment, READ_SMS)
                && shouldShowRequestPermissionRationale(fragment, RECEIVE_WAP_PUSH)
                && shouldShowRequestPermissionRationale(fragment, RECEIVE_MMS);
    }

    private boolean shouldShowRequestSmsRationale(@NonNull Activity activity) {
        return !isOneSmsGranted()
                && shouldShowRequestPermissionRationale(activity, SEND_SMS)
                && shouldShowRequestPermissionRationale(activity, RECEIVE_SMS)
                && shouldShowRequestPermissionRationale(activity, READ_SMS)
                && shouldShowRequestPermissionRationale(activity, RECEIVE_WAP_PUSH)
                && shouldShowRequestPermissionRationale(activity, RECEIVE_MMS);
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param fragment to check with
     * @return if should not ask
     */
    public boolean neverAskForSms(@NonNull Fragment fragment) {
        return !(hasAskedForSmsPermission() == shouldShowRequestSmsRationale(
                fragment));
    }

    /**
     * See {@link #neverAskForCamera(Fragment)}
     *
     * @param activity to check with
     * @return if should not ask
     */
    public boolean neverAskForSms(@NonNull Activity activity) {
        return !(hasAskedForSmsPermission() == shouldShowRequestSmsRationale(
                activity));
    }

    // ==== PERMISSION REQUESTS ====================================================================

    // ---- REQUEST PERMISSION RATIONALE -----------------------------------------------------------

    /**
     * Wraps {@link Fragment#shouldShowRequestPermissionRationale(String)}
     *
     * @param fragment   checking for permissions
     * @param permission to check
     * @return if we should show
     */
    private boolean shouldShowRequestPermissionRationale(@NonNull Fragment fragment,
                                                         @NonNull String permission) {
        return fragment.shouldShowRequestPermissionRationale(permission);
    }

    /**
     * Wraps {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)}
     *
     * @param activity   checking for permissions
     * @param permission to check
     * @return if we should show
     */
    private boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
                                                         @NonNull String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    // ---- INTENT TO SETTINGS ---------------------------------------------------------------------

    /**
     * Open the app's settings page so the user could switch an activity.
     *
     * @param activity starting this intent.
     */
    public void intentToAppSettings(@NonNull Activity activity) {
        //Open the specific App Info page:
        Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            intent = new Intent(ACTION_MANAGE_APPLICATIONS_SETTINGS);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
            }
        }
    }

    // ==== PERMISSION REQUESTS ====================================================================

    @SuppressWarnings("WeakerAccess")
    public LiveData<PermissionsResult> requestPermissions(@Permission int... permissions) {
        if (checkPermissionsGranted(permissions)) {
            MutableLiveData data = new MutableLiveData();
            data.setValue(new PermissionsResult(true, false));
            return data;
        } else {
            markPermissionsAsked(permissions);
            return requestPermission(getPermissionsToRequest(permissions));
        }
    }

    private boolean checkPermissionsGranted(@Permission int... permissions) {
        for (int permission : permissions) {
            if (!isPermissionGranted(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPermissionGranted(@Permission int permission) {
        switch (permission) {
            case REQUEST_CAMERA_PERMISSION:
                return isCameraGranted();
            case REQUEST_LOCATION_PERMISSION:
                return isLocationGranted();
            case REQUEST_MICROPHONE_PERMISSION:
                return isMicrophoneGranted();
            case REQUEST_CALENDAR_PERMISSION:
                return isCalendarGranted();
            case REQUEST_CONTACTS_PERMISSION:
                return isContactsGranted();
            case REQUEST_STORAGE_PERMISSION:
                return isStorageGranted();
            case REQUEST_PHONE_PERMISSION:
                return isPhoneGranted();
            case REQUEST_BODY_SENSOR_PERMISSION:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && isBodySensorGranted();
            case REQUEST_SMS_PERMISSION:
                return isSmsGranted();
            case REQUEST_VIBRATE_PERMISSION:
                return isVibrateGranted();
            case REQUEST_BOOT_PERMISSION:
                return isBootGranted();
        }
        return false;
    }

    private void markPermissionsAsked(@Permission int... permissions) {
        for (int permission : permissions) {
            markPermissionAsked(permission);
        }
    }

    private void markPermissionAsked(@Permission int permission) {
        switch (permission) {
            case REQUEST_CAMERA_PERMISSION:
                DbHelper.get()
                        .setCameraPermissionsAsked();
                return;
            case REQUEST_LOCATION_PERMISSION:
                DbHelper.get()
                        .setLocationPermissionsAsked();
                return;
            case REQUEST_MICROPHONE_PERMISSION:
                DbHelper.get()
                        .setMicrophonePermissionsAsked();
                return;
            case REQUEST_BOOT_PERMISSION:
                DbHelper.get()
                        .setBootPermissionsAsked();
                return;
            case REQUEST_CALENDAR_PERMISSION:
                DbHelper.get()
                        .setCalendarPermissionsAsked();
                return;
            case REQUEST_CONTACTS_PERMISSION:
                DbHelper.get()
                        .setContactsPermissionsAsked();
                return;
            case REQUEST_STORAGE_PERMISSION:
                DbHelper.get()
                        .setStoragePermissionsAsked();
                return;
            case REQUEST_PHONE_PERMISSION:
                DbHelper.get()
                        .setPhonePermissionsAsked();
                return;
            case REQUEST_BODY_SENSOR_PERMISSION:
                DbHelper.get()
                        .setBodySensorsPermissionsAsked();
                return;
            case REQUEST_SMS_PERMISSION:
                DbHelper.get()
                        .setSmsPermissionsAsked();
            case REQUEST_VIBRATE_PERMISSION:
                DbHelper.get()
                        .setVibratePermissionsAsked();
        }
    }

    private String[] getPermissionsToRequest(@Permission int... permissions) {
        final ArrayList<String> toRequest = new ArrayList<>();
        for (int permission : permissions) {
            toRequest.addAll(getPermissionsFor(permission));
        }
        return toRequest.toArray(new String[toRequest.size()]);
    }

    private List<String> getPermissionsFor(@Permission int permission) {
        switch (permission) {
            case REQUEST_CAMERA_PERMISSION:
                return getCameraPermissions();
            case REQUEST_LOCATION_PERMISSION:
                return getLocationPermissions();
            case REQUEST_MICROPHONE_PERMISSION:
                return getMicrophonePermissions();
            case REQUEST_BOOT_PERMISSION:
                return getBootPermissions();
            case REQUEST_CALENDAR_PERMISSION:
                return getCalendarPermissions();
            case REQUEST_CONTACTS_PERMISSION:
                return getContactsPermissions();
            case REQUEST_STORAGE_PERMISSION:
                return getStoragePermissions();
            case REQUEST_PHONE_PERMISSION:
                return getPhonePermissions();
            case REQUEST_BODY_SENSOR_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    return getBodySensorPermissions();
                }
                return Collections.emptyList();
            case REQUEST_SMS_PERMISSION:
                return getSmsPermissions();
            case REQUEST_VIBRATE_PERMISSION:
                return getVibratePermissions();
        }
        return Collections.emptyList();
    }

    /**
     * Request a permission.
     *
     * @param permissions to request
     */
    private LiveData<PermissionsResult> requestPermission(@NonNull final String... permissions) {
        assertPermissionsNotGranted(permissions);
        MutableLiveData<PermissionsResult> data = new MutableLiveData();

        if (isAskingForPermissions()) {
            data.setValue(null);
        } else {
            assertMainThread();
            final int key = Arrays.hashCode(permissions);

            if (isAskingForPermissions()) {
                throw new IllegalStateException(
                        "Already requesting permissions, cannot ask for permissions.");
            }

            startPermissionsActivity(permissions);

            producersMap.put(key, data);
            // Clean up if we unsubscribe before permissions come back
            /*subscriber.setProducer(producer);
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    producersMap.remove(key);
                }
            }));*/
        }
        return data;
    }

    private boolean isAskingForPermissions() {
        return PermissionRequestActivity.isAskingForPermissions();
    }

    private void startPermissionsActivity(@NonNull String[] permissions) {
        Intent i = PermissionRequestActivity.getIntent(mContext, permissions);
        mContext.startActivity(i);
    }

    private void assertMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException(
                    "Cannot request permissions off the main thread.");
        }
    }

    /* PACKAGE */ void onRequestPermissionsResult(@NonNull String[] permissions,
                                                  @NonNull int[] grantResults) {
        final int key = Arrays.hashCode(permissions);
        MutableLiveData<PermissionsResult> producer = this.producersMap.get(key);
        if (producer == null) {
            return;
        }
        final boolean granted = arePermissionsGranted(grantResults);
        producer.setValue(new PermissionsResult(granted, true));
        producersMap.remove(key);
    }

    // ---- CHECK PERMISSIONS GRANTED --------------------------------------------------------------

    /**
     * Check if permissions are granted.
     *
     * @param grantResults permissions returned in {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     * @return whether all permissions were granted
     */
    private boolean arePermissionsGranted(@NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result != PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // ---- DEBUG HELPER ---------------------------------------------------------------------------

    // If we have a permissions, and we ask again, and the user ignores it, or says no, we loose it.
    // also, even if we have a permission, and ask for it event, the system will ask it
    private void assertPermissionsNotGranted(@NonNull String[] permissions) {
        if (BuildConfig.DEBUG) {
            boolean granted;
            for (String permission : permissions) {
                granted = ContextCompat.checkSelfPermission(mContext, permission)
                            == PERMISSION_GRANTED;
                if (granted) {
                    //throw new AssertionError("Yo! You's need to not ask for " + permission + ". It's already been granted!");
                }
            }
        }
    }
}
