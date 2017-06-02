/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
package com.sonymobile.androidapp.gridcomputing.messages;

/**
 * Message sent using the Event bus to notify that the job execution details has changed.
 */
public class JobExecutionMessage {
    /**
     * Number of users associated with the job.
     */
    private final long mNumberOfUsers;
    /**
     * Title of the current job.
     */
    private final String mTitle;
    /**
     * Local contributed time.
     */
    private final long mContributedTime;

    /**
     * Default constructor.
     *
     * @param numberOfUsers   the number of users associated with the job.
     * @param title           the title of the current job.
     * @param contributedTime local contributed time.
     */
    public JobExecutionMessage(final long numberOfUsers, final String title,
                               final long contributedTime) {
        mNumberOfUsers = numberOfUsers;
        mTitle = title;
        mContributedTime = contributedTime;
    }

    /**
     * Gets the current job's title.
     *
     * @return The title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Gets the number of users associated with the job.
     *
     * @return the number of users associated with the job.
     */
    public long getNumberOfUsers() {
        return mNumberOfUsers;
    }

    /**
     * Gets the local contributed time.
     *
     * @return local contributed time.
     */
    public long getContributedTime() {
        return mContributedTime;
    }
}
