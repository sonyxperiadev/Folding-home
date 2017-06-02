/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.service;

import android.content.Context;
import android.content.Intent;

import com.sonymobile.androidapp.gridcomputing.conditions.ConditionsHandler;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.utils.AlarmUtils;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * Manager class, starts, stops, pauses and resumes folding Service.
 */
public final class ServiceManager {

    private ServiceManager() { }

    /**
     * Verify conditions to start folding and starts it if conditions are met.
     */
    public static void verifyConditionsAndStartService() {
        ConditionsHandler.getInstance().notifyConditionChanged(true);
    }

    /**
     * Start compute service.
     */
    public static void startComputeService() {
        Log.d("Control > Start Compute Service");
        final Context context = ApplicationData.getAppContext();
        context.startService(new Intent(context, ComputeService.class));
    }

    /**
     * Pauses compute service for 24hs.
     */
    public static void pause() {
        Log.d("Execution paused for 24hs");
        final long time = System.currentTimeMillis();
        MiscPref.setLastBatteryPlateauTime(0);
        SettingsPref.setPausedTime(time);
        AlarmUtils.createAlarm(AlarmUtils.AlarmType.REPEAT_24_HOUR);
        verifyConditionsAndStartService();
    }

    /**
     * Resumes service from paused state.
     */
    public static void resume() {
        Log.d("Execution resumed from pause");
        SettingsPref.setPausedTime(0);
        SettingsPref.setExecutionEnabled(true);
        AlarmUtils.cancelAlarm(AlarmUtils.AlarmType.REPEAT_24_HOUR);
        verifyConditionsAndStartService();
    }


}
