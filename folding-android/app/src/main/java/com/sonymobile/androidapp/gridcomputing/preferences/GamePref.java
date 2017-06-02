/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.preferences;

import com.sonymobile.androidapp.gridcomputing.gamification.GameHelper;
import com.sonymobile.androidapp.gridcomputing.utils.CipherUtils;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Preferences of gamification.
 */
public final class GamePref {

    /**
     * Shared preferences key for the last submitted contribution time.
     */
    public static final String JOB_LAST_SUBMITED_TIME_KEY = "JOB_LAST_SUBMITED_TIME_KEY_ENCR";

    /**
     * Preference name of the game.
     */
    public static final String PREF_FILE = "game_pref";

    /**
     * Preferences key for the flag signed out.
     */
    private static final String SIGNED_OUT_EXPLICITLY = "signed_out_explicitly";

    /**
     * Flag signed out explicitly.
     */
    private static AtomicBoolean sSignedOut;

    /**
     * This class is not intended to be instantiated.
     */
    private GamePref() {
    }

    /**
     * Gets the flag signed out explicitly.
     *
     * @return the flag signed out explicitly
     */
    public static synchronized boolean getSignedOutExplicitly() {
        if (sSignedOut == null) {
            sSignedOut = new AtomicBoolean(
                    PrefUtils.getBooleanValue(PREF_FILE, SIGNED_OUT_EXPLICITLY, true));
        }
        return sSignedOut.get();
    }

    /**
     * Sets the flag signed out explicitly.
     *
     * @param signedOutExplicitly the flag signed out explicitly
     */
    public static synchronized void setSignedOutExplicitly(final boolean signedOutExplicitly) {
        if (sSignedOut == null) {
            sSignedOut = new AtomicBoolean(signedOutExplicitly);
        } else {
            sSignedOut.set(signedOutExplicitly);
        }
        PrefUtils.setBooleanValue(PREF_FILE, SIGNED_OUT_EXPLICITLY, signedOutExplicitly);

        if (signedOutExplicitly && GameHelper.getApiClient().isConnected()) {
            GameHelper.getApiClient().clearDefaultAccountAndReconnect();
        }
    }

    /**
     * Gets the score (execution time to submit to the games service).
     *
     * @return the total time to be sent to the games service.
     */
    public static long getScoreToSubmit() {
        final String encrypted =
                PrefUtils.getStringValue(PREF_FILE, JOB_LAST_SUBMITED_TIME_KEY, "0L");
        final String decrypted = CipherUtils.decrypt(encrypted);
        try {
            return Long.parseLong(decrypted);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Increments the score (execution time to send to the games services).
     *
     * @param value the to increment by the execution time
     */
    public static void incrementScoreToSubmit(final long value) {
        final long lastTime = getScoreToSubmit();
        final String newTimeStr = String.valueOf(lastTime + value);
        PrefUtils.setStringValue(PREF_FILE, JOB_LAST_SUBMITED_TIME_KEY,
                CipherUtils.encrypt(newTimeStr));
    }

    /**
     * Resets the score (execution time to send to the games services).
     */
    public static void resetScore() {
        PrefUtils.setStringValue(PREF_FILE, JOB_LAST_SUBMITED_TIME_KEY, "");
    }

}
