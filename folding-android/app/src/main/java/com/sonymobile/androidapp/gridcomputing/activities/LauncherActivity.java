/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.activities;

import android.app.Activity;
import android.content.Intent;

import com.sonymobile.androidapp.gridcomputing.assets.CopyAssets;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;

/**
 * Shows terms of use disclaimer when starting the application for the first
 * time.
 */
public class LauncherActivity extends Activity {

    @Override
    protected final void onResume() {
        super.onResume();
        wizardCheck();
    }

    /**
     * Checks if user went through wizard and loads it if necessary.
     */
    private void wizardCheck() {
        if (!MiscPref.checkAndSetLatestVersion()) {
            CopyAssets.deleteFiles();
        }

        final Intent intent;
        if (MiscPref.getWizardFinished()) {
            intent = new Intent(LauncherActivity.this,
                    SummaryActivity.class);
        } else {
            intent = new Intent(LauncherActivity.this,
                    WizardActivity.class);
        }
        startActivity(intent);
        finish();

    }

}
