/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.service;

import org.json.JSONObject;

/**
 * JobExecutionListener defines methods to invoke
 * during job execution on events triggered by specific
 * action in the job description file.
 */
public interface JobExecutionListener {

    /**
     * Set number of users to job preferences.
     *
     * @param number Number of users set by job.
     */
    void numberOfUsersReceived(long number);

    /**
     * The details of the research job, e.g. title,
     * url and such, depending on the job.
     *
     * @param content JSON file containing the job details.
     */
    void researchDetailsReceived(JSONObject content);

    /**
     * Action triggered when client stops (regardless of
     * reason for the stop).
     */
    void clientStopped();
}
