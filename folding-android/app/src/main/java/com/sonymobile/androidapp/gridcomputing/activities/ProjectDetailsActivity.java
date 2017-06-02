/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.fragments.WebviewFragment;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;

/**
 * Activity which shows details of the current project Folding@Home is contributing to.
 * Project description opens in a WebView.
 */
public class ProjectDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.project_description);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final String url = RunningPref.getResearchUrl();
        final Fragment newFragment = WebviewFragment.newInstance(url);
        getSupportFragmentManager().beginTransaction().add(R.id.project_details_content,
                                                           newFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                // ignore
                break;
        }
        return true;
    }
}
