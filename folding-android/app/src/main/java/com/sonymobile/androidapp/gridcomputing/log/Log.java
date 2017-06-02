/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.log;

import android.text.TextUtils;

import com.sonymobile.androidapp.gridcomputing.BuildConfig;

/**
 * A debug helper class which allows to disable or enable debugging.
 */
public final class Log { // NOPMD

    /**
     * Default tag used to log the events.
     */
    public static final String LOG_TAG = "Compute";

    /**
     * If the logging is enabled.
     */
    private static boolean sLogging = true;

    /**
     * This class is not intended to be instantiated.
     */
    private Log() {
    }

    /**
     * Enables/disables the Log message.
     *
     * @param enableLog true if the logging must be enabled or false otherwise.
     */
    public static synchronized void setLogging(final boolean enableLog) { // NOPMD
        sLogging = enableLog;
    }

    /**
     * Logs a debug message.
     *
     * @param tag     Identify the source of a log message.
     * @param message The message to log.
     */
    public static synchronized void d(final String tag, final String message) { // NOPMD
        if (BuildConfig.DEBUG && sLogging && !TextUtils.isEmpty(tag)
                && !TextUtils.isEmpty(message)) {
            android.util.Log.d(tag, message);
            LogUtil.log(message);
        }
    }

    /**
     * Logs a debug message using the default tag.
     *
     * @param message The message to log.
     */
    public static synchronized void d(final String message) { // NOPMD
        d(LOG_TAG, message);
    }

    /**
     * Logs an error message.
     *
     * @param tag     Identify the source of a log message.
     * @param message The message to log.
     */
    public static synchronized void e(final String tag, final String message) { // NOPMD
        if (BuildConfig.DEBUG && sLogging && !TextUtils.isEmpty(tag)
                && !TextUtils.isEmpty(message)) {
            android.util.Log.e(tag, message);
            LogUtil.log(message);
        }
    }

    /**
     * Logs an error message using the default tag.
     *
     * @param message The message to log.
     */
    public static synchronized void e(final String message) { // NOPMD
        e(LOG_TAG, message);
    }
}
