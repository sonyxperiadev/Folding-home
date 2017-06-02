/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.sonymobile.androidapp.gridcomputing.R;

/**
 * Utility class used to check network conditions.
 */
public final class NetworkUtils {

    private NetworkUtils() { }

    /**
     * Check if application is online.
     *
     * @return true if online, false if disconnected.
     */
    public static boolean isConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) ApplicationData.getAppContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Show no networkt available error message.
     *
     * @param context application context.
     */
    public static void showNoNetworkError(final Context context) {
        Toast.makeText(context, R.string.toast_network_unavailable, Toast.LENGTH_SHORT).show();
    }
}
