package com.reversecoder.obd.reader.map.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Md. Rashadul Alam
 */
public class WattsDateUtils {

    /**
     * This method returns the number of days since the epoch (January 01, 1970, 12:00 Midnight UTC)
     * in UTC time from the datestring from OCM
     *
     * @param dateString A date in OCM format, e.g. 2016-08-11T18:52:00Z.
     * @return The number of days from the epoch to the date argument.
     */
    public static Long dateStringToEpoc(String dateString)
            throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = format.parse(dateString);
        return date.getTime();
    }
}
