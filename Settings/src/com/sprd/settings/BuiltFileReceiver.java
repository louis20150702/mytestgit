package com.sprd.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

public class BuiltFileReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "BuiltFileReceiver";
    private final String SYSTEM_FILE_PATH_1 = "/system/media/mp3_1.mp3";
    private final String INTERNAL_FILE_PATH_1 = "/storage/internalsd/Music/mp3_1.mp3";
    private final String SYSTEM_FILE_PATH_2 = "/system/media/mp3_2.mp3";
    private final String INTERNAL_FILE_PATH_2 = "/storage/internalsd/Music/mp3_2.mp3";
    private final String INTERNAL_FILE_PARENT = "/storage/internalsd/Music";
    private final String SYSTEM_FILE_PATH_3 = "/system/media/mp3_3.mp3";
    private final String INTERNAL_FILE_PATH_3 = "/storage/internalsd/Music/mp3_3.mp3";
    private final String SYSTEM_FILE_PATH_4 = "/system/media/mp4_1.mp4";
    private final String INTERNAL_FILE_PATH_4 = "/storage/internalsd/Movies/mp4_1.mp4";
    


    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (Environment.getInternalStoragePathState().equals(Environment.MEDIA_MOUNTED)) {
            File src = new File(SYSTEM_FILE_PATH_1);
            File dest = new File(INTERNAL_FILE_PATH_1);
            if (!dest.exists()) {
                copyFile(src, dest);
            }
	    src = new File(SYSTEM_FILE_PATH_2);
            dest = new File(INTERNAL_FILE_PATH_2);
            if (!dest.exists()) {
                copyFile(src, dest);
            }

	    src = new File(SYSTEM_FILE_PATH_3);
            dest = new File(INTERNAL_FILE_PATH_3);
            if (!dest.exists()) {
                copyFile(src, dest);
            }
            src = new File(SYSTEM_FILE_PATH_4);
            dest = new File(INTERNAL_FILE_PATH_4);
            if (!dest.exists()) {
                copyFile(src, dest);
            }
        }
    }

    private boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = copyToFile(in, destFile);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    private boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            File fileParent = new File(INTERNAL_FILE_PARENT);
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            destFile.createNewFile();
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
