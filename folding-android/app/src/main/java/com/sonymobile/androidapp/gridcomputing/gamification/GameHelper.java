/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.gamification;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.sonymobile.androidapp.gridcomputing.preferences.GamePref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * Class with helper methods associated to the Games Service.
 */
public final class GameHelper {

    /**
     * GoogleApiClient used to connect to the Games service.
     */
    private static GoogleApiClient sGoogleApiClient;

    /**
     * Private constructor.
     */
    private GameHelper() { }

    /**
     * Gets the google api singleton instance.
     *
     * @return the google api singleton instance.
     */
    public static synchronized GoogleApiClient getApiClient() {
        if (sGoogleApiClient == null) {
            final GoogleApiClient.Builder builder =
                    new GoogleApiClient.Builder(ApplicationData.getAppContext())
                            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                            .addApi(Games.API).addScope(Games.SCOPE_GAMES);
            sGoogleApiClient = builder.build();
        }
        return sGoogleApiClient;
    }

    /**
     * Registers connection callbacks.
     *
     * @param connectionCallbacks      the  listener to receive connection events.
     * @param connectionFailedListener the listener to receive connection failed events.
     */
    public static void registerCallbacks(final GoogleApiClient.ConnectionCallbacks
                                                 connectionCallbacks,
                                         final GoogleApiClient.OnConnectionFailedListener
                                                 connectionFailedListener) {
        getApiClient().registerConnectionCallbacks(connectionCallbacks);
        getApiClient().registerConnectionFailedListener(connectionFailedListener);
    }

    /**
     * Registers connection callbacks.
     *
     * @param connectionCallbacks      the  listener to receive connection events.
     * @param connectionFailedListener the listener to receive connection failed events.
     */
    public static void unregisterCallbacks(final GoogleApiClient.ConnectionCallbacks
                                                   connectionCallbacks,
                                           final GoogleApiClient.OnConnectionFailedListener
                                                   connectionFailedListener) {
        getApiClient().unregisterConnectionCallbacks(connectionCallbacks);
        getApiClient().unregisterConnectionFailedListener(connectionFailedListener);
    }


    /**
     * Tries to connect with the Google Game service.
     *
     * @return true if tried to connect (was previously disconnected).
     */
    public static boolean tryToConnect() {
        if (!getApiClient().isConnected() && !getApiClient().isConnecting()
                && !GamePref.getSignedOutExplicitly()) {
            GameHelper.getApiClient().connect();
            return true;
        }
        return false;
    }

    /**
     * Tries to disconnect from the Google Game service.
     *
     * @return true if tried to disconnect (was previously connected).
     */
    public static boolean tryToDisconnect() {
        boolean returnValue = false;
        GamePref.setSignedOutExplicitly(true);
        try {
            if (getApiClient().isConnected()) {
                Games.signOut(GameHelper.getApiClient());
                getApiClient().disconnect();
                returnValue = true;
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
            returnValue = true;
        } catch (IllegalStateException ex) {
            returnValue = true;
        }
        return returnValue;
    }

}
