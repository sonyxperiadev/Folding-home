/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.preferences;

/**
 * Preferences related to the job execution.
 */
public final class RunningPref {
    /**
     * Shared preferences running preferences file name.
     */
    public static final String PREF_FILE = "running_pref";
    /**
     * Shared preferences key for the accumulated folding time.
     */
    public static final String ACCUMULATED_TIME_KEY = "ACCUMULATED_TIME_KEY";

    /**
     * Constant for the minimum number of users using Folding@Home app.
     */
    public static final long MIN_NUMBER_OF_USERS = 1L;
    /**
     * Shared preferences key for the research title.
     */
    public static final String RESEARCH_TYPE = "RESEARCH_TYPE";
    /**
     * Shared preferences key for the number of users.
     */
    private static final String NUMBER_OF_USERS = "NUMBER_OF_USERS";
    /**
     * Shared preferences key for the research description url.
     */
    private static final String RESEARCH_URL = "RESEARCH_URL";
    /**
     * Shared preferences key for the research id.
     */
    private static final String RESEARCH_ID = "RESEARCH_ID";
    /**
     * Shared preferences key for the research description.
     */
    private static final String RESEARCH_DESCRIPTION = "RESEARCH_DESCRIPTION";

    private RunningPref() { }

    /**
     * Returns the user accumulated folding time.
     *
     * @return the user accumulated folding time.
     */
    public static long getAccumulatedTime() {
        return PrefUtils.getLongValue(PREF_FILE, ACCUMULATED_TIME_KEY, 0L);
    }

    /**
     * Sets the user accumulated folding time.
     *
     * @param time accumulated folding time param.
     */
    public static void setAccumulatedTime(final long time) {
        PrefUtils.setLongValue(PREF_FILE, ACCUMULATED_TIME_KEY, time);
    }

    /**
     * Increments accumulated time by the specified amount of time.
     *
     * @param time amount of the to increment on the accumulated time.
     */
    public static void incrementAccumulatedTime(final long time) {
        PrefUtils.setLongValue(PREF_FILE, ACCUMULATED_TIME_KEY, getAccumulatedTime() + time);
    }

    /**
     * Gets the number of users contributing.
     *
     * @return the number of users contributing
     */
    public static long getNumberOfUsers() {
        return PrefUtils.getLongValue(PREF_FILE, NUMBER_OF_USERS, MIN_NUMBER_OF_USERS);
    }

    /**
     * Sets the number of users contributing.
     *
     * @param users the number of users contributing
     */
    public static void setNumberOfUsers(final long users) {
        PrefUtils.setLongValue(PREF_FILE, NUMBER_OF_USERS, users);
    }

    /**
     * Gets the research type.
     *
     * @return the research type
     */
    public static String getResearchType() {
        return PrefUtils.getStringValue(PREF_FILE, RESEARCH_TYPE, "");
    }

    /**
     * Sets the research type.
     *
     * @param type the research type
     */
    public static void setResearchType(final String type) {
        PrefUtils.setStringValue(PREF_FILE, RESEARCH_TYPE, type);
    }

    /**
     * Gets the research url.
     *
     * @return the research url
     */
    public static String getResearchUrl() {
        return PrefUtils.getStringValue(PREF_FILE, RESEARCH_URL, "");
    }

    /**
     * Sets the research description url.
     *
     * @param researchUrl the research url.
     */
    public static void setResearchUrl(final String researchUrl) {
        PrefUtils.setStringValue(PREF_FILE, RESEARCH_URL, researchUrl);
    }

    /**
     * Gets the research id.
     *
     * @return the research id
     */
    public static String getResearchId() {
        return PrefUtils.getStringValue(PREF_FILE, RESEARCH_ID, "");
    }

    /**
     * Sets the research id.
     *
     * @param researchId the research id.
     */
    public static void setResearchId(final String researchId) {
        PrefUtils.setStringValue(PREF_FILE, RESEARCH_ID, researchId);
    }

    /**
     * Gets the research description.
     *
     * @return the research description
     */
    public static String getResearchDescription() {
        return PrefUtils.getStringValue(PREF_FILE, RESEARCH_DESCRIPTION, "");
    }

    /**
     * Sets the research description.
     *
     * @param researchDescription the research description.
     */
    public static void setResearchDescription(final String researchDescription) {
        PrefUtils.setStringValue(PREF_FILE, RESEARCH_DESCRIPTION, researchDescription);
    }
}
