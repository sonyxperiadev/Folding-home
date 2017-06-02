/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.views.CheckableImageButton;
import com.sonymobile.androidapp.gridcomputing.views.StatusProgressBar;

/**
 * Utility class used by the views.
 */
public final class ViewUtils {

    /**
     * This class is not intended to be instantiated.
     */
    private ViewUtils() {
    }

    /**
     * Returns the accent color.
     *
     * @param context Context used to get the resources.
     * @return The accent color or holo_blue_light (if the accent color isn't
     * available).
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static int getAccentColor(final Context context) {
        int color = ContextCompat.getColor(context, android.R.color.holo_blue_light);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            color = typedValue.data;
        } else {
            final int resId = context.getResources().getIdentifier("somc_theme_accent_color_light",
                    "color", "com.sonyericsson.uxp");
            if (resId != 0) {
                color = ContextCompat.getColor(context, resId);
            }
        }
        return color;
    }

    /**
     * Updates the status bar with the current conditions.
     *
     * @param parent  parent view.
     * @param enabled the enabled condition.
     * @param paused  the paused condition.
     * @param battery the battery condition.
     * @param charger the charger condition.
     * @param wifi    the wifi condition.
     */
    public static void updateStatusBar(final View parent, final boolean enabled,
                                       final boolean paused, final boolean battery,
                                       final boolean charger, final boolean wifi) {
        final boolean progress = battery && charger && wifi && !paused;

        final CheckableImageButton enableView = (CheckableImageButton) parent
                .findViewById(R.id.summary_menu_power_toggle);
        final View batteryView = parent.findViewById(R.id.iv_battery);
        final View chargerView = parent.findViewById(R.id.iv_charger);
        final View wifiView = parent.findViewById(R.id.iv_wifi);
        final StatusProgressBar progressView =
                (StatusProgressBar) parent.findViewById(R.id.progress);
        //final View standByProgressView = parent.findViewById(R.id.progress_stand_by);
        final TextView textView = (TextView) parent.findViewById(R.id.summary_status_text);

        boolean running = enabled && !paused;

        enableView.setChecked(running && progress);
        batteryView.setEnabled(running);
        chargerView.setEnabled(running);
        wifiView.setEnabled(running);
        textView.setEnabled(running);

        batteryView.setActivated(running && battery);
        chargerView.setActivated(running && charger);
        wifiView.setActivated(running && wifi);
        textView.setActivated(running && progress);


        if (running) {
            enableView.setImageResource(R.drawable.power_bt);
            if (progress) {
                textView.setText(R.string.helping_out);
                progressView.changeMode(StatusProgressBar.ProgressStatus.RUNNING);
            } else {
                textView.setText(R.string.stand_by);
                progressView.changeMode(StatusProgressBar.ProgressStatus.STAND_BY);
            }
        } else {
            progressView.changeMode(StatusProgressBar.ProgressStatus.NONE);
            if (!enabled) {
                enableView.setImageResource(R.drawable.power_bt_disabled);
                textView.setText(R.string.disabled);
            } else {
                enableView.setImageResource(R.drawable.power_bt_paused);
                textView.setText(R.string.paused);
            }
        }
    }

}
