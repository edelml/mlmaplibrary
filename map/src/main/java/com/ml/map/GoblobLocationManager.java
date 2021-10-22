package com.ml.map;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.GoblobPolyUtil;
import com.google.maps.android.RoutePoint;
import com.google.maps.android.SphericalUtil;
import com.ml.map.permissionsmanager.PermissionsManager;
import com.ml.map.route.AbstractRouting;
import com.ml.map.route.Route;
import com.ml.map.route.RouteEvents;
import com.ml.map.route.Segment;

import java.util.List;

import de.greenrobot.event.EventBus;

public class GoblobLocationManager {
    private static final String TAG = GoblobLocationManager.class.getSimpleName();
    private static GoblobLocationManager instance = null;
    private Context context;
    private SharedPreferences prefs;
    private Location previousLocationInfo;
    private Location currentLocationInfo;
    private RoutePoint routePoint;
    private boolean followRoute;
    private Thread simulateRouteThread;
    private boolean simulatinRoute;
    private Route currentRoute;
    private int recalculate;
    private LocationActive locationActive;

    private GoblobLocationManager() {
    }

    public void init(Context context){
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        DbHelper.init(context);
        PermissionsManager.init(context);
    }

    public static GoblobLocationManager getInstance() {
        if (instance == null) {
            instance = new GoblobLocationManager();
        }
        return instance;
    }

    public void loadCurrentRoute() {
        /*try {
            if (get("currentRoute", null) != null && !get("currentRoute", "").equalsIgnoreCase("")) {
                currentRoute = new Route(new JSONObject(get("currentRoute", "")));
                if (currentRoute != null) {
                    //parserTask(currentRoute.getRoute(), false);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    private String get(String key, String defaultValue) {
        return prefs.getString("SESSION_" + key, defaultValue);
    }

    private void set(String key, String value) {
        prefs.edit().putString("SESSION_" + key, value).apply();
    }


    public boolean isSinglePointMode() {
        return Boolean.valueOf(get("isSinglePointMode", "false"));
    }

    public void setSinglePointMode(boolean singlePointMode) {
        set("isSinglePointMode", String.valueOf(singlePointMode));
    }

    /**
     * @return whether GPS (tower) is enabled
     */
    public boolean isTowerEnabled() {
        return Boolean.valueOf(get("towerEnabled", "false"));
    }

    /**
     * @param towerEnabled set whether GPS (tower) is enabled
     */
    public void setTowerEnabled(boolean towerEnabled) {
        set("towerEnabled", String.valueOf(towerEnabled));
    }

    /**
     * @return whether GPS (satellite) is enabled
     */
    public boolean isGpsEnabled() {
        return Boolean.valueOf(get("gpsEnabled", "false"));
    }

    /**
     * @param gpsEnabled set whether GPS (satellite) is enabled
     */
    public void setGpsEnabled(boolean gpsEnabled) {
        set("gpsEnabled", String.valueOf(gpsEnabled));
    }

    /**
     * @return whether logging has started
     */
    public boolean isStarted() {
        return Boolean.valueOf(get("LOGGING_STARTED", "false"));
    }

    /**
     * @param isStarted set whether logging has started
     */
    public void setStarted(boolean isStarted) {

        set("LOGGING_STARTED", String.valueOf(isStarted));

        this.routePoint = null;

        if (isStarted) {
            set("startTimeStamp", String.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * @return the isUsingGps
     */
    public boolean isUsingGps() {
        return Boolean.valueOf(get("isUsingGps", "false"));
    }

    /**
     * @param isUsingGps the isUsingGps to set
     */
    public void setUsingGps(boolean isUsingGps) {
        set("isUsingGps", String.valueOf(isUsingGps));
    }

    /**
     * @return the currentFileName (without extension)
     */
    public String getCurrentFileName() {
        return get("currentFileName", "");
    }


    /**
     * @param currentFileName the currentFileName to set
     */
    public void setCurrentFileName(String currentFileName) {
        set("currentFileName", currentFileName);
    }

    /**
     * @return the number of satellites visible
     */
    public int getVisibleSatelliteCount() {
        return Integer.valueOf(get("satellites", "0"));
    }

    /**
     * @param satellites sets the number of visible satellites
     */
    public void setVisibleSatelliteCount(int satellites) {
        set("satellites", String.valueOf(satellites));
    }


    /**
     * @return the currentLatitude
     */
    public double getCurrentLatitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLatitude();
        } else {
            return 0;
        }
    }

    public double getPreviousLatitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLatitude() : 0;
    }

    public double getPreviousLongitude() {
        Location loc = getPreviousLocationInfo();
        return loc != null ? loc.getLongitude() : 0;
    }

    public double getTotalTravelled() {
        return Double.valueOf(get("totalTravelled", "0"));
    }

    public int getNumLegs() {
        return Integer.valueOf(get("numLegs", "0"));
    }

    public void setNumLegs(int numLegs) {
        set("numLegs", String.valueOf(numLegs));
    }

    public void setTotalTravelled(double totalTravelled) {
        if (totalTravelled == 0) {
            setNumLegs(1);
        } else {
            setNumLegs(getNumLegs() + 1);
        }
        set("totalTravelled", String.valueOf(totalTravelled));
    }

    public Location getPreviousLocationInfo() {
        return previousLocationInfo;
    }

    public void setPreviousLocationInfo(Location previousLocationInfo) {
        this.previousLocationInfo = previousLocationInfo;
    }


    /**
     * Determines whether a valid location is available
     */
    public boolean hasValidLocation() {
        return (getCurrentLocationInfo() != null && getCurrentLatitude() != 0 && getCurrentLongitude() != 0);
    }

    /**
     * @return the currentLongitude
     */
    public double getCurrentLongitude() {
        if (getCurrentLocationInfo() != null) {
            return getCurrentLocationInfo().getLongitude();
        } else {
            return 0;
        }
    }

    /**
     * @return the latestTimeStamp (for location info)
     */
    public long getLatestTimeStamp() {
        return Long.valueOf(get("latestTimeStamp", "0"));
    }

    /**
     * @return the timestamp when measuring was started
     */
    public long getStartTimeStamp() {
        return Long.valueOf(get("startTimeStamp", String.valueOf(System.currentTimeMillis())));
    }

    /**
     * @param latestTimeStamp the latestTimeStamp (for location info) to set
     */
    public void setLatestTimeStamp(long latestTimeStamp) {
        set("latestTimeStamp", String.valueOf(latestTimeStamp));
    }

    /**
     * @return whether to create a new track segment
     */
    public boolean shouldAddNewTrackSegment() {
        return Boolean.valueOf(get("addNewTrackSegment", "false"));
    }

    /**
     * @param addNewTrackSegment set whether to create a new track segment
     */
    public void setAddNewTrackSegment(boolean addNewTrackSegment) {
        set("addNewTrackSegment", String.valueOf(addNewTrackSegment));
    }

    /**
     * @param autoSendDelay the autoSendDelay to set
     */
    public void setAutoSendDelay(float autoSendDelay) {
        set("autoSendDelay", String.valueOf(autoSendDelay));
    }

    /**
     * @return the autoSendDelay to use for the timer
     */
    public float getAutoSendDelay() {
        return Float.valueOf(get("autoSendDelay", "0"));
    }

    /**
     * @param currentLocationInfo the latest LocationParameters class
     */
    public void setCurrentLocationInfo(Location currentLocationInfo) {
        this.currentLocationInfo = currentLocationInfo;
    }

    /**
     * @return the LocationParameters class containing latest lat-long information
     */
    public Location getCurrentLocationInfo() {
        return currentLocationInfo;

    }

    /**
     * @param isBound set whether the activity is bound to the GpsLoggingService
     */
    public void setBoundToService(boolean isBound) {
        set("isBound", String.valueOf(isBound));
    }

    /**
     * @return whether the activity is bound to the GpsLoggingService
     */
    public boolean isBoundToService() {
        return Boolean.valueOf(get("isBound", "false"));
    }

    public boolean hasDescription() {
        return !(getDescription().length() == 0);
    }

    public String getDescription() {
        return get("description", "");
    }

    public void clearDescription() {
        setDescription("");
    }

    public void setDescription(String newDescription) {
        set("description", newDescription);
    }

    public void setWaitingForLocation(boolean waitingForLocation) {
        set("waitingForLocation", String.valueOf(waitingForLocation));
    }

    public boolean isWaitingForLocation() {
        return Boolean.valueOf(get("waitingForLocation", "false"));
    }

    public boolean isAnnotationMarked() {
        return Boolean.valueOf(get("annotationMarked", "false"));
    }

    public void setAnnotationMarked(boolean annotationMarked) {
        set("annotationMarked", String.valueOf(annotationMarked));
    }

    public String getCurrentFormattedFileName() {
        return get("currentFormattedFileName", "");
    }

    public void setCurrentFormattedFileName(String currentFormattedFileName) {
        set("currentFormattedFileName", currentFormattedFileName);
    }

    public long getUserStillSinceTimeStamp() {
        return Long.valueOf(get("userStillSinceTimeStamp", "0"));
    }

    public void setUserStillSinceTimeStamp(long lastUserStillTimeStamp) {
        set("userStillSinceTimeStamp", String.valueOf(lastUserStillTimeStamp));
    }

    public void setFirstRetryTimeStamp(long firstRetryTimeStamp) {
        set("firstRetryTimeStamp", String.valueOf(firstRetryTimeStamp));
    }

    public long getFirstRetryTimeStamp() {
        return Long.valueOf(get("firstRetryTimeStamp", "0"));
    }

    public void setLatestDetectedActivity(DetectedActivity latestDetectedActivity) {
        set("latestDetectedActivity", Strings.getDetectedActivityName(latestDetectedActivity));
        if (latestDetectedActivity != null) {
            set("latestActivityConfidence", Integer.toString(latestDetectedActivity.getConfidence()));
            set("latestActivityType", Integer.toString(latestDetectedActivity.getType()));
        } else {
            set("latestActivityConfidence", "-1");
            set("latestActivityType", "-1");
        }
    }

    public int getLatestActivityType() {
        return Integer.parseInt(get("latestActivityType", "-1"));
    }

    public int getLatestActivityConfidence() {
        return Integer.parseInt(get("latestActivityConfidence", "-1"));
    }

    public String getLatestDetectedActivityName() {
        return get("latestDetectedActivity", "");
    }


    public void traceRoute(Context context, LatLng origin, LatLng destination, AbstractRouting.TravelMode travelMode, boolean update) {
        if (!Systems.isNetworkAvailable(context)) {
            EventBus.getDefault().post(new GoblobEvents.Error(new GoblobException(GoblobException.NETWORK_ERROR)));
            return;
        }
        Intent serviceIntent = new Intent(context, GoblobLocationService.class);
        serviceIntent.putExtra(IntentConstants.ROUTING, true);
        serviceIntent.putExtra(IntentConstants.UPDATE, update);
        serviceIntent.putExtra(IntentConstants.ORIGIN, origin);
        serviceIntent.putExtra(IntentConstants.DESTINATION, destination);
        serviceIntent.putExtra(IntentConstants.TRAVEL_MODE, travelMode);
        context.startService(serviceIntent);
    }

    public void setRoutes(List<Route> routeList, int shortestRouteIndex, boolean update, int routeAttempt) {
        /*PolylineOptions lineOptions = routeList.get(shortestRouteIndex).getPolyOptions();
        lineOptions.width(15);
        lineOptions.color(Color.RED);*/

        EventBus.getDefault().post(new GoblobEvents.NetworkConnected());

        //currentRoute.setRoute(route);

        if (update) {
            updateRoute();
        }

        //route = null;

        recalculate = 0;

        routePoint = null;

        EventBus.getDefault().post(new RouteEvents.ProcessRoute(routeList, shortestRouteIndex, update, routeAttempt));
    }

    private void updateRoute() {
        /*routePoint = null;

        if (liveRoute != null) {
            updateRoute1();
        } else {
            ParseQuery<ParseObject> liveRouteQuery = new ParseQuery("LiveRoute");
            liveRouteQuery.whereEqualTo("currentProfile", WebRtcClient.getInstance().getCurrentProfile());
            liveRouteQuery.getFirstInBackground(new com.parse.GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject object, ParseException e) {
                    liveRoute = object;
                    updateRoute1();
                }
            });
        }*/
    }

    private void updateRoute1() {
        /*if (liveRoute != null && currentRoute != null) {
            liveRoute.put("route", currentRoute.getRoute());
            liveRoute.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        e.printStackTrace();
                        liveRoute.saveEventually(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        }*/
    }

    public void saveCurrentRoute() {
        //set("currentRoute", currentRoute.toString());
    }

    public void deleteCurrentRoute() {
        set("currentRoute", "");
    }

    public Location getLocationOnRoute(Location bestLocation) {
        if (currentRoute != null) {
            boolean updateOrigin = currentRoute.updateOrigin(bestLocation);
            RoutePoint routePoint = GoblobPolyUtil.pointOnPath(new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude()), currentRoute.getPoints(), true, 40, this.routePoint != null ? this.routePoint.getPointPos() : 0);
            if (routePoint != null) {
                recalculate = 0;
                List<Segment> segmentList = currentRoute.getSegments();
                for (int i = 0; i < segmentList.size(); i++) {
                    if (routePoint.getPointPos() >= segmentList.get(i).getStartPointPos() && routePoint.getPointPos() <= segmentList.get(i).getEndPointPos()) {
                        routePoint.setSegmentPos(i);
                    }
                }
                if (routePoint.getPointPos() == currentRoute.getPoints().size() - 1) {
                    if (!isSimulatingRoute()) {
                        currentRoute.setRouteEnded(true);
                    }
                    EventBus.getDefault().post(new RouteEvents.SimulateRouteStartStop(false));
                    EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                }
                if (!Double.isNaN(routePoint.getLatLng().latitude)
                        && !Double.isNaN(routePoint.getLatLng().longitude)) {
                    LatLng pointOnPath = routePoint.getLatLng();

                    Bundle b = bestLocation.getExtras();
                    if (b == null) {
                        b = new Bundle();
                    }
                    if (this.routePoint != null) {
                        if (this.routePoint.getLatLng().latitude == routePoint.getLatLng().latitude
                                && this.routePoint.getLatLng().longitude == routePoint.getLatLng().longitude) {
                            b.putBoolean("repeatedPoint", true);
                        }
                        checkSegment(routePoint);
                    }

                    this.routePoint = routePoint;
                    bestLocation.setLatitude(pointOnPath.latitude);
                    bestLocation.setLongitude(pointOnPath.longitude);
                    bestLocation.setProvider("RoutePoint");
                    b.putString("routePoint", routePoint.toString());
                    bestLocation.setExtras(b);
                }
            } else if (updateOrigin && !isSimulatingRoute()) {
                if (recalculate == 3) {
                    traceRoute(context, currentRoute.getOrigin(), currentRoute.getDestination(), currentRoute.getTravelMode(), true);
                }
                recalculate++;
            }
        }
        return bestLocation;
    }

    private void checkSegment(RoutePoint routePoint) {
        List<Segment> segmentList = currentRoute.getSegments();

        if (this.routePoint.getSegmentPos() == routePoint.getSegmentPos()) {
            if (routePoint.getSegmentPos() + 1 < segmentList.size()) {
                double distance = SphericalUtil.computeDistanceBetween(routePoint.getLatLng(), segmentList.get(routePoint.getSegmentPos() + 1).startPoint());
                if (distance <= 100 && !this.routePoint.isA100Meters()) {
                    this.routePoint.set100Meters(true);
                    EventBus.getDefault().post(new RouteEvents.RouteDescription(segmentList.get(routePoint.getSegmentPos() + 1).getInstruction()));
                } else if (distance <= 500 && !this.routePoint.isA500Meters()) {
                    this.routePoint.set500Meters(true);
                    EventBus.getDefault().post(new RouteEvents.RouteDescription("In 500 meters " + segmentList.get(routePoint.getSegmentPos() + 1).getInstruction()));
                }
                routePoint.set100Meters(this.routePoint.isA100Meters());
                routePoint.set500Meters(this.routePoint.isA500Meters());
            }
        } else {
            EventBus.getDefault().post(new RouteEvents.RouteDescription(segmentList.get(routePoint.getSegmentPos()).getInstruction()));
        }
    }

    public boolean isFollowRoute() {
        return followRoute;
    }

    public void setFollowRoute(boolean followRoute) {
        this.followRoute = followRoute;
    }

    public void simulateRoute(boolean b) {
        if (!simulatinRoute && !b) {
            if (simulateRouteThread != null) {
                simulateRouteThread.interrupt();
                simulateRouteThread = null;
            }
            return;
        } else if (simulatinRoute && b) {
            return;
        }

        simulatinRoute = b;
        if (b) {
            routePoint = null;
            List<Segment> segmentList = currentRoute.getSegments();
            if (segmentList.size() > 0) {
                EventBus.getDefault().post(new RouteEvents.RouteDescription(segmentList.get(0).getInstruction()));
            }
            EventBus.getDefault().post(new RouteEvents.SimulateRouteStartStop(true));
            List<LatLng> points = currentRoute.getPoints();
            simulateRouteThread = new Thread() {
                public void run() {
                    for (int i = 0; i < points.size() && simulatinRoute; i++) {
                        LatLng point = points.get(i);
                        Location loc = new Location("simulateRouteLocation");
                        loc.setLatitude(point.latitude);
                        loc.setLongitude(point.longitude);
                        loc.setBearing(0);
                        loc.setAccuracy(1);
                        if (i + 1 < points.size()) {
                            Bundle b = new Bundle();
                            b.putString("routePoint", new RoutePoint(point, i, true, points.get(i + 1)).toString());
                            loc.setExtras(b);
                        }
                        EventBus.getDefault().post(new ServiceEvents.LocationUpdate(getLocationOnRoute(loc)));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    simulatinRoute = false;
                    followRoute = false;
                    if (currentRoute != null) {
                        currentRoute.setOrigin(currentRoute.getStartPoint());
                    }
                    EventBus.getDefault().post(new RouteEvents.SimulateRouteStartStop(false));
                    simulateRoute(false);
                }
            };
            simulateRouteThread.start();
        }
    }

    public boolean isSimulatingRoute() {
        return simulatinRoute;
    }

    public void clearRoute() {
        currentRoute = null;
        routePoint = null;
    }

    public void setCurrentRoute(Route currentRoute) {
        this.currentRoute = currentRoute;
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    /**
     * Funcion que recibe una location y pregunta por todos los profiles_to_share que esten almacenados localmente
     * Para cada profile que este compartiendo se manda a salvar la liveLocation usando una funcion en la cloud
     * que se llama saveLiveLocation
     *
     * @param loc Location que se recibe de los sensores del device
     */
    public void saveLiveLocation(Location loc) {

    }

    public void requestLocationActive(){
        if (locationActive != null){
            if (isLocationEnable()) {
                locationActive.active();
            } else {
                locationActive.disable();
            }
        }
    }

    public boolean isLocationEnable(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isLocationEnabled();
        }
        ContentResolver contentResolver = context.getContentResolver();
        int mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        return mode != Settings.Secure.LOCATION_MODE_OFF;
    }

    private void checkLocationActive(Activity activity, LocationActive locationActive) {
        this.locationActive = locationActive;

        if (isLocationEnable()){
            locationActive.active();
        } else {
            checkLocationSettings(activity);
        }
    }

    /**
     * Check if the device's location settings are adequate for the app's needs using the
     *
     */
    private void checkLocationSettings(final Activity activity) {
        Log.e(TAG, "checkLocationSettings");
        LocationRequest mLocationRequest = new LocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(context).checkLocationSettings(builder.build());
        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        GoblobLocationManager.getInstance().requestLocationActive();
                    }
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // LocationParameters settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(activity, 2003);
                                break;
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // LocationParameters settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.

                            break;
                    }
                }
            }
        });
    }

    public Context getApplicationContext() {
        return context;
    }

    public void startLocationService() {
        Intent serviceIntent = new Intent(context, GoblobLocationService.class);
        serviceIntent.putExtra(IntentConstants.IMMEDIATE_START, true);
        context.startService(serviceIntent);
    }

    public void stopLocationService(){
        Intent serviceIntent = new Intent(context, GoblobLocationService.class);
        serviceIntent.putExtra(IntentConstants.IMMEDIATE_STOP, true);
        context.startService(serviceIntent);
    }

    /**
     * Whether to display certain values using imperial units
     */
    @ProfilePreference(name= PreferenceNames.DISPLAY_IMPERIAL)
    public boolean shouldDisplayImperialUnits() {
        return prefs.getBoolean(PreferenceNames.DISPLAY_IMPERIAL, false);
    }
}
