/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.SummaryActivity;
import com.sonymobile.androidapp.gridcomputing.database.JobCheckpointsContract;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.service.ServiceManager;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;
import com.sonymobile.androidapp.gridcomputing.utils.FormatUtils;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to show notifications.
 */
public final class NotificationHelper {

    private NotificationHelper() { }

    /**
     * An identifier for the notification of this application.
     */
    public static final int NOTIFICATION_ID = 1;
    /**
     * Notification button click action string.
     */
    private static final String NOTIFICATION_BUTTON_CLICK_ACTION =
            "com.sonymobile.androidapp.gridcomputing.NOTIFICATION_ACTION";

    /**
     * The request code for the pending intent when the action button is
     * clicked.
     */
    private static final int NOTIFICATION_BUTTON_REQUEST_CODE = 654;
    /**
     * Boradcast receiver called when the user clicks on the notification's
     * power button.
     */
    private static final BroadcastReceiver NOTIFICATION_POWER_RECEIVER =
            new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(final Void... params) {
                            final boolean isPaused = SettingsPref.isPaused();
                            final boolean isEnable = SettingsPref.isExecutionEnabled();
                            if (!isEnable) {
                                NotificationManager notificationManager =
                                        (NotificationManager) context
                                                .getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.cancel(NOTIFICATION_ID);
                                ServiceManager.resume();
                            } else if (!isPaused) {
                                ServiceManager.pause();
                            }

                            return null;
                        }
                    };
                    task.execute();
                }
            };
    private static NotificationStatus sLastShownNotification = NotificationStatus.STATUS_NONE;

    public static Notification buildNotification(final NotificationStatus status) {
        final Context context = ApplicationData.getAppContext();
        final Intent intent = new Intent(NOTIFICATION_BUTTON_CLICK_ACTION);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                NOTIFICATION_BUTTON_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // build notification
        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setContentTitle(context
                .getString(R.string.app_name));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);

        String textNotification = "";
        switch (status) {
            case STATUS_NONE:
                NotificationManager notificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
                break;

            case STATUS_EXECUTING_JOB:
                textNotification = context.getString(R.string.helping_out);
                notificationBuilder.setOngoing(true);
                notificationBuilder.setAutoCancel(true);
                addNotificationAction(notificationBuilder, R.string.pause, pendingIntent);

                context.registerReceiver(NOTIFICATION_POWER_RECEIVER,
                        new IntentFilter(NOTIFICATION_BUTTON_CLICK_ACTION));
                break;

            case STATUS_FINISHED:
                textNotification = context.getString(R.string.notification_contribution_time,
                        FormatUtils.getMainTimeString(Math.min(TimeUnit.DAYS.toMillis(1),
                                JobCheckpointsContract.get24HourAccumulatedTime())));
                notificationBuilder.setOngoing(false);
                notificationBuilder.setAutoCancel(true);

                // If execution has been disabled the notification should
                // show an option to it turn contribution on again.
                if (!SettingsPref.isExecutionEnabled()) {
                    addNotificationAction(notificationBuilder, R.string.turn_on, pendingIntent);
                    context.registerReceiver(NOTIFICATION_POWER_RECEIVER,
                            new IntentFilter(NOTIFICATION_BUTTON_CLICK_ACTION));
                }
                break;
            default:
                break;
        }

        if ("".equals(textNotification)) {
            return null;
        }

        sLastShownNotification = status;

        notificationBuilder.setContentText(textNotification);
        notificationBuilder.setStyle(new Notification.BigTextStyle().bigText(textNotification));

        //Open the SummaryActivity
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, SummaryActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(contentIntent);
        return notificationBuilder.build();
    }

    public static void updateNotification() {
        final Context context = ApplicationData.getAppContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = buildNotification(sLastShownNotification);

        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    public static void showNotification(final NotificationStatus status) {
        final Context context = ApplicationData.getAppContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (status != sLastShownNotification) {
            final Notification notification = buildNotification(status);
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    /**
     * Verifies API and adds Action to Notification.Builder.
     *
     * @param notificationBuilder notification builder.
     * @param strId               String resource ID.
     * @param pIntent             pendingIntent.
     */
    private static void addNotificationAction(final Notification.Builder notificationBuilder,
                                              final int strId, final PendingIntent pIntent) {
        final Context context = ApplicationData.getAppContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Notification.Action.Builder actionBuilder =
                    new Notification.Action.Builder(
                            Icon.createWithResource(context,
                                    R.drawable.ic_notify_power),
                            context.getString(strId), pIntent);
            notificationBuilder.addAction(actionBuilder.build());
        } else {
            notificationBuilder.addAction(R.drawable.ic_notify_power,
                    context.getString(strId), pIntent);
        }
    }
}
