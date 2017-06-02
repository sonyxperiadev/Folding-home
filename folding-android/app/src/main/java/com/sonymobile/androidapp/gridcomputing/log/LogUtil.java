/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.log;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Pair;

import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to save the log to a file.
 */
public final class LogUtil {

    /**
     * Log sub folder.
     */
    public static final String LOG_FOLDER = "logs";

    /**
     * Log file name.
     */
    public static final String LOG_FILE_NAME = "Folding-At-Home-log";

    /**
     * The queue holding the log entries. The first element of each pair is the
     * timestamp in milliseconds and the second element is the log itself.
     */
    public static final ConcurrentLinkedQueue<Pair<Long, String>> LOGS_QUEUE =
            new ConcurrentLinkedQueue<>();

    /**
     * The atomic boolean used to lock the file writing.
     */
    private static final AtomicBoolean WRITE_LOCK = new AtomicBoolean(false);

    /**
     * Date format used to format the log.
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * System line separator.
     */
    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    /**
     * The write permission.
     */
    private static final int WRITE_PERMISSION = ApplicationData.getAppContext()
            .checkCallingOrSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    /**
     * This class is not intended to be instantiated.
     */
    private LogUtil() {
    }

    /**
     * Logs a text and appends it to a file.
     *
     * @param text the text to be logged.
     */
    public static synchronized void log(final String text) { // NOPMD
        if (WRITE_PERMISSION == PackageManager.PERMISSION_GRANTED) {
            LOGS_QUEUE.add(new Pair<>(System.currentTimeMillis(), text));
            writeLog();
        }
    }

    /**
     * Method to run a Thread and save the logs to a file.
     */
    private static synchronized void writeLog() {
        if (!WRITE_LOCK.getAndSet(true)) {
            final Thread mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final String externalDir = Environment.getExternalStorageDirectory()
                            .getAbsolutePath();
                    final File logDir = new File(externalDir + File.separator + LOG_FOLDER);
                    final File logFile = new File(logDir, LOG_FILE_NAME);
                    Writer writer = null;
                    BufferedWriter out = null;
                    try {
                        if ((logDir.mkdirs() || logDir.isDirectory())
                                && (logFile.createNewFile() || logFile.exists())) {
                            writer = new OutputStreamWriter(new FileOutputStream(logFile,
                                    true), "UTF-8");
                            out = new BufferedWriter(writer);

                            Pair<Long, String> curr;
                            final Calendar cal = Calendar.getInstance();
                            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT,
                                    Locale.US);
                            while ((curr = LOGS_QUEUE.poll()) != null) {
                                cal.setTimeInMillis(curr.first);
                                out.write(dateFormat.format(cal.getTime()) + " " // NOPMD
                                        + curr.second
                                        + LINE_SEPARATOR);
                            }
                        }
                    } catch (final IOException e) { // NOPMD
                        android.util.Log.e(Log.LOG_TAG, e.getMessage()); // NOPMD
                    } catch (final IllegalArgumentException e) { // NOPMD
                        android.util.Log.e(Log.LOG_TAG, e.getMessage()); // NOPMD
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (final IOException e) { // NOPMD
                                android.util.Log.e(Log.LOG_TAG, e.getMessage()); // NOPMD
                            }
                        }
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (final IOException e) { // NOPMD
                                android.util.Log.e(Log.LOG_TAG, e.getMessage()); // NOPMD
                            }
                        }
                    }
                    WRITE_LOCK.set(false);
                }
            });
            mThread.start();
        }
    }
}
