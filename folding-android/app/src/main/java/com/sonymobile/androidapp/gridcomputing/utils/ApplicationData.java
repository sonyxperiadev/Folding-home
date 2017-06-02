/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.facebook.FacebookSdk;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;

import de.greenrobot.event.EventBus;

/**
 * Stores the application context.
 */
public class ApplicationData extends Application {

    /**
     * The application context.
     */
    private static Context sContext;

    /**
     * Event bus used to send messages across the application.
     */
    private static EventBus sEventBus;
    /**
     * System default exception handler.
     */
    private Thread.UncaughtExceptionHandler mDefaultUEH;
    /**
     * Custom exception handler.
     */
    private final Thread.UncaughtExceptionHandler mCaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final Thread thread, final Throwable ex) {
                    String classpath = null;
                    //ONLY ignores the "Results have already been set" exception from GTM
                    if (ex != null && ex.getStackTrace().length > 0) {
                        classpath = ex.getStackTrace()[0].toString();
                    }
                    final boolean gtmException = classpath != null
                            && ex.getMessage().contains("Results have already been set")
                            && classpath.contains("com.google.android.gms.tagmanager");
                    if (!gtmException) {
                        // run default handler
                        mDefaultUEH.uncaughtException(thread, ex);
                    }
                }
            };

    /**
     * Returns an application context.
     *
     * @return an application context.
     */
    public static Context getAppContext() {
        return sContext;
    }

    /**
     * Gets the event bus.
     *
     * @return the event bus.
     */
    public static EventBus getBus() {
        return sEventBus;
    }

    /**
     * Checks if the environment is JUnit this means, if the application
     * is running through JUnit.
     * @return true if the application is running from JUnit.
     */
    public static boolean isJUnit() {
        return "JUnit".equals(System.getProperty("Env"));
    }

    /**
     * Inits event bus instance.
     */
    private static void initEventBus() {
        sEventBus = new EventBus();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public final void onCreate() {
        super.onCreate();
        setContext();

        // Init Facebook SDK.
        FacebookSdk.sdkInitialize(getAppContext());

        // for catching app global unhandled exceptions
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);

        //Fabric.with(getAppContext(), new Crashlytics());

        initEventBus();

        MiscPref.setLastBatteryPlateauTime(0);
    }

    /**
     * Sets the app context.
     */
    private void setContext() {
        sContext = this;
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
