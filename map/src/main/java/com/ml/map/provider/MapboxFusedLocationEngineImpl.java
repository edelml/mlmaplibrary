package com.ml.map.provider;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mapbox replacement for Google Play Services Fused Location Client
 * <p>
 * Note: fusion will not work in background mode.
 */
class MapboxFusedLocationEngineImpl extends AndroidLocationEngineImpl {
    private static final String TAG = "MapboxLocationEngine";

    MapboxFusedLocationEngineImpl(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public LocationListener createListener(LocationEngineCallback<LocationEngineResult> callback) {
        return new MapboxLocationEngineCallbackTransport(callback);
    }

    @Override
    public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
        Location bestLastLocation = getBestLastLocation();
        if (bestLastLocation != null) {
            callback.onSuccess(LocationEngineResult.create(bestLastLocation));
        } else {
            callback.onFailure(new Exception("Last location unavailable"));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                       @NonNull LocationListener listener,
                                       @Nullable Looper looper) throws SecurityException {
        super.requestLocationUpdates(request, listener, looper);

        // Start network provider along with gps
        if (shouldStartNetworkProvider(request.getPriority())) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        request.getInterval(), request.getDisplacement(),
                        listener, looper);
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                       @NonNull PendingIntent pendingIntent) throws SecurityException {
        super.requestLocationUpdates(request, pendingIntent);

        // Start network provider along with gps
        if (shouldStartNetworkProvider(request.getPriority())) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, request.getInterval(),
                        request.getDisplacement(), pendingIntent);
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
        }
    }

    private Location getBestLastLocation() {
        Location bestLastLocation = null;
        for (String provider : locationManager.getAllProviders()) {
            Location location = getLastLocationFor(provider);
            if (location == null) {
                continue;
            }

            if (MapboxLocationEngineCallbackTransport.isBetterLocation(location, bestLastLocation)) {
                bestLastLocation = location;
            }
        }
        return bestLastLocation;
    }

    private boolean shouldStartNetworkProvider(int priority) {
        return (priority == LocationEngineRequest.PRIORITY_HIGH_ACCURACY
                || priority == LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                && currentProvider.equals(LocationManager.GPS_PROVIDER);
    }

    private static final class MapboxLocationEngineCallbackTransport implements LocationListener {
        private final LocationEngineCallback<LocationEngineResult> callback;
        private Location currentBestLocation;

        MapboxLocationEngineCallbackTransport(LocationEngineCallback<LocationEngineResult> callback) {
            this.callback = callback;
        }

        /**
         * Checks whether two providers are the same
         * <p>
         * (c) https://developer.android.com/guide/topics/location/strategies
         */
        private static boolean isSameProvider(String provider1, String provider2) {
            if (provider1 == null) {
                return provider2 == null;
            }
            return provider1.equals(provider2);
        }

        private static final int TWO_MINUTES = 1000 * 60 * 2;
        private static final int ACCURACY_THRESHOLD_METERS = 200;

        /**
         * Determines whether one Location reading is better than the current Location fix
         * <p>
         * (c) https://developer.android.com/guide/topics/location/strategies
         *
         * @param location            The new Location that you want to evaluate
         * @param currentBestLocation The current Location fix, to which you want to compare the new one
         */
        static boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > ACCURACY_THRESHOLD_METERS;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(location.getProvider(),
                    currentBestLocation.getProvider());

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true;
            } else if (isNewer && !isLessAccurate) {
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
            }
            return false;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, currentBestLocation)) {
                currentBestLocation = location;
            }

            if (callback != null) {
                callback.onSuccess(LocationEngineResult.create(currentBestLocation));
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }
    }
}
