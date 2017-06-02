/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.content.Context;
import android.text.format.DateFormat;

import com.sonymobile.androidapp.gridcomputing.R;

import java.text.Format;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Helper class containing a few format methods.
 */
public final class FormatUtils {

    /**
     * 1 Week in Days.
     */
    private static final int ONE_WEEK_IN_DAYS = 7;

    /**
     * 1 Day in Hours.
     */
    private static final int ONE_DAY_IN_HOURS = 24;

    /**
     * This class is not intended to be instantiated.
     */
    private FormatUtils() {
    }

    /**
     * Get time in the current device date/time format.
     *
     * @param hour   hours.
     * @param minute minutes.
     * @return formated time string.
     */
    public static String getTimeString(final int hour, final int minute) {
        final Format format = DateFormat.getTimeFormat(ApplicationData.getAppContext());
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return format.format(cal.getTime());
    }

    /**
     * Get time in the main screen format.
     *
     * @param timeMillis the time in milliseconds to format.
     * @return formated time string.
     */
    public static String getMainTimeString(final long timeMillis) {
        int minutes = (int) (timeMillis / TimeUnit.MINUTES.toMillis(1));
        int hours = (int) (timeMillis / TimeUnit.HOURS.toMillis(1));

        int days = hours / ONE_DAY_IN_HOURS;
        final int weeks = days / ONE_WEEK_IN_DAYS;
        days = days % ONE_WEEK_IN_DAYS;
        hours = hours % ONE_DAY_IN_HOURS;
        minutes = minutes % (int) TimeUnit.MINUTES.toSeconds(1);

        final Context context = ApplicationData.getAppContext();

        String resultStr;
        if (weeks > 0) {
            resultStr = context.getString(R.string.time_string_weeks, weeks, days, hours);
        } else if (days > 0) {
            resultStr = context.getString(R.string.time_string_days, days, hours, minutes);
        } else {
            resultStr = context.getString(R.string.time_string_hours, hours, minutes);
        }

        return resultStr;
    }
}
