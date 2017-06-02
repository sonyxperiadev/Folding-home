/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test;

import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.database.JobCheckpointsContract;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DataBaseTest{

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
    }

    @Test
    public void testWrite() throws InterruptedException {
        JobCheckpointsContract.clear24HourAccumulatedTime();
        assertEquals(0, JobCheckpointsContract.get24HourAccumulatedTime());

        Thread.sleep(100);
        JobCheckpointsContract.addCheckpoint(1000);
        assertEquals(1000, JobCheckpointsContract.get24HourAccumulatedTime());

        Thread.sleep(100);
        JobCheckpointsContract.addCheckpoint(1000);
        assertEquals(2000, JobCheckpointsContract.get24HourAccumulatedTime());

        Thread.sleep(100);
        JobCheckpointsContract.addCheckpoint(1000);
        assertEquals(3000, JobCheckpointsContract.get24HourAccumulatedTime());

        Thread.sleep(100);
        JobCheckpointsContract.clear24HourAccumulatedTime();
        assertEquals(3000, JobCheckpointsContract.get24HourAccumulatedTime());
    }

}
