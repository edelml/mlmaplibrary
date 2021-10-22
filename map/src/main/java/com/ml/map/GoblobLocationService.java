package com.ml.map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.ml.map.provider.LocationEngineCallback;
import com.ml.map.provider.LocationEngineProvider;
import com.ml.map.provider.LocationEngineResult;
import com.ml.map.route.AbstractRouting;
import com.ml.map.route.Route;
import com.ml.map.route.RouteEvents;
import com.ml.map.route.RouteException;
import com.ml.map.route.Routing;
import com.ml.map.route.RoutingListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

public class GoblobLocationService extends Service {
    private static final String TAG = GoblobLocationService.class.getSimpleName();
    private static final Logger LOG = LoggerFactory.getLogger(GoblobLocationService.class);
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static NotificationUtils notificationUtils;
    private static int NOTIFICATION_ID = 8675309;
    private static GoblobLocationService instance;
    private final IBinder binder = new GpsLoggingBinder();
    protected LocationManager gpsLocationManager;
    AlarmManager nextPointAlarmManager;
    PendingIntent activityRecognitionPendingIntent;
    private Notification.Builder nfc = null;
    // ---------------------------------------------------
    // Helpers and managers
    // ---------------------------------------------------
    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private GoblobLocationManager goblobLocationManager = GoblobLocationManager.getInstance();
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    private GeneralLocationListener passiveLocationListener;
    private Intent alarmIntent;
    private Handler handler = new Handler();
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location bestLocation;
    private boolean isStarted = false;
    private int routeAttempt = 0;
    private LocationRequest mLocationRequest;
    // ---------------------------------------------------
    private Runnable stopManagerRunnable = new Runnable() {
        @Override
        public void run() {
            LOG.warn("Absolute timeout reached, giving up on this point");
            stopManagerAndResetAlarm();
        }
    };

    public static GoblobLocationService get() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {
        instance = this;

        notificationUtils = new NotificationUtils(this, "location");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        registerEventBus();

        //GoblobLocationManager.getInstance().setupShareLocation();
    }

    private void requestActivityRecognitionUpdates() {

        //if (preferenceHelper.shouldNotLogIfUserIsStill()) {
        LOG.debug("Requesting activity recognition updates");
        Intent intent = new Intent(getApplicationContext(), GoblobLocationService.class);
        activityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognitionClient arClient = ActivityRecognition.getClient(getApplicationContext());
        arClient.requestActivityUpdates(preferenceHelper.getMinimumLoggingInterval() * 1000, activityRecognitionPendingIntent);
        //}

    }

    private void stopActivityRecognitionUpdates() {
        try {
            if (activityRecognitionPendingIntent != null) {
                LOG.debug("Stopping activity recognition updates");
                ActivityRecognitionClient arClient = ActivityRecognition.getClient(getApplicationContext());
                arClient.removeActivityUpdates(activityRecognitionPendingIntent);
            }
        } catch (Exception ex) {
            LOG.error("Could not stop activity recognition service", ex);
        }
    }

    private void registerEventBus() {
        EventBus.getDefault().registerSticky(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "GpsLoggingService is being destroyed by Android OS.");
        unregisterEventBus();
        removeNotification();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        LOG.error("Android is low on memory!");
        super.onLowMemory();
    }

    private void handleIntent(Intent intent) {

        ActivityRecognitionResult arr = ActivityRecognitionResult.extractResult(intent);
        if (arr != null) {
            EventBus.getDefault().post(new ServiceEvents.ActivityRecognitionEvent(arr));
            return;
        }

        if (intent != null) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {


                if (!Systems.locationPermissionsGranted(this)) {
                    LOG.error("User has not granted permission to access location services. Will not continue!");
                    return;
                }

                boolean needToStartGpsManager = false;

                if (bundle.getBoolean(IntentConstants.IMMEDIATE_START)) {
                    LOG.info("Intent received - Start Logging Now");
                    EventBus.getDefault().post(new CommandEvents.RequestStartStop(true));
                }

                if (bundle.getBoolean("currentlocation")) {
                    LOG.info("Intent received - currentlocation");
                    getLastLocation();
                }

                if (bundle.getBoolean(IntentConstants.IMMEDIATE_STOP)) {
                    LOG.info("Intent received - Stop logging now");
                    EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                }

                if (bundle.getBoolean(IntentConstants.GET_STATUS)) {
                    LOG.info("Intent received - Sending Status by broadcast");
                    EventBus.getDefault().post(new CommandEvents.GetStatus());
                }


                if (bundle.getBoolean(IntentConstants.AUTOSEND_NOW)) {
                    LOG.info("Intent received - Send Email Now");
                    EventBus.getDefault().post(new CommandEvents.AutoSend(null));
                }

                if (bundle.getBoolean(IntentConstants.GET_NEXT_POINT)) {
                    LOG.info("Intent received - Get Next Point");
                    needToStartGpsManager = true;
                }

                if (bundle.getBoolean(IntentConstants.ROUTING)) {
                    LOG.info("Routing....");
                    LatLng origin = bundle.getParcelable(IntentConstants.ORIGIN);
                    LatLng destination = bundle.getParcelable(IntentConstants.DESTINATION);
                    boolean update = bundle.getBoolean(IntentConstants.UPDATE);
                    AbstractRouting.TravelMode travelMode = (AbstractRouting.TravelMode) bundle.get(IntentConstants.TRAVEL_MODE);
                    traceRoute(travelMode, origin, destination, update);
                }

                if (bundle.getString(IntentConstants.SET_DESCRIPTION) != null) {
                    LOG.info("Intent received - Set Next Point Description: " + bundle.getString(IntentConstants.SET_DESCRIPTION));
                    EventBus.getDefault().post(new CommandEvents.Annotate(bundle.getString(IntentConstants.SET_DESCRIPTION)));
                }

                if (bundle.getString(IntentConstants.SWITCH_PROFILE) != null) {
                    LOG.info("Intent received - switch profile: " + bundle.getString(IntentConstants.SWITCH_PROFILE));
                    EventBus.getDefault().post(new ProfileEvents.SwitchToProfile(bundle.getString(IntentConstants.SWITCH_PROFILE)));
                }

                if (bundle.get(IntentConstants.PREFER_CELLTOWER) != null) {
                    boolean preferCellTower = bundle.getBoolean(IntentConstants.PREFER_CELLTOWER);
                    LOG.debug("Intent received - Set Prefer Cell Tower: " + String.valueOf(preferCellTower));

                    if (preferCellTower) {
                        preferenceHelper.setChosenListeners(0);
                    } else {
                        preferenceHelper.setChosenListeners(1, 2);
                    }

                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.TIME_BEFORE_LOGGING) != null) {
                    int timeBeforeLogging = bundle.getInt(IntentConstants.TIME_BEFORE_LOGGING);
                    LOG.debug("Intent received - logging interval: " + String.valueOf(timeBeforeLogging));
                    preferenceHelper.setMinimumLoggingInterval(timeBeforeLogging);
                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.DISTANCE_BEFORE_LOGGING) != null) {
                    int distanceBeforeLogging = bundle.getInt(IntentConstants.DISTANCE_BEFORE_LOGGING);
                    LOG.debug("Intent received - Set Distance Before Logging: " + String.valueOf(distanceBeforeLogging));
                    preferenceHelper.setMinimumDistanceInMeters(distanceBeforeLogging);
                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.GPS_ON_BETWEEN_FIX) != null) {
                    boolean keepBetweenFix = bundle.getBoolean(IntentConstants.GPS_ON_BETWEEN_FIX);
                    LOG.debug("Intent received - Set Keep Between Fix: " + String.valueOf(keepBetweenFix));
                    preferenceHelper.setShouldKeepGPSOnBetweenFixes(keepBetweenFix);
                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.RETRY_TIME) != null) {
                    int retryTime = bundle.getInt(IntentConstants.RETRY_TIME);
                    LOG.debug("Intent received - Set duration to match accuracy: " + String.valueOf(retryTime));
                    preferenceHelper.setLoggingRetryPeriod(retryTime);
                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.ABSOLUTE_TIMEOUT) != null) {
                    int absoluteTimeout = bundle.getInt(IntentConstants.ABSOLUTE_TIMEOUT);
                    LOG.debug("Intent received - Set absolute timeout: " + String.valueOf(absoluteTimeout));
                    preferenceHelper.setAbsoluteTimeoutForAcquiringPosition(absoluteTimeout);
                    needToStartGpsManager = true;
                }

                if (bundle.get(IntentConstants.LOG_ONCE) != null) {
                    boolean logOnceIntent = bundle.getBoolean(IntentConstants.LOG_ONCE);
                    LOG.debug("Intent received - Log Once: " + String.valueOf(logOnceIntent));
                    needToStartGpsManager = false;
                    logOnce();
                }

                try {
                    if (bundle.get(Intent.EXTRA_ALARM_COUNT) != "0") {
                        needToStartGpsManager = true;
                    }
                } catch (Throwable t) {
                    LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Received a weird EXTRA_ALARM_COUNT value. Cannot continue.");
                    needToStartGpsManager = false;
                }


                if (needToStartGpsManager && goblobLocationManager.isStarted()) {
                    startGpsManager();
                }
            }
        } else {
            // A null intent is passed in if the service has been killed and restarted.
            LOG.debug("Service restarted with null intent. Were we logging previously - " + goblobLocationManager.isStarted());
            if (goblobLocationManager.isStarted()) {
                startLogging();
            }

        }
    }

    /**
     * Sets up the auto email timers based on user preferences.
     */
    @TargetApi(23)
    public void setupAutoSendTimers() {
        LOG.debug("Setting up autosend timers. Auto Send Enabled - " + String.valueOf(preferenceHelper.isAutoSendEnabled())
                + ", Auto Send Delay - " + String.valueOf(goblobLocationManager.getAutoSendDelay()));

        if (preferenceHelper.isAutoSendEnabled() && goblobLocationManager.getAutoSendDelay() > 0) {
            long triggerTime = System.currentTimeMillis() + (long) (goblobLocationManager.getAutoSendDelay() * 60 * 1000);

            alarmIntent = new Intent(this, AlarmReceiver.class);
            cancelAlarm();

            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Systems.isDozing(this)) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerTime, sender);
            }
            LOG.debug("Autosend alarm has been set");

        } else {
            if (alarmIntent != null) {
                LOG.debug("alarmIntent was null, canceling alarm");
                cancelAlarm();
            }
        }
    }

    public void logOnce() {
        goblobLocationManager.setSinglePointMode(true);

        if (goblobLocationManager.isStarted()) {
            startGpsManager();
        } else {
            startLogging();
        }
    }

    private void cancelAlarm() {
        if (alarmIntent != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(sender);
        }
    }

    /**
     * Method to be called if user has chosen to auto email log files when he
     * stops logging
     */
    private void autoSendLogFileOnStop() {
        if (preferenceHelper.isAutoSendEnabled() && preferenceHelper.shouldAutoSendOnStopLogging()) {
            autoSendLogFile(null);
        }
    }

    /**
     * Calls the Auto Senders which process the files and send it.
     */
    private void autoSendLogFile(@Nullable String formattedFileName) {

        LOG.debug("Filename: " + formattedFileName);

        if (!Strings.isNullOrEmpty(formattedFileName) || !Strings.isNullOrEmpty(Strings.getFormattedFileName())) {
            String fileToSend = Strings.isNullOrEmpty(formattedFileName) ? Strings.getFormattedFileName() : formattedFileName;
            FileSenderFactory.autoSendFiles(fileToSend);
            setupAutoSendTimers();
        }
    }

    private void resetAutoSendTimersIfNecessary() {

        if (goblobLocationManager.getAutoSendDelay() != preferenceHelper.getAutoSendInterval()) {
            goblobLocationManager.setAutoSendDelay(preferenceHelper.getAutoSendInterval());
            setupAutoSendTimers();
        }
    }

    /**
     * Resets the form, resets file name if required, reobtains preferences
     */
    protected void startLogging() {
        LOG.debug(".");

        if (isStarted)
            return;

        goblobLocationManager.setAddNewTrackSegment(true);

        try {
            startForeground(NOTIFICATION_ID, new Notification());
        } catch (Exception ex) {
            LOG.error("Could not start GPSLoggingService in foreground. ", ex);
        }

        isStarted = true;

        goblobLocationManager.setStarted(true);

        resetAutoSendTimersIfNecessary();
        showNotification();
        setupAutoSendTimers();
        resetCurrentFileName(true);
        notifyClientsStarted(true);
        startPassiveManager();
        startGpsManager();
        requestActivityRecognitionUpdates();
    }

    private void notifyByBroadcast(boolean loggingStarted) {
        LOG.debug("Sending a custom broadcast");
        String event = (loggingStarted) ? "started" : "stopped";
        Intent sendIntent = new Intent();
        sendIntent.setAction("com.goblob.EVENT");
        sendIntent.putExtra("goblobevent", event);
        sendIntent.putExtra("filename", goblobLocationManager.getCurrentFormattedFileName());
        sendIntent.putExtra("startedtimestamp", goblobLocationManager.getStartTimeStamp());
        sendBroadcast(sendIntent);
    }

    /**
     * Informs main activity and broadcast listeners whether logging has started/stopped
     */
    private void notifyClientsStarted(boolean started) {
        LOG.info((started) ? getString(R.string.started) : getString(R.string.stopped));
        notifyByBroadcast(started);
        EventBus.getDefault().post(new ServiceEvents.LoggingStatus(started));
    }

    /**
     * Notify status of logger
     */
    private void notifyStatus(boolean started) {
        LOG.info((started) ? getString(R.string.started) : getString(R.string.stopped));
        notifyByBroadcast(started);
    }

    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Stops logging, removes notification, stops GPS manager, stops email timer
     */
    public void stopLogging() {
        LOG.debug(".");
        GoblobLocationManager.getInstance().setFollowRoute(false);
        GoblobLocationManager.getInstance().simulateRoute(false);
        GoblobLocationManager.getInstance().deleteCurrentRoute();
        goblobLocationManager.setAddNewTrackSegment(true);
        goblobLocationManager.setTotalTravelled(0);
        bestLocation = null;
        goblobLocationManager.setPreviousLocationInfo(null);
        isStarted = false;
        goblobLocationManager.setStarted(false);
        goblobLocationManager.setUserStillSinceTimeStamp(0);
        goblobLocationManager.setLatestTimeStamp(0);
        stopAbsoluteTimer();
        // Email log file before setting location info to null
        autoSendLogFileOnStop();
        cancelAlarm();
        goblobLocationManager.setCurrentLocationInfo(null);
        goblobLocationManager.setSinglePointMode(false);
        stopForeground(true);

        removeNotification();
        stopAlarm();
        stopGpsManager();
        stopPassiveManager();
        stopActivityRecognitionUpdates();
        notifyClientsStarted(false);
        goblobLocationManager.setCurrentFileName("");
        goblobLocationManager.setCurrentFormattedFileName("");
        stopSelf();
    }

    /**
     * Hides the notification icon in the status bar if it's visible.
     */
    private void removeNotification() {
        notificationUtils.cancel(NOTIFICATION_ID);
    }

    /**
     * Shows a notification icon in the status bar for GPS Logger
     */
    private void showNotification() {

        Intent stopLoggingIntent = new Intent(this, GoblobLocationService.class);
        stopLoggingIntent.setAction("NotificationButton_STOP");
        stopLoggingIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
        PendingIntent piStop = PendingIntent.getService(this, 0, stopLoggingIntent, 0);

        Intent annotateIntent = new Intent(this, NotificationAnnotationActivity.class);
        annotateIntent.setAction("com.goblob.NOTIFICATION_BUTTON");
        PendingIntent piAnnotate = PendingIntent.getActivity(this, 0, annotateIntent, 0);

        // What happens when the notification item is clicked
        Intent contentIntent = new Intent();
        contentIntent.setClassName(BuildConfig.LIBRARY_PACKAGE_NAME, "com.ml.map.MainActivity");
        contentIntent.setPackage(getBaseContext().getPackageName());
        contentIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(contentIntent);

        PendingIntent pending = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence contentTitle = getString(R.string.goblob_still_running);
        CharSequence contentText = getString(R.string.app_name);
        long notificationTime = System.currentTimeMillis();

        if (goblobLocationManager.hasValidLocation()) {
            contentTitle = Strings.getFormattedLatitude(goblobLocationManager.getCurrentLatitude()) + ", "
                    + Strings.getFormattedLongitude(goblobLocationManager.getCurrentLongitude());

            contentText = Html.fromHtml("<b>" + getString(R.string.txt_altitude) + "</b> " + Strings.getDistanceDisplay(this, goblobLocationManager.getCurrentLocationInfo().getAltitude(), preferenceHelper.shouldDisplayImperialUnits(), false)
                    + "  "
                    + "<b>" + getString(R.string.txt_travel_duration) + "</b> " + Strings.getDescriptiveDurationString((int) (System.currentTimeMillis() - goblobLocationManager.getStartTimeStamp()) / 1000, this)
                    + "  "
                    + "<b>" + getString(R.string.txt_accuracy) + "</b> " + Strings.getDistanceDisplay(this, goblobLocationManager.getCurrentLocationInfo().getAccuracy(), preferenceHelper.shouldDisplayImperialUnits(), true));

            notificationTime = goblobLocationManager.getCurrentLocationInfo().getTime();
        }

        if (nfc == null) {
            nfc = notificationUtils.getAndroidNotificationLocation();

            nfc.setContentIntent(pending);
            nfc.setSmallIcon(R.drawable.notification);
            nfc.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.goblob));
            nfc.setStyle(new Notification.BigTextStyle().bigText(contentText).setBigContentTitle(contentTitle));

            if (!preferenceHelper.shouldHideNotificationButtons()) {
                nfc./*addAction(R.drawable.annotate2, getString(R.string.menu_annotate), piAnnotate)
                        .*/addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.shortcut_stop), piStop);
            }
        }

        nfc.setContentTitle(contentTitle);
        nfc.setContentText(contentText);
        nfc.setStyle(new Notification.BigTextStyle().bigText(contentText).setBigContentTitle(contentTitle));
        nfc.setWhen(notificationTime);

        nfc.setAutoCancel(false);

        notificationUtils.notify(NOTIFICATION_ID, nfc.build());
    }

    @SuppressLint("MissingPermission")
    private void startFusedLocation() {
        if (mLocationCallback == null) {
            mLocationCallback = new GeneralLocationListener(this, "FUSED");
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("ResourceType")
    private void startPassiveManager() {
        //if (preferenceHelper.getChosenListeners().contains(LocationManager.PASSIVE_PROVIDER)) {
        LOG.debug("Starting passive location listener");
        if (passiveLocationListener == null) {
            passiveLocationListener = new GeneralLocationListener(this, BundleConstants.PASSIVE);
        }
        passiveLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        passiveLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 0, passiveLocationListener);
        //}
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (GoblobLocationManager.getInstance().isStarted()) {
            Log.e(TAG, "***********make alarm************");
            Intent restartService = new Intent(getApplicationContext(), this.getClass());
            restartService.setPackage(getPackageName());
            PendingIntent restartPendingIntent =
                    PendingIntent.getService(getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);

            if (Build.VERSION.SDK_INT < 23) {
                if (Build.VERSION.SDK_INT >= 19) {
                    nextPointAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
                            restartPendingIntent);
                } else {
                    nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
                            restartPendingIntent);
                }
            } else {
                nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
                        restartPendingIntent);
            }
        }

        super.onTaskRemoved(rootIntent);
    }

    /**
     * Starts the location manager. There are two location managers - GPS and
     * Cell Tower. This code determines which manager to request updates from
     * based on user preference and whichever is enabled. If GPS is enabled on
     * the phone, that is used. But if the user has also specified that they
     * prefer cell towers, then cell towers are used. If neither is enabled,
     * then nothing is requested.
     */
    @SuppressLint("MissingPermission")
    @SuppressWarnings("ResourceType")
    private void startGpsManager() {

        startFusedLocation();

        //If the user has been still for more than the minimum seconds
        if (userHasBeenStillForTooLong()) {
            LOG.info("No movement detected in the past interval, will not log");
            setAlarmForNextPoint();
            return;
        }

        if (gpsLocationListener == null) {
            gpsLocationListener = new GeneralLocationListener(this, "GPS");
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralLocationListener(this, "CELL");
        }

        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkTowerAndGpsStatus();

        if (goblobLocationManager.isGpsEnabled()) {
            LOG.info("Requesting GPS location updates");
            // gps satellite based
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, gpsLocationListener);
            gpsLocationManager.addGpsStatusListener(gpsLocationListener);
            gpsLocationManager.addNmeaListener(gpsLocationListener);

            goblobLocationManager.setUsingGps(true);
            startAbsoluteTimer();
        }

        if (goblobLocationManager.isTowerEnabled() && !goblobLocationManager.isGpsEnabled()) {
            LOG.info("Requesting cell and wifi location updates");
            goblobLocationManager.setUsingGps(false);
            // Cell tower and wifi based
            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, towerLocationListener);

            startAbsoluteTimer();
        }

        if (!goblobLocationManager.isTowerEnabled() && !goblobLocationManager.isGpsEnabled()) {
            LOG.error("No provider available!");
            goblobLocationManager.setUsingGps(false);
            LOG.error(getString(R.string.gpsprovider_unavailable));
            stopLogging();
            setLocationServiceUnavailable();
            return;
        }

        EventBus.getDefault().post(new ServiceEvents.WaitingForLocation(true));
        goblobLocationManager.setWaitingForLocation(true);
    }

    private boolean userHasBeenStillForTooLong() {
        return !goblobLocationManager.hasDescription() && !goblobLocationManager.isSinglePointMode() &&
                (goblobLocationManager.getUserStillSinceTimeStamp() > 0 && (System.currentTimeMillis() - goblobLocationManager.getUserStillSinceTimeStamp()) > (preferenceHelper.getMinimumLoggingInterval() * 1000));
    }

    private void startAbsoluteTimer() {
        if (preferenceHelper.getAbsoluteTimeoutForAcquiringPosition() >= 1) {
            handler.postDelayed(stopManagerRunnable, preferenceHelper.getAbsoluteTimeoutForAcquiringPosition() * 1000);
        }
    }

    private void stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable);
    }

    /**
     * This method is called periodically to determine whether the cell tower /
     * gps providers have been enabled, and sets class level variables to those
     * values.
     */
    private void checkTowerAndGpsStatus() {
        goblobLocationManager.setTowerEnabled(towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        goblobLocationManager.setGpsEnabled(gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    /**
     * Stops the location managers
     */
    @SuppressWarnings("ResourceType")
    private void stopGpsManager() {

        stopFusedLocation();

        if (towerLocationListener != null) {
            LOG.debug("Removing towerLocationManager updates");
            towerLocationManager.removeUpdates(towerLocationListener);
        }

        if (gpsLocationListener != null) {
            LOG.debug("Removing gpsLocationManager updates");
            gpsLocationManager.removeUpdates(gpsLocationListener);
            gpsLocationManager.removeGpsStatusListener(gpsLocationListener);
        }

        goblobLocationManager.setWaitingForLocation(false);
        EventBus.getDefault().post(new ServiceEvents.WaitingForLocation(false));

    }

    private void stopFusedLocation() {
        if (mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @SuppressWarnings("ResourceType")
    private void stopPassiveManager() {
        if (passiveLocationManager != null) {
            LOG.debug("Removing passiveLocationManager updates");
            passiveLocationManager.removeUpdates(passiveLocationListener);
        }
    }

    /**
     * Sets the current file name based on user preference.
     */
    private void resetCurrentFileName(boolean newLogEachStart) {

        String oldFileName = goblobLocationManager.getCurrentFormattedFileName();

        /* Update the file name, if required. (New day, Re-start service) */
        if (preferenceHelper.shouldCreateCustomFile()) {
            if (Strings.isNullOrEmpty(Strings.getFormattedFileName())) {
                goblobLocationManager.setCurrentFileName(preferenceHelper.getCustomFileName());
            }

            LOG.debug("Should change file name dynamically: " + preferenceHelper.shouldChangeFileNameDynamically());

            if (!preferenceHelper.shouldChangeFileNameDynamically()) {
                goblobLocationManager.setCurrentFileName(Strings.getFormattedFileName());
            }

        } else if (preferenceHelper.shouldCreateNewFileOnceADay()) {
            // 20100114.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            goblobLocationManager.setCurrentFileName(sdf.format(new Date()));
        } else if (newLogEachStart) {
            // 20100114183329.gpx
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            goblobLocationManager.setCurrentFileName(sdf.format(new Date()));
        }

        if (!Strings.isNullOrEmpty(oldFileName)
                && !oldFileName.equalsIgnoreCase(Strings.getFormattedFileName())
                && goblobLocationManager.isStarted()) {
            LOG.debug("New file name, should auto upload the old one");
            EventBus.getDefault().post(new CommandEvents.AutoSend(oldFileName));
        }

        goblobLocationManager.setCurrentFormattedFileName(Strings.getFormattedFileName());

        LOG.info("Filename: " + Strings.getFormattedFileName());
        EventBus.getDefault().post(new ServiceEvents.FileNamed(Strings.getFormattedFileName()));

    }


    void setLocationServiceUnavailable() {
        EventBus.getDefault().post(new ServiceEvents.LocationServicesUnavailable());
    }

    /**
     * Stops location manager, then starts it.
     */
    public void restartGpsManagers() {
        LOG.debug("Restarting location managers");
        stopGpsManager();
        startGpsManager();
    }

    /**
     * This event is raised when the GeneralLocationListener has a new location.
     * This method in turn updates notification, writes to file, re-obtains
     * preferences, notifies main service client and resets location managers.
     *
     * @param loc LocationParameters object
     */
    void onLocationChanged(Location loc) {
        if (!goblobLocationManager.isStarted()) {
            LOG.debug("onLocationChanged called, but goblobLocationManager.isStarted is false");
            stopLogging();
            return;
        }

        if (loc == null ||
                Double.isNaN(loc.getLatitude()) ||
                Double.isNaN(loc.getLongitude())) {
            return;
        }

        if (bestLocation == null) {
            bestLocation = loc;
        } else if (loc.getAccuracy() <= bestLocation.getAccuracy()) {
            bestLocation = loc;
        }

        loc = bestLocation;

        double speed = -1;
        double distanceTravelled = -1;
        if (goblobLocationManager.getCurrentLocationInfo() != null) {
            distanceTravelled = Maths.calculateDistance(loc.getLatitude(), loc.getLongitude(), goblobLocationManager.getCurrentLocationInfo().getLatitude(), goblobLocationManager.getCurrentLocationInfo().getLongitude());
            long timeDifference = Math.abs(loc.getTime() - goblobLocationManager.getCurrentLocationInfo().getTime()) / 1000;

            if (timeDifference != 0) {
                speed = distanceTravelled / timeDifference;
            }

            if (speed > 357) { //357 m/s ~=  1285 km/h
                bestLocation = null;
                LOG.warn(String.format("Very large jump detected - %d meters in %d sec - discarding point", (long) distanceTravelled, timeDifference));
                return;
            }

            if (distanceTravelled != -1 && distanceTravelled < 10) {
                if (loc.getAccuracy() >= goblobLocationManager.getCurrentLocationInfo().getAccuracy()) {
                    bestLocation = null;
                    return;
                }
            }

            if (timeDifference < 1) {
                return;
            }

            loc.setSpeed((float) speed);
        }

        long currentTimeStamp = System.currentTimeMillis();

        LOG.debug("Has description? " + goblobLocationManager.hasDescription() + ", Single point? " + goblobLocationManager.isSinglePointMode() + ", Last timestamp: " + goblobLocationManager.getLatestTimeStamp());

        // Don't log a point until the user-defined time has elapsed
        // However, if user has set an annotation, just log the point, disregard any filters
        if (!goblobLocationManager.hasDescription() && !goblobLocationManager.isSinglePointMode() && (currentTimeStamp - goblobLocationManager.getLatestTimeStamp()) < (preferenceHelper.getMinimumLoggingInterval() * 1000)) {
            //return;
        }

        //Don't log a point if user has been still
        // However, if user has set an annotation, just log the point, disregard any filters
        if (userHasBeenStillForTooLong()) {
            LOG.info("Received location but the user hasn't moved, ignoring");
            return;
        }

        if (!isFromValidListener(loc)) {
            return;
        }


        boolean isPassiveLocation = loc.getExtras().getBoolean(BundleConstants.PASSIVE);

        //check if we change of day and then write the last position of yesterday as the first position of today
        if (preferenceHelper.shouldCreateNewFileOnceADay()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String today = sdf.format(new Date());
            if (!today.equals(Strings.getFormattedFileName())) {
                resetCurrentFileName(false);
            }
        }

        //Check if a ridiculous distance has been travelled since previous point - could be a bad GPS jump
        /*if(goblobLocationManager.getCurrentLocationInfo() != null){
            double distanceTravelled = Maths.calculateDistance(loc.getLatitude(), loc.getLongitude(), goblobLocationManager.getCurrentLocationInfo().getLatitude(), goblobLocationManager.getCurrentLocationInfo().getLongitude());
            long timeDifference = (int)Math.abs(loc.getTime() - goblobLocationManager.getCurrentLocationInfo().getTime())/1000;

            if( timeDifference > 0 && (distanceTravelled/timeDifference) > 357){ //357 m/s ~=  1285 km/h
                LOG.warn(String.format("Very large jump detected - %d meters in %d sec - discarding point", (long)distanceTravelled, timeDifference));
                return;
            }
        }*/

        // Don't do anything until the user-defined accuracy is reached
        // However, if user has set an annotation, just log the point, disregard any filters
        /*if (!goblobLocationManager.hasDescription() &&  preferenceHelper.getMinimumAccuracy() > 0) {


            if(!loc.hasAccuracy() || loc.getAccuracy() == 0){
                return;
            }

            //Don't apply the retry interval to passive locations
            if (!isPassiveLocation && preferenceHelper.getMinimumAccuracy() < Math.abs(loc.getAccuracy())) {

                if(goblobLocationManager.getFirstRetryTimeStamp() == 0){
                    goblobLocationManager.setFirstRetryTimeStamp(System.currentTimeMillis());
                }

                if (currentTimeStamp - goblobLocationManager.getFirstRetryTimeStamp() <= preferenceHelper.getLoggingRetryPeriod() * 1000) {
                    LOG.warn("Only accuracy of " + String.valueOf(loc.getAccuracy()) + " m. Point discarded." + getString(R.string.inaccurate_point_discarded));
                    //return and keep trying
                    return;
                }

                if (currentTimeStamp - goblobLocationManager.getFirstRetryTimeStamp() > preferenceHelper.getLoggingRetryPeriod() * 1000) {
                    LOG.warn("Only accuracy of " + String.valueOf(loc.getAccuracy()) + " m and timeout reached." + getString(R.string.inaccurate_point_discarded));
                    //Give up for now
                    stopManagerAndResetAlarm();

                    //reset timestamp for next time.
                    goblobLocationManager.setFirstRetryTimeStamp(0);
                    return;
                }

                //Success, reset timestamp for next time.
                goblobLocationManager.setFirstRetryTimeStamp(0);
            }
        }*/

        //Don't do anything until the user-defined distance has been traversed
        // However, if user has set an annotation, just log the point, disregard any filters
        /*if (!goblobLocationManager.hasDescription() && !goblobLocationManager.isSinglePointMode() && preferenceHelper.getMinimumDistanceInterval() > 0 && goblobLocationManager.hasValidLocation()) {

            double distanceTraveled = Maths.calculateDistance(loc.getLatitude(), loc.getLongitude(),
                    goblobLocationManager.getCurrentLatitude(), goblobLocationManager.getCurrentLongitude());

            if (preferenceHelper.getMinimumDistanceInterval() > distanceTraveled) {
                LOG.warn(String.format(getString(R.string.not_enough_distance_traveled), String.valueOf(Math.floor(distanceTraveled))) + ", point discarded");
                stopManagerAndResetAlarm();
                return;
            }
        }*/


        LOG.info(SessionLogcatAppender.MARKER_LOCATION, String.valueOf(loc.getLatitude()) + "," + String.valueOf(loc.getLongitude()));
        loc = goblobLocationManager.getLocationOnRoute(Locations.getLocationWithAdjustedAltitude(loc, preferenceHelper));
        if (loc.getExtras().getBoolean("repeatedPoint", false)) {
            return;
        }
        resetCurrentFileName(false);
        goblobLocationManager.setLatestTimeStamp(System.currentTimeMillis());
        goblobLocationManager.setFirstRetryTimeStamp(0);
        goblobLocationManager.setCurrentLocationInfo(loc);
        setDistanceTraveled(loc);
        showNotification();

        if (isPassiveLocation) {
            LOG.debug("Logging passive location to file");
        }

        writeToFile(loc);
        //resetAutoSendTimersIfNecessary();
        stopManagerAndResetAlarm();

        EventBus.getDefault().post(new ServiceEvents.LocationUpdate(loc));

        GoblobLocationManager.getInstance().saveLiveLocation(loc);

        if (goblobLocationManager.isSinglePointMode()) {
            LOG.debug("Single point mode - stopping now");
            stopLogging();
        }
        bestLocation = null;
    }

    private boolean isFromValidListener(Location loc) {

        if (!preferenceHelper.getChosenListeners().contains(LocationManager.GPS_PROVIDER) && !preferenceHelper.getChosenListeners().contains(LocationManager.NETWORK_PROVIDER)) {
            return true;
        }

        if (!preferenceHelper.getChosenListeners().contains(LocationManager.NETWORK_PROVIDER)) {
            return loc.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER);
        }

        if (!preferenceHelper.getChosenListeners().contains(LocationManager.GPS_PROVIDER)) {
            return !loc.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER);
        }

        return true;
    }

    private void setDistanceTraveled(Location loc) {
        // Distance
        if (goblobLocationManager.getPreviousLocationInfo() == null) {
            goblobLocationManager.setPreviousLocationInfo(loc);
        }
        // Calculate this location and the previous location location and add to the current running total distance.
        // NOTE: Should be used in conjunction with 'distance required before logging' for more realistic values.
        double distance = Maths.calculateDistance(
                goblobLocationManager.getPreviousLatitude(),
                goblobLocationManager.getPreviousLongitude(),
                loc.getLatitude(),
                loc.getLongitude());
        goblobLocationManager.setPreviousLocationInfo(loc);
        goblobLocationManager.setTotalTravelled(goblobLocationManager.getTotalTravelled() + distance);
    }

    protected void stopManagerAndResetAlarm() {
        /*if (!preferenceHelper.shouldKeepGPSOnBetweenFixes()) {
            stopGpsManager();
        }*/

        stopAbsoluteTimer();
        setAlarmForNextPoint();
    }


    private void stopAlarm() {
        Intent i = new Intent(this, GoblobLocationService.class);
        i.putExtra(IntentConstants.GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);
    }

    private void stopAlarmForNextRoutingAttempt(boolean update) {
        Intent serviceIntent = new Intent(this, GoblobLocationService.class);
        serviceIntent.putExtra(IntentConstants.ROUTING, true);
        serviceIntent.putExtra(IntentConstants.UPDATE, update);
        PendingIntent pi = PendingIntent.getService(this, 0, serviceIntent, 0);
        nextPointAlarmManager.cancel(pi);
    }

    @TargetApi(23)
    private void setAlarmForNextRoutingAttempt(boolean update) {
        LOG.debug("Set alarm for " + preferenceHelper.getMinimumLoggingInterval() + " seconds");

        Intent serviceIntent = new Intent(this, GoblobLocationService.class);
        serviceIntent.putExtra(IntentConstants.ROUTING, true);
        serviceIntent.putExtra(IntentConstants.UPDATE, update);
        PendingIntent pi = PendingIntent.getService(this, 0, serviceIntent, 0);
        nextPointAlarmManager.cancel(pi);

        if (Systems.isDozing(this)) {
            //Only invoked once per 15 minutes in doze mode
            LOG.warn("Device is dozing, using infrequent alarm");
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pi);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pi);
        }
    }

    @TargetApi(23)
    private void setAlarmForNextPoint() {
        LOG.debug("Set alarm for " + preferenceHelper.getMinimumLoggingInterval() + " seconds");

        Intent i = new Intent(this, GoblobLocationService.class);
        i.putExtra(IntentConstants.GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        if (Systems.isDozing(this)) {
            //Only invoked once per 15 minutes in doze mode
            LOG.warn("Device is dozing, using infrequent alarm");
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pi);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pi);
        }
    }


    /**
     * Calls file helper to write a given location to a file.
     *
     * @param loc LocationParameters object
     */
    private void writeToFile(Location loc) {
        goblobLocationManager.setAddNewTrackSegment(false);

        try {
            LOG.debug("Calling file writers");
            FileLoggerFactory.write(getApplicationContext(), loc);

            if (goblobLocationManager.hasDescription()) {
                LOG.info("Writing annotation: " + goblobLocationManager.getDescription());
                FileLoggerFactory.annotate(getApplicationContext(), goblobLocationManager.getDescription(), loc);
            }
        } catch (Exception e) {
            LOG.error(getString(R.string.could_not_write_to_file), e);
        }

        goblobLocationManager.clearDescription();
        EventBus.getDefault().post(new ServiceEvents.AnnotationStatus(true));
    }

    /**
     * Informs the main service client of the number of visible satellites.
     *
     * @param count Number of Satellites
     */
    void setSatelliteInfo(int count) {
        goblobLocationManager.setVisibleSatelliteCount(count);
        EventBus.getDefault().post(new ServiceEvents.SatellitesVisible(count));
    }

    public void onNmeaSentence(long timestamp, String nmeaSentence) {

        if (preferenceHelper.shouldLogToNmea()) {
            NmeaFileLogger nmeaLogger = new NmeaFileLogger(Strings.getFormattedFileName());
            nmeaLogger.write(timestamp, nmeaSentence);
        }
    }

    @SuppressLint("MissingPermission")
    public LiveData<Location> getLastLocation() {
        MutableLiveData<Location> data = new MutableLiveData<>();

        LocationEngineProvider.getBestLocationEngine(getApplicationContext()).getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                if (result == null || result.getLastLocation() == null) {
                    return;
                }

                Location location = result.getLastLocation();

                location = Locations.getLocationWithAdjustedAltitude(location, preferenceHelper);

                Bundle b = new Bundle();
                b.putBoolean(BundleConstants.LAST_LOCATION, true);
                location.setExtras(b);

                if (goblobLocationManager.isStarted()) {
                    goblobLocationManager.setCurrentLocationInfo(location);
                    showNotification();
                }

                location = goblobLocationManager.getLocationOnRoute(location);

                EventBus.getDefault().post(new ServiceEvents.LocationUpdate(location));

                data.setValue(location);
            }

            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });
        return data;
    }

    private void traceRoute(Routing.TravelMode travelMode, LatLng origin, LatLng destination, boolean update) {
        if (!Systems.isNetworkAvailable(GoblobLocationManager.getInstance().getApplicationContext())) {
            EventBus.getDefault().post(new GoblobEvents.Error(new GoblobException(GoblobException.NETWORK_ERROR)));
            return;
        }

        Routing routing = new Routing.Builder()
                .travelMode(travelMode)
                .alternativeRoutes(true)
                .key(GoblobLocationManager.getInstance().getApplicationContext().getResources().getString(R.string.google_maps_key))
                .withListener(new RoutingListener() {
                    @Override
                    public void onRoutingFailure(RouteException e) {
                        if (e != null) {
                            e.printStackTrace();
                        }
                        if (routeAttempt < 5) {
                            routeAttempt++;
                            setAlarmForNextRoutingAttempt(update);
                        } else {
                            routeAttempt = 0;
                            EventBus.getDefault().post(new GoblobEvents.Error(new GoblobException(GoblobException.NETWORK_ERROR)));
                        }
                    }

                    @Override
                    public void onRoutingStart() {
                        EventBus.getDefault().post(new RouteEvents.Routing(update, origin, destination));
                    }

                    @Override
                    public void onRoutingSuccess(List<Route> routeList, int shortestRouteIndex) {
                        //EventBus.getDefault().post(new RouteEvents.RoutingSuccess(route, shortestRouteIndex));
                        if (routeList != null && routeList.size() > 0) {
                            goblobLocationManager.setRoutes(routeList, shortestRouteIndex, update, routeAttempt);
                        } else {
                            Log.d("onPostExecute", "without Polylines drawn");
                            if (routeAttempt < 5) {
                                routeAttempt++;
                                setAlarmForNextRoutingAttempt(update);
                            } else {
                                routeAttempt = 0;
                            }
                        }
                    }

                    @Override
                    public void onRoutingCancelled() {
                        Log.e("onRoutingCancelled", "***************************onRoutingCancelled");
                    }
                })
                .waypoints(origin, destination)
                .build();
        routing.execute();
    }

    @EventBusHook
    public void onEvent(CommandEvents.RequestToggle requestToggle) {
        if (goblobLocationManager.isStarted()) {
            stopLogging();
        } else {
            startLogging();
        }
    }

    @EventBusHook
    public void onEvent(CommandEvents.RequestStartStop startStop) {
        goblobLocationManager.setFirstRetryTimeStamp(0);
        if (startStop.start) {
            startLogging();
        } else {
            stopLogging();
        }
        EventBus.getDefault().removeStickyEvent(CommandEvents.RequestStartStop.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.GetStatus getStatus) {
        CommandEvents.GetStatus statusEvent = EventBus.getDefault().removeStickyEvent(CommandEvents.GetStatus.class);
        if (statusEvent != null) {
            notifyStatus(goblobLocationManager.isStarted());
        }

    }

    @EventBusHook
    public void onEvent(CommandEvents.AutoSend autoSend) {
        autoSendLogFile(autoSend.formattedFileName);

        EventBus.getDefault().removeStickyEvent(CommandEvents.AutoSend.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.Annotate annotate) {
        final String desc = annotate.annotation;
        if (desc.length() == 0) {
            LOG.debug("Clearing annotation");
            goblobLocationManager.clearDescription();
        } else {
            LOG.debug("Pending annotation: " + desc);
            goblobLocationManager.setDescription(desc);
            EventBus.getDefault().post(new ServiceEvents.AnnotationStatus(false));

            if (goblobLocationManager.isStarted()) {
                startGpsManager();
            } else {
                logOnce();
            }
        }

        EventBus.getDefault().removeStickyEvent(CommandEvents.Annotate.class);
    }

    @EventBusHook
    public void onEvent(CommandEvents.LogOnce logOnce) {
        logOnce();
    }

    @EventBusHook
    public void onEvent(ServiceEvents.ActivityRecognitionEvent activityRecognitionEvent) {

        goblobLocationManager.setLatestDetectedActivity(activityRecognitionEvent.result.getMostProbableActivity());

        if (!preferenceHelper.shouldNotLogIfUserIsStill()) {
            goblobLocationManager.setUserStillSinceTimeStamp(0);
            return;
        }

        if (activityRecognitionEvent.result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
            LOG.debug(activityRecognitionEvent.result.getMostProbableActivity().toString());
            if (goblobLocationManager.getUserStillSinceTimeStamp() == 0) {
                LOG.debug("Just entered still state, attempt to log");
                startGpsManager();
                goblobLocationManager.setUserStillSinceTimeStamp(System.currentTimeMillis());
            }

        } else {
            LOG.debug(activityRecognitionEvent.result.getMostProbableActivity().toString());
            //Reset the still-since timestamp
            goblobLocationManager.setUserStillSinceTimeStamp(0);
            LOG.debug("Just exited still state, attempt to log");
            startGpsManager();
        }
    }

    @EventBusHook
    public void onEvent(ProfileEvents.SwitchToProfile switchToProfileEvent) {
        try {

            if (preferenceHelper.getCurrentProfileName().equals(switchToProfileEvent.newProfileName)) {
                return;
            }

            LOG.debug("Switching to profile: " + switchToProfileEvent.newProfileName);

            //Save the current settings to a file (overwrite)
            File f = new File(Files.storageFolder(GoblobLocationService.this), preferenceHelper.getCurrentProfileName() + ".properties");
            preferenceHelper.savePropertiesFromPreferences(f);

            //Read from a possibly existing file and load those preferences in
            File newProfile = new File(Files.storageFolder(GoblobLocationService.this), switchToProfileEvent.newProfileName + ".properties");
            if (newProfile.exists()) {
                preferenceHelper.setPreferenceFromPropertiesFile(newProfile);
            }

            //Switch current profile name
            preferenceHelper.setCurrentProfileName(switchToProfileEvent.newProfileName);

        } catch (IOException e) {
            LOG.error("Could not save profile to file", e);
        }
    }

    /**
     * Can be used from calling classes as the go-between for methods and
     * properties.
     */
    public class GpsLoggingBinder extends Binder {
        public GoblobLocationService getService() {
            return GoblobLocationService.this;
        }
    }

}
