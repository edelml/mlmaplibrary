package com.ml.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSenderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FileSenderFactory.class);

    public static void autoSendFiles(String fileToSend) {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        LOG.info("Auto-sending file " + fileToSend);

        File gpxFolder = new File(preferenceHelper.getGoblobFolder());

        if (Files.fromFolder(gpxFolder).length < 1) {
            LOG.warn("No files found to send.");
            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(Files.fromFolder(gpxFolder, new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(fileToSend) && !s.contains("zip");
            }
        })));

        List<File> zipFiles = new ArrayList<>();

        if (files.size() == 0) {
            LOG.warn("No files found to send after filtering.");
            return;
        }

        if (preferenceHelper.shouldSendZipFile()) {
            File zipFile = new File(gpxFolder.getPath(), fileToSend + ".zip");
            ArrayList<String> filePaths = new ArrayList<>();

            for (File f : files) {
                filePaths.add(f.getAbsolutePath());
            }

            LOG.info("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.zipFiles();

            zipFiles.clear();
            zipFiles.add(zipFile);
        }


        /*for (FileSender sender : senders) {
            LOG.debug("Sender: " + sender.getClass().getName());
            //Special case for OSM Uploader
            if(!sender.accept(null, ".zip")){
                sender.uploadFile(files);
                continue;
            }

            if(preferenceHelper.shouldSendZipFile()){
                sender.uploadFile(zipFiles);
            } else {
                sender.uploadFile(files);
            }

        }*/
    }
}
