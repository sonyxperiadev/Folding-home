/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.preferences;

import android.text.TextUtils;

import com.sonymobile.androidapp.gridcomputing.service.EnvironmentMessenger;

import java.util.UUID;

/**
 * General purpose preferences used across the app.
 */
public final class MiscPref {
    /**
     * Shared preferences miscellaneous file name.
     */
    public static final String PREF_FILE = "misc_pref";
    /**
     * Shared preferences key for the UUID.
     */
    public static final String UUID_KEY = "UUID";
    /**
     * Shared preferences key for disabled application.
     */
    public static final String DISABLED_APP_KEY = "DISABLED_APP_KEY";
    /**
     * Shared preferences key set when the user has finished seeing the wizard.
     */
    public static final String WIZARD_FINISHED_KEY = "WIZARD_FINISHED_KEY";
    /**
     * Shared preferences key for the latest app version.
     */
    public static final String LATEST_VERSION_KEY = "LATEST_VERSION_KEY";
    /**
     * Shared preferences key used to store the time at which the battery reached the
     * minimum plateau level.
     */
    public static final String LAST_BATTERY_PLATEAU_TIME = "LAST_BATTERY_PLATEAU_TIME_KEY";

    private MiscPref() { }

    /**
     * Gets the UUID.
     *
     * @return previously saved UUID, else generates a new UUID.
     */
    public static String getUUID() {
        String uuid = PrefUtils.getStringValue(PREF_FILE, UUID_KEY, "");
        if (!TextUtils.isEmpty(uuid)) {
            return uuid;
        }
        uuid = UUID.randomUUID().toString();
        PrefUtils.setStringValue(PREF_FILE, UUID_KEY, uuid);
        return uuid;
    }

    /**
     * Checks if the app is disabled.
     *
     * @return true if the app is disabled, false otherwise.
     */
    public static boolean getDisabledApp() {
        return PrefUtils.getBooleanValue(PREF_FILE, DISABLED_APP_KEY, false);
    }

    /**
     * Set the application disable key.
     *
     * @param disabledApp disable param key.
     */
    public static void setDisabledApp(final boolean disabledApp) {
        PrefUtils.setBooleanValue(PREF_FILE, DISABLED_APP_KEY, disabledApp);
    }

    /**
     * Checks if the user has finished seeing the wizard for the first time.
     *
     * @return true if the user has finished seeing the wizard for the first time, false otherwise.
     */
    public static boolean getWizardFinished() {
        return PrefUtils.getBooleanValue(PREF_FILE, WIZARD_FINISHED_KEY, false);
    }

    /**
     * User has finished wizard.
     */
    public static void setWizardFinished() {
        PrefUtils.setBooleanValue(PREF_FILE, WIZARD_FINISHED_KEY, true);
    }

    /**
     * Checks the latest installed version of the Folding@Home app and sets the latest.
     *
     * @return true if version found is the same as latest version.
     */
    public static boolean checkAndSetLatestVersion() {
        final String latestVersion = PrefUtils.getStringValue(PREF_FILE, LATEST_VERSION_KEY, "");
        final String currentVersion = EnvironmentMessenger.getVersionCode();
        PrefUtils.setStringValue(PREF_FILE, LATEST_VERSION_KEY, currentVersion);
        return currentVersion.equals(latestVersion);
    }

    /**
     * Returns the time at which the battery reached the plateau level.
     * @return the time in millis.
     */
    public static long getLastBatteryPlateauTime() {
        return PrefUtils.getLongValue(PREF_FILE, LAST_BATTERY_PLATEAU_TIME, 0L);
    }

    /**
     * Sets the time at which the battery reached the plateau level.
     * @param time the time in millis.
     */
    public static void setLastBatteryPlateauTime(final long time) {
        PrefUtils.setLongValue(PREF_FILE, LAST_BATTERY_PLATEAU_TIME, time);
    }
}
