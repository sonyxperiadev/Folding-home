/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.utils;

import android.content.res.Resources;

public class NativeInterfaceUtils {

    public static int getNativeTimePickerIdFromDialog() {
        return Resources.getSystem().getIdentifier("timePicker", "id", "android");
    }

    public static String getNativeDateSetStringFromDialog() {
        return Resources.getSystem().getString(
                Resources.getSystem().getIdentifier("date_time_set", "string", "android"));
    }

    public static String getNativeDateDoneStringFromDialog() {
        return Resources.getSystem().getString(
                Resources.getSystem().getIdentifier("date_time_done", "string", "android"));
    }

    public static String getNativeDateCancelStringFromDialog() {
        return Resources.getSystem().getString(
                Resources.getSystem().getIdentifier("cancel", "string", "android"));
    }
}
