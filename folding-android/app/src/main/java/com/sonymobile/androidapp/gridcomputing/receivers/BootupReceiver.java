/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.receivers;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.service.ServiceManager;
import com.sonymobile.androidapp.gridcomputing.utils.AlarmUtils;

/**
 * Bootup receiver to start our background service with the device.
 */
public class BootupReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            if (MiscPref.getWizardFinished()) {
                ServiceManager.verifyConditionsAndStartService();
                AlarmUtils.createAlarm(AlarmUtils.AlarmType.REPEAT_1_MIN);
                checkPausedApp();
            }
        }
    }

    /**
     * Checks if application was paused and sets an alarm to resume it, if needed.
     */
    private void checkPausedApp() {
        final long pausedTime = SettingsPref.getPauseTime();
        if (pausedTime > 0) {
            final long now = System.currentTimeMillis();
            final long elapsedTime = now - pausedTime;

            // If elapsedTime is greater than 24hs, the app should no longer be paused.
            if (elapsedTime >= AlarmManager.INTERVAL_DAY) {
                ServiceManager.resume();
            } else {
                AlarmUtils.createScheduledAlarm(AlarmManager.INTERVAL_DAY - elapsedTime, 0);
            }

        }
    }
}
