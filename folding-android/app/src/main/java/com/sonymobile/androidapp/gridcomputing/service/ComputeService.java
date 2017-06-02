/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.sonymobile.androidapp.gridcomputing.conditions.ConditionsHandler;
import com.sonymobile.androidapp.gridcomputing.database.JobCheckpointsContract;
import com.sonymobile.androidapp.gridcomputing.gamification.GameHelper;
import com.sonymobile.androidapp.gridcomputing.gamification.Scores;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.messages.ConditionMessage;
import com.sonymobile.androidapp.gridcomputing.messages.JobExecutionMessage;
import com.sonymobile.androidapp.gridcomputing.notifications.NotificationHelper;
import com.sonymobile.androidapp.gridcomputing.notifications.NotificationStatus;
import com.sonymobile.androidapp.gridcomputing.preferences.GamePref;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;
import com.sonymobile.androidapp.gridcomputing.utils.JSONUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

/**
 * Class that manages the application service.
 */
public class ComputeService extends Service implements JobExecutionListener {

    /**
     * Time update internal.
     */
    private static final long TIME_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    /**
     * Flag representing if the job is running.
     */
    private static final AtomicBoolean EXECUTING_JOBS = new AtomicBoolean(false);
    /**
     * Flag representing if is running foreground.
     */
    private static final AtomicBoolean RUNNING_FOREGROUND = new AtomicBoolean(false);
    /**
     * Update time handler.
     */
    private final Handler mUpdateTimeHandler = new Handler();
    /**
     * Compute environment.
     */
    private ComputeEnvironment mEnvironment;

    /**
     * Last time.
     */
    private long mLastTime;
    /**
     * Runnable to update time.
     */
    private final Runnable mUpdateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            mUpdateTimeHandler.postDelayed(mUpdateTimeRunnable, TIME_UPDATE_INTERVAL);
            final long currentTime = SystemClock.elapsedRealtime();
            final long elapsedTime = currentTime - mLastTime;
            mLastTime = currentTime;

            RunningPref.incrementAccumulatedTime(elapsedTime);
            GamePref.incrementScoreToSubmit(elapsedTime);
            JobCheckpointsContract.addCheckpoint(elapsedTime);

            Scores.submitScore(GameHelper.getApiClient());
            sendDetailsMessage();
        }
    };

    /**
     * Checks if the job is executed.
     * @return the true if the job is running.
     */
    public static boolean isExecutingJobs() {
        return EXECUTING_JOBS.get();
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public final int onStartCommand(final Intent intent, final int flags,
                                    final int startId) {
        return START_STICKY;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        Log.d("Service > Creating service");

        mEnvironment = new ComputeEnvironment(this, this);
        mEnvironment.runJob();
        mLastTime = SystemClock.elapsedRealtime();
        mUpdateTimeHandler.postDelayed(mUpdateTimeRunnable, TIME_UPDATE_INTERVAL);

        EXECUTING_JOBS.set(true);

        ApplicationData.getBus().registerSticky(this);
        setForeground(true);
    }

    @Override
    public final void onDestroy() {
        Log.d("Service > Destroying service");
        ApplicationData.getBus().unregister(this);
        mUpdateTimeHandler.removeCallbacks(mUpdateTimeRunnable);
        EXECUTING_JOBS.set(false);
        sendDetailsMessage();

        if (ConditionsHandler.getInstance().checkEnabledCondition()) {
            stopForeground(true);
        } else {
            stopForeground(false);
        }
    }

    @Override
    public void numberOfUsersReceived(final long number) {
        Log.d("Service > numberOfUsersReceived: " + number);
        RunningPref.setNumberOfUsers(number);
        sendDetailsMessage();
    }

    @Override
    public void researchDetailsReceived(final JSONObject content) {
        Log.d("Service > researchDetailsReceived: " + content);
        final String title = JSONUtils.getString(content, "title", "");
        final String url = JSONUtils.getString(content, "url", "");
        final String id = JSONUtils.getString(content, "target_id", "");
        final String description = JSONUtils.getString(content, "description", "");
        RunningPref.setResearchType(title);
        RunningPref.setResearchUrl(url);
        RunningPref.setResearchId(id);
        RunningPref.setResearchDescription(description);
        sendDetailsMessage();
    }

    @Override
    public void clientStopped() {
        Log.d("Service > clientStopped");
        stopSelf();
    }

    /**
     * Sets the flag run in foreground.
     * @param runInForeground the flag run in foreground.
     */
    private void setForeground(final boolean runInForeground) {
        if (runInForeground) {
            final Notification notification =
                    NotificationHelper.buildNotification(NotificationStatus.STATUS_EXECUTING_JOB);
            if (notification != null && !RUNNING_FOREGROUND.get()) {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification);
                mUpdateTimeHandler.removeCallbacks(mUpdateTimeRunnable);
                mUpdateTimeHandler.postDelayed(mUpdateTimeRunnable, TIME_UPDATE_INTERVAL);
            }
            RUNNING_FOREGROUND.set(true);
        } else {
            mUpdateTimeHandler.removeCallbacks(mUpdateTimeRunnable);
            stopForeground(true);
            RUNNING_FOREGROUND.set(false);
        }
    }

    /**
     * Send details message.
     */
    private void sendDetailsMessage() {
        ApplicationData.getBus().post(new JobExecutionMessage(
                RunningPref.getNumberOfUsers(),
                RunningPref.getResearchType(),
                RunningPref.getAccumulatedTime()));
    }

    @SuppressWarnings("unused")
    public void onEventBackgroundThread(final ConditionMessage message) {
        if (message.isHardStop() || message.isSoftStop()) {
            String conditionsStr = '<' + TextUtils.join(",", message.getNotMetConditions()) + '>';
        }

        if (message.isHardStop()) {
            mEnvironment.conditionChanged(false, true);
            setForeground(false);
        } else if (message.isSoftStop()) {
            mEnvironment.conditionChanged(false, false);
            setForeground(false);
        } else {
            mEnvironment.conditionChanged(true, false);
            setForeground(true);
        }
    }
}
