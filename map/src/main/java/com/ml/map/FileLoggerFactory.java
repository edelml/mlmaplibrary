package com.ml.map;

import android.content.Context;
import android.location.Location;

import com.ml.map.csv.CSVFileLogger;
import com.ml.map.geojson.GeoJSONLogger;
import com.ml.map.gpx.Gpx10FileLogger;
import com.ml.map.gpx.Gpx11FileLogger;
import com.ml.map.kml.Kml22FileLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory {

    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static GoblobLocationManager goblobLocationManager = GoblobLocationManager.getInstance();

    public static List<FileLogger> getFileLoggers(Context context) {

        List<FileLogger> loggers = new ArrayList<>();

        if(Strings.isNullOrEmpty(preferenceHelper.getGoblobFolder())){
            return loggers;
        }

        File gpxFolder = new File(preferenceHelper.getGoblobFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        int batteryLevel = Systems.getBatteryLevel(context);

        if (preferenceHelper.shouldLogToGpx()) {
            File gpxFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".gpx");
            if(preferenceHelper.shouldLogAsGpx11()) {
                loggers.add(new Gpx11FileLogger(gpxFile, goblobLocationManager.shouldAddNewTrackSegment()));
            } else {
                loggers.add(new Gpx10FileLogger(gpxFile, goblobLocationManager.shouldAddNewTrackSegment()));
            }
        }

        if (preferenceHelper.shouldLogToKml()) {
            File kmlFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, goblobLocationManager.shouldAddNewTrackSegment()));
        }

        if (preferenceHelper.shouldLogToCSV()) {
            File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".csv");
            loggers.add(new CSVFileLogger(file, batteryLevel));
        }

        if (preferenceHelper.shouldLogToOpenGTS()) {
            //loggers.add(new OpenGTSLogger(context, batteryLevel));
        }

        if (preferenceHelper.shouldLogToCustomUrl()) {
            String androidId = Systems.getAndroidId(context);
            /*loggers.add(new CustomUrlLogger(preferenceHelper.getCustomLoggingUrl(), batteryLevel,
                    androidId, preferenceHelper.getCustomLoggingHTTPMethod(), preferenceHelper.getCustomLoggingHTTPBody(), preferenceHelper.getCustomLoggingHTTPHeaders()));*/
        }

        if(/* Should log to Android Wear */  true){
            //loggers.add(new AndroidWearLogger(context));
        }

        if(preferenceHelper.shouldLogToGeoJSON()){
            File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".geojson");
            loggers.add(new GeoJSONLogger(file, goblobLocationManager.shouldAddNewTrackSegment()));
        }


        return loggers;
    }

    public static void write(Context context, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.write(loc);
        }
    }

    public static void annotate(Context context, String description, Location loc) throws Exception {
        for (FileLogger logger : getFileLoggers(context)) {
            logger.annotate(description, loc);
        }
    }
}
