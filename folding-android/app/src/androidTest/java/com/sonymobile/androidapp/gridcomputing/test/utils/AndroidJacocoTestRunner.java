/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.utils;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnitRunner;

import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.test.BuildConfig;

import java.lang.reflect.Method;

public class AndroidJacocoTestRunner extends AndroidJUnitRunner {

    static {
        System.setProperty("jacoco-agent.destfile", "/data/data/"
                               + BuildConfig.APPLICATION_ID + "/coverage.ec");
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        try {
            Class rt = Class.forName("org.jacoco.agent.rt.RT");
            Method getAgent = rt.getMethod("getAgent");
            Method dump = getAgent.getReturnType().getMethod("dump", boolean.class);
            Object agent = getAgent.invoke(null);
            dump.invoke(agent, false);
        } catch (Throwable e) {
            Log.d(e.getMessage());
        }
        super.finish(resultCode, results);
    }
}

