/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.util.Locale;


public final class EnvironmentMessenger {

    private EnvironmentMessenger() { }

    /**
     * Returns the key message sent to the client.js.
     *
     * @return a JSON containing the key reply message.
     */
    public static String getJsonKeyReply() {
        final String key = "clientkey";
        final String uuid = MiscPref.getUUID();
        return "{\"action\": \"key\", \"content\":{\"key\":\"" + key  + "\", "
                + "\"uuid\":\"" + uuid + "\", "
                + "\"app_version\" : \"" + getVersionCode() + "\", "
                + "\"platform\" : \"android\", "
                + "\"os_version\" : \"" + Build.VERSION.RELEASE + "\", "
                + "\"arch\" : \"" + Build.CPU_ABI + "\""
                + "}}\n";
    }

    /**
     * Returns the kill message sent to the client.js.
     *
     * @param immediately True if should kill the client immediately.
     * @return a JSON containing the kill message.
     */
    public static String getJsonKillClient(final boolean immediately) {
        if (immediately) {
            return "{\"action\": \"kill\", \"content\":\"SIGKILL\"}\n";
        }
        return "{\"action\": \"kill\", \"content\":\"SIGTERM\"}\n";
    }

    /**
     * Returns the resume execution message sent to the client.js.
     *
     * @return a JSON containing the resume execution message.
     */
    public static String getJsonResumeJobClient() {
        return "{\"action\": \"continue\", \"content\":{}}\n";
    }

    /**
     * Returns the versionCode of the application, defined in
     * AndroidManifest.xml.
     *
     * @return The version code of the application.
     */
    public static String getVersionCode() {
        try {
            final Context context = ApplicationData.getAppContext();
            final PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return String.format(Locale.US, "%d", packageInfo.versionCode);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
