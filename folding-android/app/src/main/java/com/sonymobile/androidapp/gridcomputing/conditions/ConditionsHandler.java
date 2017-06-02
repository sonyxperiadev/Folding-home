/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.conditions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.SystemClock;

import com.sonymobile.androidapp.gridcomputing.BuildConfig;
import com.sonymobile.androidapp.gridcomputing.assets.AssetCopyListener;
import com.sonymobile.androidapp.gridcomputing.assets.CopyAssets;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.messages.ConditionMessage;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.service.ComputeService;
import com.sonymobile.androidapp.gridcomputing.service.ServiceManager;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class which verifies all the conditions necessary to execute jobs.
 */
public final class ConditionsHandler extends BroadcastReceiver implements AssetCopyListener {

    /**
     * Restricted minimum battery level.
     */
    public static final float MIN_BATTERY_LEVEL = .98f;

    /**
     * Battery level threshold during job execution.
     */
    public static final float MIN_BATTERY_LEVEL_EXEC_THRESHOLD = 0.85f;
    /**
     * Battery level threshold to start executing if the battery level remains between this level
     * and {@link #MIN_BATTERY_LEVEL} for a specific amount of time {@link #MIN_BATTERY_LEVEL}.
     */
    public static final float MIN_BATTERY_LEVEL_PLATEAU = 0.95f;
    /**
     * Time limit to start executing if the battery reamins between
     * {@link #MIN_BATTERY_LEVEL_EXEC_THRESHOLD} and {@link #MIN_BATTERY_LEVEL}.
     */
    public static final float MIN_TIME_PLATEAU = TimeUnit.MINUTES.toMillis(10);

    /**
     * Battery level threshold to force kill the process.
     */
    public static final float MIN_BATTERY_FORCE_KILL = 0.80f;
    /**
     * Max attempts to copy the assets files.
     */
    private static final int COPY_ASSETS_MAX_TRIES = 3;
    /**
     * Wait time to the callback Thread.
     */
    private static final int CONDITION_CALLBACK_WAIT_TIME = 3500;
    /**
     * Wait time to auto check the statuses.
     */
    private static final long AUTO_CHECKER_DELAY = TimeUnit.MINUTES.toMillis(1);
    /**
     * This class' singleton instance.
     */
    private static ConditionsHandler sConditionsHandler;
    /**
     * Counts the number of callbacks made to the ConditionsListener.
     */
    private final AtomicInteger mConditionCallbackCount;
    /**
     * Execution context.
     */
    private Context mContext;
    /**
     * Number of copy assets tries.
     */
    private int mCopyAssetsAttempts;
    /**
     * If the device is plugged to an AC charger.
     */
    private boolean mAcCharging;
    /**
     * If the device is plugged to an USB charger.
     */
    private boolean mUSBCharging;
    /**
     * If the device is charging via wireless charger.
     */
    private boolean mWirelessCharging;
    /**
     * The battery level.
     */
    private float mBatteryLevel;
    /**
     * If the device is in a wifi network.
     */
    private boolean mWifiNetwork;
    /**
     * If the device is in an unmetered network.
     */
    private boolean mWifiUnmetered;
    /**
     * If the assets were copied.
     */
    private boolean mAssetsCopy;
    /**
     * If the assets are still being copied.
     */
    private boolean mAssetsCopyOnProgress;
    /**
     * Thread used to wait the timeout and call the ConditionsListener.
     */
    private Thread mConditionalCallbackThread;

    /**
     * Handler to run an auto update task to refresh all the conditions.
     * This is useful because some devices will not fire power/wifi intent properly.
     */
    private Handler mAutoCheckHandler = new Handler();

    /**
     * Simple constructor.
     */
    private ConditionsHandler() {
        Log.d("ConditionsHandler creating new instance...");
        mContext = ApplicationData.getAppContext();
        mConditionCallbackCount = new AtomicInteger();
        loadInitialConditions();
        registerReceivers();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance of this class.
     */
    public static synchronized ConditionsHandler getInstance() {
        if (sConditionsHandler == null) {
            sConditionsHandler = new ConditionsHandler();
        }
        return sConditionsHandler;
    }

    /**
     * Destroy singleton instance.
     */
    private static void destroyInstance() {
        sConditionsHandler = null;
    }

    /**
     * Clears the listeners and other objects associated with this instance.
     */
    public void shutDown() {
        Log.d("ConditionsHandler shutting down...");
        unregisterReceivers();
        ConditionsHandler.destroyInstance();
        mContext = null;
    }

    /**
     * Copies the files from assets (if not previously copied).
     */
    private void copyAssets() {
        if (mContext != null) {
            final boolean assetsSuccess = CopyAssets.filesCopied();

            if (assetsSuccess) {
                mAssetsCopyOnProgress = false;
                setAssetsCopy(true);
            } else {
                mCopyAssetsAttempts++;
                mAssetsCopyOnProgress = true;
                new Thread(new CopyAssets(mContext, this)).start();

            }
        }
    }

    /**
     * Waits a timeout to avoid multiple calls at the same time to the listener
     * and calls the conditionChanged method from the registered callback in a
     * separated thread to avoid hogging any broadcast.
     * @param dispatchImmediately the flag dispatch Immediately.
     */
    public void notifyConditionChanged(final boolean dispatchImmediately) {
        // This is done in an external thread to avoid hogging a broadcast
        // receiver.
        mConditionCallbackCount.incrementAndGet();

        //how this works:
        //1) There is a dispatch period to avoid flood when some events changes too fast
        // (eg. battery)
        //2) In some cases we want to bypass the dispatch, for instance, when the user
        //   enable/disable the execution because we want the UI to be updated as soon as possible
        //3) The dispatch period is done using a thread, which simply sleeps for a fixed period
        //4) After the period ends (or the thread is interrupted) it posts an event to notify that
        //   any condition has changed
        //5) If any event occurs while the thread sleeps, it will not be posted and the events
        //   will only be posted after the sleep period
        //6) If we want the events to be dispatched immediately then we simply interrupt the thread
        //   which will cause the events to be posted immediately
        if (mConditionalCallbackThread == null || mConditionCallbackCount.get() == 1
                || dispatchImmediately) {
            if (dispatchImmediately && mConditionalCallbackThread != null
                    && mConditionalCallbackThread.isAlive()) {
                mConditionalCallbackThread.interrupt();
            } else {
                mConditionalCallbackThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!dispatchImmediately) {
                            try {
                                Thread.sleep(CONDITION_CALLBACK_WAIT_TIME);
                            } catch (final InterruptedException e) {
                                Log.e(e.getLocalizedMessage());
                            }
                        }

                        mConditionCallbackCount.set(0);
                        final boolean hardStop = !checkEnabledCondition()
                                || mBatteryLevel < MIN_BATTERY_FORCE_KILL && !BuildConfig.TEST_MODE;
                        final List<ConditionType> notMetCondition = getNotMetConditions();
                        final boolean softStop = notMetCondition.size() != 0;

                        if (notMetCondition.size() == 0 && !ComputeService.isExecutingJobs()) {
                            ServiceManager.startComputeService();
                        }

                        Log.d("notifyConditionChanged softStop: "
                                + softStop + " hardStop: " + hardStop);
                        ApplicationData.getBus().postSticky(new ConditionMessage(notMetCondition,
                                hardStop, softStop));

                    }
                });
                mConditionalCallbackThread.start();
            }

        }
    }

    /**
     * Registers all the broadcast receivers necessary to verify if the
     * conditions to execute jobs are met.
     */
    private void registerReceivers() {
        if (mContext != null) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            mContext.registerReceiver(this, filter);
        }
        setAutoChecker(false);
    }

    /**
     * Unregisters the receivers registered by registerReceivers().
     */
    private void unregisterReceivers() {
        if (mContext != null) {
            try {
                mContext.unregisterReceiver(this);
            } catch (final IllegalArgumentException e) {
                Log.e(e.getMessage());
            }
        }
        setAutoChecker(true);
    }

    @Override
    public synchronized void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        boolean notifyChange = false;
        Log.d("ConditionChanged > action: " + action);
        switch (action) {
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
            case ConnectivityManager.CONNECTIVITY_ACTION:
                notifyChange = true;
                updateWifiCondition();
                break;
            case Intent.ACTION_POWER_CONNECTED:
            case Intent.ACTION_POWER_DISCONNECTED:
                notifyChange = true;
                updatePowerCondition();
                //TODO send charger connected event
                //GaTrackerUtils.getInstance().sendChargerConnected(action,
                // (int) (mBatteryLevel * 100));
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                final float currentLevel = readChargeLevel();
                if (mBatteryLevel == currentLevel) {
                    notifyChange = false;
                } else {
                    notifyChange = true;
                    updatePowerCondition();
                }
                break;
            default:
                break;
        }
        if (notifyChange) {
            notifyConditionChanged(false);
        }

    }

    @Override
    public synchronized void onAssetCopySuccess() {
        setAssetsCopy(true);
        mAssetsCopyOnProgress = false;
        notifyConditionChanged(false);
    }

    @Override
    public synchronized void onAssetCopyError() {
        setAssetsCopy(false);
        mAssetsCopyOnProgress = false;
        if (mCopyAssetsAttempts < COPY_ASSETS_MAX_TRIES) {
            copyAssets();
        }
    }

    /**
     * Forces to load all data necessary to verify the conditions and to copy
     * the assets file.
     */
    private void loadInitialConditions() {
        final AsyncTask<Void, Void, Void> mTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                copyAssets();
                updateAllConditions();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                if (!mAssetsCopyOnProgress) {
                    notifyConditionChanged(false);
                }
            }
        };

        mTask.execute();
    }

    /**
     * Loads all data necessary to verify the conditions.
     */
    private void updateAllConditions() {
        updatePowerCondition();
        updateWifiCondition();
    }

    /**
     * Calculates the battery level.
     *
     * @param batteryIntent an Intent using the filter
     *                      Intent.ACTION_BATTERY_CHANGED.
     * @return the battery level in percentage (0.00, 1.00).
     */
    private float readChargeLevel(final Intent batteryIntent) {
        final int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    /**
     * Calculates the battery level.
     *
     * @return the battery level in percentage (0.00, 1.00).
     */
    private float readChargeLevel() {
        final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent batteryIntent = mContext.registerReceiver(null, ifilter);
        return readChargeLevel(batteryIntent);
    }

    /**
     * Updates the power conditions: Plugged in the AC/USB Charger and the
     * battery level.
     */
    private void updatePowerCondition() {
        final AsyncTask<Void, Void, Void> mTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                if (mContext != null) {
                    final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    final Intent batteryStatus = mContext.registerReceiver(null, ifilter);
                    int plugged = -1;
                    if (batteryStatus != null) {
                        plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    }
                    final boolean acCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC;
                    final boolean usbCharging = plugged == BatteryManager.BATTERY_PLUGGED_USB;
                    final boolean wirelessCharging =
                            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

                    setAcCharging(acCharging);
                    setUSBCharging(usbCharging);
                    setWirelessCharging(wirelessCharging);

                    final float chargelevel = readChargeLevel(batteryStatus);
                    setBatteryLevel(chargelevel);
                }
                return null;
            }
        };

        mTask.execute();
    }

    /**
     * Updates the Wifi conditions: connected to a wifi network and connected to
     * an unmetered network.
     */
    private void updateWifiCondition() {
        final AsyncTask<Void, Void, Void> mTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                if (mContext != null) {
                    final ConnectivityManager connManager = (ConnectivityManager) mContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);

                    final NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();

                    // We only want WIFI networks.
                    final boolean inWifiNetwork = activeNetwork != null && activeNetwork
                            .getType() == ConnectivityManager.TYPE_WIFI;
                    setWifiNetwork(inWifiNetwork);

                    // We don't want metered networks.
                    final boolean isMetered = connManager.isActiveNetworkMetered();
                    setUnmeteredNetwork(!isMetered);
                }
                return null;
            }
        };

        mTask.execute();
    }

    /**
     * Sets a flag indicating if the assets were copied successfully.
     *
     * @param assetsCondition true if the assets were copied, false otherwise.
     */
    public void setAssetsCopy(final boolean assetsCondition) {
        mAssetsCopy = assetsCondition;
    }

    /**
     * Sets the battery level.
     *
     * @param batteryLevel the battery level to be set.
     */
    public void setBatteryLevel(final float batteryLevel) {
        mBatteryLevel = batteryLevel;
        if (batteryLevel < MIN_BATTERY_LEVEL_PLATEAU || batteryLevel >= MIN_BATTERY_LEVEL) {
            MiscPref.setLastBatteryPlateauTime(0L);
        } else if (MiscPref.getLastBatteryPlateauTime() == 0) {
            MiscPref.setLastBatteryPlateauTime(SystemClock.elapsedRealtime());
        }
    }

    /**
     * Gets if the device is in an unmetered network.
     *
     * @return if the device is in an unmetered network.
     */
    public boolean isUnmeteredNetwork() {
        return mWifiUnmetered;
    }

    /**
     * Sets if the device is in an unmetered network.
     *
     * @param unmetered if the device is in an unmetered network.
     */
    public void setUnmeteredNetwork(final boolean unmetered) {
        mWifiUnmetered = unmetered;
    }

    /**
     * Gets if the device is in a wifi network.
     *
     * @return if the device is in a wifi network.
     */
    public boolean isWifiNetwork() {
        return mWifiNetwork;
    }

    /**
     * Sets if the device is in a wifi network.
     *
     * @param wifiNetwork if the device is in a wifi network.
     */
    public void setWifiNetwork(final boolean wifiNetwork) {
        mWifiNetwork = wifiNetwork;
    }

    /**
     * Gets if the device is plugged to an AC charger.
     *
     * @return if the device is plugged to an AC charger.
     */
    public boolean isAcCharging() {
        return mAcCharging;
    }

    /**
     * Sets if the device is plugged to an AC charger.
     *
     * @param acCharging if the device is plugged to an AC charger.
     */
    public void setAcCharging(final boolean acCharging) {
        mAcCharging = acCharging;
    }

    /**
     * Gets if the device is plugged to an USB charger.
     *
     * @return if the device is plugged to an USB charger.
     */
    public boolean isUSBCharging() {
        return mUSBCharging;
    }

    /**
     * Sets if the device is plugged to an USB charger.
     *
     * @param usbCharging if the device is plugged to an USB charger.
     */
    public void setUSBCharging(final boolean usbCharging) {
        mUSBCharging = usbCharging;
    }

    /**
     * Gets if the device is charging via wireless charger.
     *
     * @return if the device is charging via wireless charger.
     */
    public boolean isWirelessCharging() {
        return mWirelessCharging;
    }

    /**
     * Sets if the device is charging via wireless charger.
     *
     * @param wirelessCharging if the device is charging via wireless charger.
     */
    public void setWirelessCharging(final boolean wirelessCharging) {
        mWirelessCharging = wirelessCharging;
    }

    /**
     * Checks if the execution is enabled.
     *
     * @return if the execution is enabled.
     */
    public boolean checkEnabledCondition() {
        return SettingsPref.isExecutionEnabled();
    }

    /**
     * Checks if the execution has been paused by the user.
     * @return true if the execution has been paused by the user, false otherwise.
     */
    public boolean isPaused() {
        return SettingsPref.isPaused();
    }

    /**
     * Checks if the current battery level condition is met.
     *
     * @return true if the service has just started, check if the current
     * battery level is >= than RunningSettings.MIN_BATTERY_LEVEL, if it
     * has already started to execute jobs, verify if battery level is
     * >= RunningSettings.MIN_BATTERY_LEVEL_EXEC_THERESHOLD .
     */
    public boolean checkBatteryCondition() {
        boolean result;
        if (ComputeService.isExecutingJobs()) {
            result = mBatteryLevel > MIN_BATTERY_LEVEL_EXEC_THRESHOLD;
        } else if (mBatteryLevel >= MIN_BATTERY_LEVEL_PLATEAU && mBatteryLevel < MIN_BATTERY_LEVEL) {
            final long lastTime = MiscPref.getLastBatteryPlateauTime();
            final long currentTime = SystemClock.elapsedRealtime();
            result = lastTime > 0 && (currentTime - lastTime) >= MIN_TIME_PLATEAU;
        } else {
            result = mBatteryLevel >= MIN_BATTERY_LEVEL;
        }
        return result;
    }

    /**
     * Set a handler to run an auto checker task.
     * @param stop true if the checker should stop, false otherwise.
     */
    private void setAutoChecker(final boolean stop) {
        if (stop) {
            mAutoCheckHandler.removeCallbacksAndMessages(null);
        } else {
            mAutoCheckHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateAllConditions();
                    setAutoChecker(false);
                    notifyConditionChanged(false);
                }
            }, AUTO_CHECKER_DELAY);
        }
    }

    /**
     * Checks all the power condition.
     *
     * @return true if the battery condition is met and if the device is plugged
     * in the AC charger.
     */
    public boolean checkChargerCondition() {
        return isAcCharging() || isWirelessCharging();
    }

    /**
     * Checks if the assets were copied successfully.
     *
     * @return true if the assets were copied successfully.
     */
    public boolean checkAssetsCondition() {
        return mAssetsCopy;
    }

    /**
     * Checks if the wifi condition is met.
     *
     * @return true if the device is connected to a wifi and unmetered network.
     */
    public boolean checkUnmeteredWifiCondition() {
        return mWifiUnmetered && mWifiNetwork;
    }

    /**
     * Checks all the conditions except wifi.
     *
     * @return true if all the conditions are met or false
     * otherwise.
     */
    public boolean checkAllConditions() {
        return getNotMetConditions().size() == 0;
    }

    /**
     * Get all conditions that are not met.
     *
     * @return a list with not met conditions.
     */
    public List<ConditionType> getNotMetConditions() {
        final boolean assetsCondition = checkAssetsCondition();
        final boolean enabledCondition = checkEnabledCondition();
        final boolean wifiCondition = checkUnmeteredWifiCondition();
        final boolean chargerCondition = BuildConfig.TEST_MODE || checkChargerCondition();
        final boolean batteryCondition = BuildConfig.TEST_MODE || checkBatteryCondition();
        final boolean pausedCondition = !isPaused();

        final List<ConditionType> notSatisfiedConditions = new ArrayList<>();
        if (!assetsCondition) {
            notSatisfiedConditions.add(ConditionType.ASSETS);
        }
        if (!pausedCondition) {
            notSatisfiedConditions.add(ConditionType.PAUSED);
        }
        if (!enabledCondition) {
            notSatisfiedConditions.add(ConditionType.ENABLED);
        }
        if (!wifiCondition) {
            notSatisfiedConditions.add(ConditionType.WIFI);
        }
        if (!chargerCondition) {
            MiscPref.setLastBatteryPlateauTime(0);
            notSatisfiedConditions.add(ConditionType.CHARGER);
        }
        if (!batteryCondition) {
            notSatisfiedConditions.add(ConditionType.BATTERY);
        }

        Log.d("Checking > Conditions not met: " + notSatisfiedConditions);

        return notSatisfiedConditions;
    }
}
