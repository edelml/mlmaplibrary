package com.ml.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
    private static final int BUFFER = 2048;
    private static final Logger LOG = LoggerFactory.getLogger(ZipHelper.class);
    private final String[] files;
    private final String zipFile;

    public ZipHelper(String[] files, String zipFile) {
        this.files = files;
        this.zipFile = zipFile;
    }

    public void zipFiles() {
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(zipFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[BUFFER];

            for (String f : files) {
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(f.substring(f.lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                out.closeEntry();
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            LOG.error("Could not create zip file", e);
        }

    }

}
