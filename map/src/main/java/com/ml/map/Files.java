package com.ml.map;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Files {
    /**
     * Gets the Goblob-specific MIME type to use for a given filename/extension
     *
     * @param fileName
     * @return
     */
    public static String getMimeType(String fileName) {

        if (fileName == null || fileName.length() == 0) {
            return "";
        }


        int pos = fileName.lastIndexOf(".");
        if (pos == -1) {
            return "application/octet-stream";
        } else {

            String extension = fileName.substring(pos + 1, fileName.length());


            if (extension.equalsIgnoreCase("gpx")) {
                return "application/gpx+xml";
            } else if (extension.equalsIgnoreCase("kml")) {
                return "application/vnd.google-earth.kml+xml";
            } else if (extension.equalsIgnoreCase("zip")) {
                return "application/zip";
            }
        }

        //Unknown mime type
        return "application/octet-stream";

    }

    public static void addToMediaDatabase(File file, String mimeType){

        MediaScannerConnection.scanFile(GoblobLocationManager.getInstance().getApplicationContext(),
                new String[]{file.getPath()},
                new String[]{mimeType},
                null);
    }

    public static File[] fromFolder(File folder) {
        return fromFolder(folder, null);
    }

    public static File[] fromFolder(File folder, FilenameFilter filter) {

        if (folder == null || !folder.exists() || folder.listFiles() == null) {
            return new File[]{};
        } else {
            if (filter != null) {
                return folder.listFiles(filter);
            }
            return folder.listFiles();
        }
    }

    public static File storageFolder(Context context){
        File storageFolder = context.getExternalFilesDir(null);
        if(storageFolder == null){
            storageFolder = context.getFilesDir();
        }
        return storageFolder;
    }

    public static void setFileExplorerLink(TextView txtFilename, Spanned htmlString, final String pathToLinkTo, final Context context) {

        final Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.parse("file://" + pathToLinkTo), "resource/folder");
        intent.setAction(Intent.ACTION_VIEW);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            txtFilename.setLinksClickable(true);
            txtFilename.setClickable(true);
            txtFilename.setMovementMethod(LinkMovementMethod.getInstance());
            txtFilename.setSelectAllOnFocus(false);
            txtFilename.setTextIsSelectable(false);
            txtFilename.setText(htmlString);

            txtFilename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    context.startActivity(intent);
                }
            });
        }
    }

    public static boolean isAllowedToWriteTo(String goblobFolder) {
        return new File(goblobFolder).canWrite();
    }

    public static String getAssetFileAsString(String pathToAsset, Context context){
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(pathToAsset);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
//            Log.e(TAG, "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
//                    Log.e(TAG, "Error closing asset " + name);
                }
            }
        }

        return null;

    }

    public static File createTestFile() throws IOException {
        File gpxFolder = new File(PreferenceHelper.getInstance().getGoblobFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        File testFile = new File(gpxFolder.getPath(), "goblob_test.xml");
        if (!testFile.exists()) {
            testFile.createNewFile();

            FileOutputStream initialWriter = new FileOutputStream(testFile, true);
            BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

            initialOutput.write("<x>This is a test file</x>".getBytes());
            initialOutput.flush();
            initialOutput.close();

            Files.addToMediaDatabase(testFile, "text/xml");
        }

        return testFile;
    }

    public static boolean reallyExists(File gpxFile) {
        // Sometimes .isFile returns false even if a file exists.
        // This guesswork tries to determine whether file exists in a few different ways.
        return gpxFile.isFile() || gpxFile.getAbsoluteFile().exists() || gpxFile.getAbsoluteFile().isFile();
    }
}
