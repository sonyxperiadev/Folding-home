/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * Helper class to manipulate Preferences.
 */
public final class PrefUtils {

    /**
     * This class is not intended to be instantiated.
     */
    private PrefUtils() {
    }

    public static SharedPreferences getSharedPreferences(final String prefFile) {
        final Context currContext = ApplicationData.getAppContext();
        return currContext.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    }

    /**
     * Gets int value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @return the int value
     */
    public static int getIntValue(final String prefFile, final String key) {
        return getSharedPreferences(prefFile).getInt(key, 0);
    }

    /**
     * Sets int value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @param value    the int value
     */
    public static void setIntValue(final String prefFile, final String key, final int value) {
        final SharedPreferences sharedPref = getSharedPreferences(prefFile);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Gets string value of the preference.
     *
     * @param prefFile     the name of the preference
     * @param key          the preference key
     * @param defaultValue the value returned in case preference doesn't exist
     * @return the string value
     */
    public static String getStringValue(final String prefFile, final String key,
                                        final String defaultValue) {
        return getSharedPreferences(prefFile).getString(key, defaultValue);
    }

    /**
     * Sets string value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @param value    the string value
     */
    public static void setStringValue(final String prefFile, final String key, final String value) {
        final SharedPreferences sharedPref = getSharedPreferences(prefFile);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Gets long value of the preference.
     *
     * @param prefFile     the name of the preference
     * @param key          the preference key
     * @param defaultValue the value returned in case preference doesn't exist
     * @return the long value
     */
    public static long getLongValue(final String prefFile, final String key,
                                    final long defaultValue) {
        return getSharedPreferences(prefFile).getLong(key, defaultValue);
    }

    /**
     * Sets long value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @param value    the long value
     */
    public static void setLongValue(final String prefFile, final String key, final long value) {
        final SharedPreferences sharedPref = getSharedPreferences(prefFile);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * Gets boolean value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @return the boolean value
     */
    public static boolean getBooleanValue(final String prefFile, final String key) {
        return getBooleanValue(prefFile, key, false);
    }

    /**
     * Gets boolean value of the preference.
     *
     * @param prefFile     the name of the preference
     * @param key          the preference key
     * @param defaultValue the default value
     * @return the boolean value
     */
    public static boolean getBooleanValue(
            final String prefFile, final String key, final boolean defaultValue) {
        return getSharedPreferences(prefFile).getBoolean(key, defaultValue);
    }

    /**
     * Sets boolean value of the preference.
     *
     * @param prefFile the name of the preference
     * @param key      the preference key
     * @param value    the boolean value
     */
    public static void setBooleanValue(
            final String prefFile, final String key, final boolean value) {
        final SharedPreferences sharedPref = getSharedPreferences(prefFile);
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
}
