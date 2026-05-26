package com.koshub.psdku.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for date formatting.
 */
public class DateHelper {
    private static final String DEFAULT_FORMAT = "dd MMM yyyy";

    public static String formatDate(Date date) {
        return formatDate(date, DEFAULT_FORMAT);
    }

    public static String formatDate(long timestamp) {
        return formatDate(new Date(timestamp), DEFAULT_FORMAT);
    }

    public static String formatDate(Date date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, new Locale("id", "ID"));
        return sdf.format(date);
    }

    public static String getCurrentDate() {
        return formatDate(new Date());
    }
}
