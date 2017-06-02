/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import com.sonymobile.androidapp.gridcomputing.fragments.ReportChartFragment;
import com.sonymobile.androidapp.gridcomputing.log.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Contract class that defines the job checkpoints table.
 */
public final class JobCheckpointsContract {

    /**
     * SQL drop table statement.
     */
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + JobCheckpointEntry.TABLE_NAME;

    /**
     * SQLite Date type.
     */
    private static final String DATE_TYPE = " TEXT";

    /**
     * SQLite Integer type.
     */
    private static final String INTEGER_TYPE = " INTEGER";

    /**
     * Comma constant.
     */
    private static final String COMMA_SEP = ",";

    /**
     * SQL statement to create the job_checkpoint table.
     */
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + JobCheckpointEntry.TABLE_NAME + " ("
                    + JobCheckpointEntry._ID + DATE_TYPE
                    + " PRIMARY KEY" + COMMA_SEP
                    + JobCheckpointEntry.COLUMN_NAME_DURATION + INTEGER_TYPE + " )";

    /**
     * Time format used to store time in SQLite: 2015-05-26 18:40:53.497.
     */
    private static final String DAY_TIME_FORMAT = "%Y-%m-%d %H:%M:%f";

    /**
     * SQLite function to get the current time.
     */
    private static final String TIME_NOW_FUNCTION =
            "strftime('" + DAY_TIME_FORMAT + "', 'now', 'localtime')";

    /**
     * SQLite function to get the current time minus 1 day.
     */
    private static final String TIME_YESTERDAY_FUNCTION = "strftime('" + DAY_TIME_FORMAT
            + "', 'now', '-1 day')";

    /**
     * Query used to insert a new row.
     */
    private static final String INSERT_ROW_QUERY = "insert or replace into %1$s (%2$s, %3$s)"
            + "values (%4$s, %5$d)";

    /**
     * Query used to delete old rows.
     */
    private static final String DELETE_ROWS_QUERY = "delete from %1$s where %2$s < %3$s";

    /**
     * Query used to retrieve the sum of checkpoints.
     */
    private static final String SELECT_SUM_QUERY = "select sum(%1$s) from %2$s where %3$s > %4$s";

    /**
     * Used to parse date from sql to java.
     */
    private static final SimpleDateFormat SQL_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /**
     * Private constructor.
     */
    private JobCheckpointsContract() {
    }

    /**
     * Ads a new row to the Database with the current time and the checkpoint time.
     *
     * @param checkpointTime the checkpoint time to save.
     */
    public static void addCheckpoint(final long checkpointTime) {
        final Object[] params = {JobCheckpointEntry.TABLE_NAME,
                JobCheckpointEntry._ID, JobCheckpointEntry.COLUMN_NAME_DURATION,
                TIME_NOW_FUNCTION,
                checkpointTime};
        final String formattedQuery = String.format(Locale.US, INSERT_ROW_QUERY, params);
        Log.d(">>> addCheckpoint: " + formattedQuery);
        synchronized (JobCheckpointsContract.class) {
            final SQLiteDatabase db = JobDBHelper.getInstance().getReadableDatabase();
            db.execSQL(formattedQuery);
            db.close();
        }
    }

    /**
     * Removes entries in table older than 24 hours (1 day).
     */
    public static void clear24HourAccumulatedTime() {
        final Object[] params = {JobCheckpointEntry.TABLE_NAME,
                JobCheckpointEntry._ID, TIME_YESTERDAY_FUNCTION};
        final String formattedQuery = String.format(Locale.US, DELETE_ROWS_QUERY, params);
        synchronized (JobCheckpointsContract.class) {
            final SQLiteDatabase db = JobDBHelper.getInstance().getReadableDatabase();
            db.execSQL(formattedQuery);
            db.close();
        }
    }

    /**
     * Gets the sum of checkpoints from past 24 hours (1 day).
     *
     * @return the sum of checkpoint from the past 24 hours.
     */
    public static long get24HourAccumulatedTime() {
        final Object[] params = {JobCheckpointEntry.COLUMN_NAME_DURATION,
                JobCheckpointEntry.TABLE_NAME, JobCheckpointEntry._ID,
                TIME_YESTERDAY_FUNCTION};
        final String formattedQuery = String.format(Locale.US, SELECT_SUM_QUERY, params);
        long returnValue = 0L;
        synchronized (JobCheckpointsContract.class) {
            final SQLiteDatabase db = JobDBHelper.getInstance().getReadableDatabase();
            Cursor c = db.rawQuery(formattedQuery, new String[]{});


            if (c.moveToFirst()) {
                returnValue = c.getLong(0);
            }
            c.close();
            db.close();
        }
        return returnValue;
    }

    /**
     * Gets an hourly report based on a data type.
     *
     * @param dataType the data type to ger the reports.
     * @return the sparse array which the keys are the days of the month for weekly
     * or monthly report and years for all time reports.
     */
    public static SparseArray<Pair<Date, Double>> getHourlyReport(
            final ReportChartFragment.DataType dataType) {
        final SparseArray<Pair<Date, Double>> sparseArray = new SparseArray<>();

        String dateSelectQuery = "";
        String groupBy = "";
        String periodQuery = "";
        String orderBy = "";

        //using strftime('%Y-%m-%d', ...) to start counting since the begining of day
        if (dataType == ReportChartFragment.DataType.WEEK) {
            groupBy = "day_of_month";
            dateSelectQuery = "strftime('%d', _id) day_of_month";

            periodQuery = "strftime('%Y-%m-%d', 'now', 'localtime', '-6 days')";
        } else if (dataType == ReportChartFragment.DataType.MONTH) {
            groupBy = "strftime('%d', _id), strftime('%m', _id)";
            dateSelectQuery = "strftime('%s', _id) / 86400 days_since_1970";
            orderBy = "_id";

            periodQuery =
                "strftime('%Y-%m-%d', 'now', 'localtime', 'weekday 0', '-7 days', '-28 days')";
        } else if (dataType == ReportChartFragment.DataType.ALL_TIME) {
            groupBy = "year";
            dateSelectQuery = "strftime('%Y', _id) year";

            periodQuery = "date(0)";
        }

        final String formattedQuery = "SELECT "
                + (TextUtils.isEmpty(dateSelectQuery) ? "" : dateSelectQuery + ", ")
                + " _id, sum (" + JobCheckpointEntry.COLUMN_NAME_DURATION
                + ") / 3600000.0" // converting to hours
                + " FROM " + JobCheckpointEntry.TABLE_NAME
                + (TextUtils.isEmpty(periodQuery) ? "" : " WHERE " + JobCheckpointEntry._ID
                + " BETWEEN " + periodQuery + " AND datetime('now', 'localtime') ")
                + (TextUtils.isEmpty(groupBy) ? "" : " group by " + groupBy)
                + (TextUtils.isEmpty(orderBy) ? "" : " order by " + orderBy);

        Log.d(">>> query: " + formattedQuery);

        synchronized (JobCheckpointsContract.class) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = JobDBHelper.getInstance().getReadableDatabase();
                cursor = db.rawQuery(formattedQuery, new String[]{});
                while (cursor.moveToNext()) {
                    //get the values as String to avoid error when the SQL lib tries to parse the data
                    final String key = cursor.getString(0);
                    final String date = cursor.getString(1);
                    final String value = cursor.getString(2);
                    sparseArray.append(Integer.parseInt(key),
                                       new Pair<>(SQL_DATE_FORMAT.parse(date),
                                                  Double.valueOf(value)));
                }
            } catch (Exception e) {
                Log.e(e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }
        }

        return sparseArray;
    }

    /**
     * BaseColumns class to define the job_checkpoint table.
     */
    public abstract static class JobCheckpointEntry implements BaseColumns {
        /**
         * The table name.
         */
        public static final String TABLE_NAME = "job_checkpoint";

        /**
         * Column name to store the checkpoint duration.
         */
        public static final String COLUMN_NAME_DURATION = "checkpoint_duration";
    }
}
