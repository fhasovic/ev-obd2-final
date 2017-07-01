package com.reversecoder.obd.reader.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.reversecoder.obd.reader.R;
import com.reversecoder.obd2.utils.ObdUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Md. Rashadul Alam
 */
public class AppUtils {

    public static String convertStreamToString(InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (java.util.NoSuchElementException e) {
            return "";
        }
    }

    public static void saveLogcatToFile(Context context, String devemail, String ccemail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{devemail});
        emailIntent.putExtra(Intent.EXTRA_CC, new String[]{ccemail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[EV-OBD2_" + context.getResources().getString(R.string.app_version_name) + "] Debug log of " + ObdUtil.convertMilliSecondToTime(System.currentTimeMillis(), "yyyy.MM.dd 'at' hh:mm:ss a"));

        StringBuilder sb = new StringBuilder();
        sb.append("\nManufacturer: ").append(Build.MANUFACTURER);
        sb.append("\nModel: ").append(Build.MODEL);
        sb.append("\nAndroid version: ").append(Build.VERSION.RELEASE);

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        String fileName = "[EV-OBD2_" + context.getResources().getString(R.string.app_version_name) + "]_" + ObdUtil.convertMilliSecondToTime(System.currentTimeMillis(), "yyyy.MM.dd'_'hh.mm.ss'_'a") + ".txt";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + "EV-OBD2");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (dir.exists()) {
            File outputFile = new File(dir, fileName);
            Uri uri = Uri.fromFile(outputFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            Log.d("saveLogcatToFile", "Going to send logcat from " + outputFile);
            //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            try {
                @SuppressWarnings("unused")
                Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
