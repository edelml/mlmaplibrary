package com.ml.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NmeaFileLogger {

    protected final static Object lock = new Object();
    String fileName;
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(10), new RejectionHandler());

    private PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private GoblobLocationManager goblobLocationManager = GoblobLocationManager.getInstance();

    public NmeaFileLogger(String fileName) {
        this.fileName = fileName;
    }

    public void write(long timestamp, String nmeaSentence)  {

        File gpxFolder = new File(preferenceHelper.getGoblobFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        File nmeaFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".nmea");

        if (!nmeaFile.exists()) {
            try {
                nmeaFile.createNewFile();
            } catch (IOException e) {

            }
        }

        NmeaWriteHandler writeHandler = new NmeaWriteHandler(nmeaFile, nmeaSentence);
        EXECUTOR.execute(writeHandler);
    }
}

class NmeaWriteHandler implements Runnable {

    File gpxFile;
    String nmeaSentence;

    NmeaWriteHandler(File gpxFile, String nmeaSentence) {
        this.gpxFile = gpxFile;
        this.nmeaSentence = nmeaSentence;
    }

    @Override
    public void run() {

        synchronized (NmeaFileLogger.lock) {

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(gpxFile, true));
                writer.write(nmeaSentence);
                writer.newLine();
                writer.close();
                Files.addToMediaDatabase(gpxFile, "text/plain");

            } catch (IOException e) {

            }
        }

    }
}
