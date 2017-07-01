package com.reversecoder.obd2.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Md. Rashsadul Alam
 */
public class ObdUtil {

    public static final String SIMPLE_DATE_FORMAT = "EEE, d MMM yyyy hh:mm:ss a";

    /*
    *  @see <a href="https://developer.android.com/reference/java/text/SimpleDateFormat.html">SimpleDateFormat</a>
    * */
    public static String convertMilliSecondToTime(long milliSecond, String dateFormat) {
        Date date = new Date(milliSecond);
        SimpleDateFormat dateformat;
        if (dateFormat == null || dateFormat.equalsIgnoreCase("")) {
            dateformat = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        } else {
            dateformat = new SimpleDateFormat(dateFormat);
        }
        return dateformat.format(date);
    }

//    public static String getStringFromInputStream(InputStream is)
//            throws IOException {
//        StringBuffer sb = new StringBuffer();
//        BufferedReader reader = null;
//        String line = null;
//        try {
//            reader = new BufferedReader(new InputStreamReader(is));
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (Exception ex) {
//                }
//            }
//        }
//        return sb.toString();
//    }

    public static byte[] getBytesFromInputStream(InputStream is) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BufferedInputStream bin = new BufferedInputStream(is);
        byte[] totalBytes = null;
        int numRead = 0;
        byte[] bytes = new byte[1024];
        try {
            while ((numRead = bin.read(bytes)) > 0) {
                bos.write(bytes, 0, numRead);
            }
            bos.flush();
            totalBytes = bos.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return totalBytes;
    }

    public static int getRandomInteger() {
        Random r = new Random();
        int a = r.nextInt((1000 - 100)) + 100;
        return a;
    }

    public static long getRandomLong() {
        Random r = new Random();
        long a = r.nextLong();
        return a;
    }
}
