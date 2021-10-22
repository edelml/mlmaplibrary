/*
 * Copyright (C) 2016 goblob
 *
 * This file is part of Goblob for Android.
 *
 * Goblob for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Goblob for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Goblob for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ml.map.csv;

import android.location.Location;
import androidx.annotation.Nullable;

import com.ml.map.BundleConstants;
import com.ml.map.FileLogger;
import com.ml.map.Files;
import com.ml.map.GoblobLocationManager;
import com.ml.map.Maths;
import com.ml.map.Strings;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

public class CSVFileLogger implements FileLogger {

    private final Integer batteryLevel;
    private File file;
    protected final String name = "TXT";

    public CSVFileLogger(File file, @Nullable Integer batteryLevel) {
        this.file = file;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public void write(Location loc) throws Exception {
        if (!GoblobLocationManager.getInstance().hasDescription()) {
            annotate("", loc);
        }
    }

    String getCsvLine(Location loc, String dateTimeString) {
        return getCsvLine("", loc, dateTimeString);
    }

    String getCsvLine(String description, Location loc, String dateTimeString) {

        if (description.length() > 0) {
            description = "\"" + description.replaceAll("\"", "\"\"") + "\"";
        }

        String outputString = String.format(Locale.US, "%s,%f,%f,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", dateTimeString,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.hasAltitude() ? loc.getAltitude() : "",
                loc.hasAccuracy() ? loc.getAccuracy() : "",
                loc.hasBearing() ? loc.getBearing() : "",
                loc.hasSpeed() ? loc.getSpeed() : "",
                Maths.getBundledSatelliteCount(loc),
                loc.getProvider(),
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.HDOP))) ? loc.getExtras().getString(BundleConstants.HDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.VDOP))) ? loc.getExtras().getString(BundleConstants.VDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.PDOP))) ? loc.getExtras().getString(BundleConstants.PDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.GEOIDHEIGHT))) ? loc.getExtras().getString(BundleConstants.GEOIDHEIGHT) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA))) ? loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DGPSID))) ? loc.getExtras().getString(BundleConstants.DGPSID) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY))) ? loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY) : "",
                (batteryLevel != null) ? batteryLevel : "",
                description
        );
        return outputString;
    }

    @Override
    public void annotate(String description, Location loc) throws Exception {
        if (!Files.reallyExists(file)) {
            file.createNewFile();

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);
            String header = "time,lat,lon,elevation,accuracy,bearing,speed,satellites,provider,hdop,vdop,pdop,geoidheight,ageofdgpsdata,dgpsid,activity,battery,annotation\n";
            output.write(header.getBytes());
            output.flush();
            output.close();

        }

        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

        String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
        String csvLine = getCsvLine(description, loc, dateTimeString);


        output.write(csvLine.getBytes());
        output.flush();
        output.close();
        Files.addToMediaDatabase(file, "text/csv");
    }

    @Override
    public String getName() {
        return name;
    }

}
