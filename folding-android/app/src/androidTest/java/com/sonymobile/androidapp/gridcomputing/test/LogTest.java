/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test;

import android.os.Environment;
import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.log.LogUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class LogTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
    }

    @Test
    public void testLog() throws InterruptedException {
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File logDir = new File(externalDir + File.separator + LogUtil.LOG_FOLDER);
        logDir.mkdirs();
        deleteFiles(logDir);
        logDir.delete();

        LogUtil.log("test...");
        LogUtil.log("test...");
        Thread.sleep(5000);

        Log.setLogging(true);
        Log.e("error message");
        Log.d("debug message");
    }

    private void deleteFiles(File parent) {
        String[] entries = parent.list();
        if (entries != null) {
            for (String s : entries) {
                File currentFile = new File(parent.getAbsolutePath(), s);
                currentFile.delete();
            }
        }
    }

}
