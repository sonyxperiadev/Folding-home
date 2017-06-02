/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.messages;

import com.sonymobile.androidapp.gridcomputing.conditions.ConditionType;

import java.util.List;

/**
 * Message sent using the Event bus to notify that the conditions has changed.
 */
public class ConditionMessage {
    /**
     * List of not met conditions.
     */
    private List<ConditionType> mNotMetConditions;
    /**
     * true if the service must stop immediately.
     */
    private boolean mHardStop;
    /**
     * true if the service must stop gracefully.
     */
    private boolean mSoftStop;

    /**
     * Default constructor.
     *
     * @param notMetConditions list of not met conditions.
     * @param hardStop         true if the service must stop immediately.
     * @param softStop         true if the service must stop gracefully.
     */
    public ConditionMessage(final List<ConditionType> notMetConditions,
                            final boolean hardStop, final boolean softStop) {
        this.mNotMetConditions = notMetConditions;
        this.mHardStop = hardStop;
        this.mSoftStop = softStop;
    }

    /**
     * Gets the list of not met conditions.
     *
     * @return list of not met conditions.
     */
    public final List<ConditionType> getNotMetConditions() {
        return mNotMetConditions;
    }

    /**
     * Checks if the service must stop immediately.
     *
     * @return true if the service must stop immediately.
     */
    public final boolean isHardStop() {
        return mHardStop;
    }

    /**
     * Checks if the service must stop gracefully.
     *
     * @return true if the service must stop gracefully.
     */
    public final boolean isSoftStop() {
        return mSoftStop;
    }
}
