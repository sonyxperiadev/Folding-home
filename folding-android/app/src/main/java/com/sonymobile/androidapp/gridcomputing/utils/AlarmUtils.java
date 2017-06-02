/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
package com.sonymobile.androidapp.gridcomputing.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.sonymobile.androidapp.gridcomputing.receivers.AlarmReceiver;

import java.util.concurrent.TimeUnit;

/**
 * This class provides an abstraction of the AlarmManager class to help setting alarms.
 */
public final class AlarmUtils {

    /**
     * 1min Alarm pending intent id.
     */
    private static final int ALARM_1MIN_ID = 147851;
    /**
     * 24hours Alarm pending intent id.
     */
    private static final int ALARM_24HOURS_ID = 147852;
    /**
     * 30min Alarm pending intent id.
     */
    private static final int ALARM_30MIN_ID = 147853;
    /**
     * Scheduled Alarm pending intent id.
     */
    private static final int ALARM_SCHEDULED_ID = 147854;

    private AlarmUtils() { }

    /**
     * Sets an alarm to be triggered at the specified time since device boot and subsequent alarms
     * within the interval, if needed.
     *
     * @param intentAction Alarm intent action name.
     * @param intentId     Alarm pending intent ID.
     * @param startTime    Time in milliseconds which the alarm is to be first triggered.
     * @param interval     Interval between this alarm and the next. 0 for non repeating alarms.
     * @param wakeUp       true if the alarm is set wake up the device when the alarm is triggered.
     *                     False, if it's set to be triggered on the next time the device wakes up.
     */
    private static void setAlarm(final String intentAction, final int intentId,
                                 final long startTime, final long interval, final boolean wakeUp) {
        final Context context = ApplicationData.getAppContext();
        final Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(intentAction);
        final PendingIntent pIntent = PendingIntent.getBroadcast(context, intentId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        final int alarmType;
        if (wakeUp) {
            alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        } else {
            alarmType = AlarmManager.ELAPSED_REALTIME;
        }

        final long alarmStartTime = SystemClock.elapsedRealtime() + startTime;

        if (interval > 0) {
            alarmManager.setInexactRepeating(alarmType, alarmStartTime, interval, pIntent);
        } else {
            alarmManager.set(alarmType, alarmStartTime, pIntent);
        }
    }

    /**
     * Sets an alarm to be triggered at the specified time and subsequent alarms within the
     * interval, if needed.
     *
     * @param intentAction Alarm intent action name.
     * @param intentId     Alarm pending intent ID.
     * @param startTime    Time when the alarm will be triggered. i.e. Calendar.getTimeInMillis()
     * @param interval     Interval between the first triggered alarm and the next.
     *                     0 for non repeating alarms.
     * @param wakeUp       true if the alarm is set wake up the device when the alarm is triggered.
     *                     False, if it's set to be triggered on the next time the device wakes up.
     */
    private static void setRTCAlarm(final String intentAction, final int intentId,
                                    final long startTime, final long interval,
                                    final boolean wakeUp) {
        final Context context = ApplicationData.getAppContext();
        final Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(intentAction);
        final PendingIntent pIntent = PendingIntent.getBroadcast(context, intentId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        final int alarmType;
        if (wakeUp) {
            alarmType = AlarmManager.RTC_WAKEUP;
        } else {
            alarmType = AlarmManager.RTC;
        }

        if (interval > 0) {
            alarmManager.setInexactRepeating(alarmType, startTime, interval, pIntent);
        } else {
            alarmManager.set(alarmType, startTime, pIntent);
        }
    }

    /**
     * Cancel alarm with the given ID.
     *
     * @param name Type of the alarm to cancel.
     * @param id   ID of the alarm to cancel.
     */
    private static void cancelAlarm(final String name, final int id) {
        final Context context = ApplicationData.getAppContext();
        final Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(name);
        final PendingIntent pIntent = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(pIntent);
    }

    /**
     * Creates an alarm with the following AlarmType.
     *
     * @param alarmType type of the alarm.
     */
    public static void createAlarm(final AlarmType alarmType) {
        switch (alarmType) {
            case REPEAT_1_MIN:
                setAlarm(alarmType.name(), ALARM_1MIN_ID,
                        TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1), true);
                break;
            case REPEAT_30_MIN:
                setAlarm(alarmType.name(), ALARM_30MIN_ID,
                        AlarmManager.INTERVAL_HALF_HOUR, AlarmManager.INTERVAL_HALF_HOUR, true);
                break;
            case REPEAT_24_HOUR:
                setAlarm(alarmType.name(), ALARM_24HOURS_ID,
                        AlarmManager.INTERVAL_DAY, 0, true);
                break;
            default:
                break;

        }
    }

    /**
     * Schedules an alarm to trigger at the given time and interval.
     *
     * @param time     time when the alarm will be triggered.
     * @param interval interval when the next alarm will be triggered.
     */
    public static void createScheduledAlarm(final long time, final long interval) {
        setAlarm(AlarmType.SCHEDULED.name(), ALARM_SCHEDULED_ID, time, interval, true);
    }

    /**
     * Cancels an AlarmType with the following AlarmType.
     *
     * @param alarmType type of the alarm.
     */
    public static void cancelAlarm(final AlarmType alarmType) {
        switch (alarmType) {
            case REPEAT_1_MIN:
                cancelAlarm(alarmType.name(), ALARM_1MIN_ID);
                break;
            case REPEAT_30_MIN:
                cancelAlarm(alarmType.name(), ALARM_30MIN_ID);
                break;
            case REPEAT_24_HOUR:
                cancelAlarm(alarmType.name(), ALARM_24HOURS_ID);
                break;
            default:
                break;
        }
    }

    /**
     * Alarm types.
     */
    public enum AlarmType {
        /**
         * Alarm repeated within an 1min interval.
         */
        REPEAT_1_MIN,
        /**
         * Alarm repeated within a 30min interval.
         */
        REPEAT_30_MIN,
        /**
         * Alarm repeated within a 24 hour interval.
         */
        REPEAT_24_HOUR,
        /**
         * Alarm triggered within a scheduled time.
         */
        SCHEDULED
    }
}
