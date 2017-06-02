/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;


/**
 * Helper class to store Job related data.
 */
public final class JobDBHelper extends SQLiteOpenHelper {

    /**
     * DataBase version. Must increment the version when the database changes.
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * Database name.
     */
    public static final String DATABASE_NAME = "JobData.db";

    /**
     * Static instance of this DB.
     */
    private static JobDBHelper sInstance;

    /**
     * Private constructor.
     */
    private JobDBHelper() {
        super(ApplicationData.getAppContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Gets an instance of this DB.
     *
     * @return a singleton instance of this DB.
     */
    public static JobDBHelper getInstance() {
        if (sInstance == null) {
            sInstance = new JobDBHelper();
        }
        return sInstance;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(JobCheckpointsContract.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(JobCheckpointsContract.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
