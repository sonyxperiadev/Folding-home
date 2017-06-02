/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.conditions;

/**
 * Enum to represent any condition that changes while the app is running.
 */
public enum ConditionType {
    /**
     * Charger condition.
     */
    CHARGER,
    /**
     * Battery level condition.
     */
    BATTERY,
    /**
     * Wi-Fi status condition.
     */
    WIFI,
    /**
     * Enable button condition.
     */
    ENABLED,
    /**
     * Paused execution condition.
     */
    PAUSED,
    /**
     * Copied assets condition.
     */
    ASSETS,
    /**
     * GTM Kill switch condition.
     */
    KILL_SWITCH,
    /**
     * GTM disabled app condition.
     */
    DISABLED_APP
}
