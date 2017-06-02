/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sonymobile.androidapp.gridcomputing.notifications.NotificationHelper;

/**
 * Receives a callback when the phone language changes and updates the
 * notification to new language.
 */
public final class LanguageChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_LOCALE_CHANGED)) {
            NotificationHelper.updateNotification();
        }
    }
}
