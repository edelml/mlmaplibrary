package com.ml.map;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;

public class Systems {

    public static boolean networkAvailable;
    public static boolean routeExists = true;

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 0;

        if (level == -1 || scale == -1) {
            return 50;
        }

        return (int) (((float) level / (float) scale) * 100.0f);
    }

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static boolean isPackageInstalled(String targetPackage, Context context) {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) return true;
        }
        return false;
    }


    /**
     * Check if there is any connectivity to a Wifi network
     *
     * @param context
     * @return
     */
    public static boolean isConnectedWifi(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     *
     * @param context
     * @return
     */
    public static boolean isConnectedMobile(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Check if there is fast connectivity
     *
     * @param context
     * @return
     */
    public static boolean isConnectedFast(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected() && isConnectionFast(info.getType(), info.getSubtype()));
    }

    /**
     * Check if the connection is fast
     *
     * @param type
     * @param subType
     * @return
     */
    public static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                /*
                 * Above API level 7, make sure to set android:targetSdkVersion
                 * to appropriate level to use these
                 */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        // note that this may return null if no network is currently active
        return cm.getActiveNetworkInfo();
    }

  /* public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return (info != null && info.isConnected());
    }
*/
    // TODO: probar si esto funciona asi.
    public static boolean isNetworkAvailable(Context context) {
        // Esto debe llamarse antes y si esta en Doze/Idle mode entonces no tiene conexion.
        if (isDozing(context)) return false;

        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        //If we aren't even associated with a network, we're done
        boolean connected = (null != activeNetworkInfo)
                && activeNetworkInfo.isConnected();
        if (!connected) return false;

        //Check if we can access a remote server
        new Runnable(){
            @Override
            public void run(){
                try {
                    //Check Google Public DNS
                    InetAddress host = InetAddress.getByName("8.8.8.8");
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(host, 53), 5000);
                    //It exists if no exception is thrown
                    routeExists = true;
                    s.close();
                } catch (IOException e) {
                    routeExists = false;
                }
            }
        };
        return (connected && routeExists);
    }


    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     * https://github.com/yigit/android-priority-jobqueue/blob/master/jobqueue/src/main/java/com/path/android/jobqueue/network/NetworkUtilImpl.java#L60
     */
    @TargetApi(23)
    public static boolean isDozing(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isDeviceIdleMode() &&
                    !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }


    public static boolean locationPermissionsGranted(Context context) {
        int fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);

        return fineCheck == PackageManager.PERMISSION_GRANTED && coarseCheck == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().length() == 0;
    }

    public static void setLocale(String userSpecifiedLocale, Context baseContext, Resources resources) {

        if (!isNullOrEmpty(userSpecifiedLocale)) {
            String language, country = "";

            if (userSpecifiedLocale.contains("-")) {
                language = userSpecifiedLocale.split("-")[0];
                country = userSpecifiedLocale.split("-")[1];
            } else {
                language = userSpecifiedLocale;
            }

            Locale locale = new Locale(language, country);
            Locale.setDefault(locale);
            resources.getConfiguration().locale = locale;
            baseContext.getResources().updateConfiguration(resources.getConfiguration(), baseContext.getResources().getDisplayMetrics());
        }
    }

    /**
     * Check if a service is running for a given Context
     *
     * @param context
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceClassName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClassName.equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if an app process is running for a given Context
     *
     * @param context
     * @return
     */
    public static boolean isAppProcessRunning(Context context, String appProcessName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (appProcessName.equals(processInfo.processName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
