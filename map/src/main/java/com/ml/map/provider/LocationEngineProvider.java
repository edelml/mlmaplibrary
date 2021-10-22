package com.ml.map.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * The main entry point for location engine integration.
 */
public final class LocationEngineProvider {
    private static final String GOOGLE_LOCATION_SERVICES = "com.google.android.gms.location.LocationServices";
    private static final String GOOGLE_API_AVAILABILITY = "com.google.android.gms.common.GoogleApiAvailability";

    private LocationEngineProvider() {
        // prevent instantiation
    }

    /**
     * Returns instance to the best location engine, given the included libraries.
     *
     * @param context    {@link Context}.
     * @param background true if background optimized engine is desired (note: parameter deprecated)
     * @return a unique instance of {@link LocationEngine} every time method is called.
     * @since 1.0.0
     */
    @NonNull
    @Deprecated
    public static LocationEngine getBestLocationEngine(@NonNull Context context, boolean background) {
        return getBestLocationEngine(context);
    }

    /**
     * Returns instance to the best location engine, given the included libraries.
     *
     * @param context    {@link Context}.
     * @return a unique instance of {@link LocationEngine} every time method is called.
     * @since 1.1.0
     */
    @NonNull
    public static LocationEngine getBestLocationEngine(@NonNull Context context) {
        checkNotNull(context, "context == null");

        boolean hasGoogleLocationServices = isOnClasspath(GOOGLE_LOCATION_SERVICES);
        if (isOnClasspath(GOOGLE_API_AVAILABILITY)) {
            // Check Google Play services APK is available and up-to-date on this device
            hasGoogleLocationServices &= GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                    == ConnectionResult.SUCCESS;
        }
        return getLocationEngine(context, hasGoogleLocationServices);
    }

    private static LocationEngine getLocationEngine(Context context, boolean isGoogle) {
        return isGoogle ? new LocationEngineProxy<>(new GoogleLocationEngineImpl(context.getApplicationContext())) :
                new LocationEngineProxy<>(new MapboxFusedLocationEngineImpl(context.getApplicationContext()));
    }

    /**
     * Checks if class is on class path
     * @param className of the class to check.
     * @return true if class in on class path, false otherwise.
     */
    static boolean isOnClasspath(String className) {
        boolean isOnClassPath = true;
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            isOnClassPath = false;
        }
        return isOnClassPath;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference object reference.
     * @param message   exception message to use if check fails.
     * @param <T>       object type.
     * @return validated non-null reference.
     */
    static <T> T checkNotNull(@Nullable T reference, String message) {
        if (reference == null) {
            throw new NullPointerException(message);
        }
        return reference;
    }
}
