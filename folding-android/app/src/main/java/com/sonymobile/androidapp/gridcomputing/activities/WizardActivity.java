/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.fragments.WizardMainFragment;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;

/**
 * Wizard activity that shows the necessary requirements to contribute in the
 * grid project.
 */
public class WizardActivity extends FragmentActivity {

    /**
     * Wizard Fragment Tag.
     */
    public static final String TAG_WIZARD_FRAGMENT = "MAIN_FRAGMENT";

    /**
     * Current visible fragment.
     */
    private Fragment mCurrentFragment;

    @Override
    protected final void onCreate(final Bundle savedState) {
        super.onCreate(savedState);
        Log.d("Activity > WizardActivity onCreate");

        setContentView(R.layout.activity_wizard);

        final WizardMainFragment fragment = new WizardMainFragment();
        final FragmentTransaction fTransaction = getSupportFragmentManager()
                .beginTransaction();
        fTransaction.replace(R.id.fragment_container, fragment,
                TAG_WIZARD_FRAGMENT);
        fTransaction.commit();

        mCurrentFragment = fragment;

    }

    @Override
    protected final void onPause() {
        super.onPause();
        Log.d("Activity > WizardActivity onPause");
    }

    @Override
    protected final void onResume() {
        super.onResume();
        Log.d("Activity > WizardActivity onResume");
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
        Log.d("Activity > WizardActivity onDestroy");
    }

    @Override
    public final void onBackPressed() {
        if (mCurrentFragment == null) {
            super.onBackPressed();
        } else if (mCurrentFragment.getTag().equalsIgnoreCase(
                TAG_WIZARD_FRAGMENT)
                && !((WizardMainFragment) mCurrentFragment).onBackPressed()) {
            super.onBackPressed();
        }
    }

    /**
     * Finishes wizard and start the project summary screen.
     */
    public final void finishWizard() {
        MiscPref.setWizardFinished();
        final Intent intent = new Intent(this, SummaryActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    /**
     * Method called when a view is clicked.
     *
     * @param view the view clicked.
     */
    public final void onClick(final View view) {
        if (view.getId() == R.id.bt_wizard_read_more) {
            showReadMoreDialog();
        } else if (view.getId() == R.id.sign_in_button) {
            startSummaryActivityWithSignIn();
        }
    }

    /**
     * Show read more dialog.
     */
    public final void showReadMoreDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_wizard_info);

        // Fullscreen
        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        dialog.setTitle(R.string.more_info);
        final String[] texts = getResources().getStringArray(
                R.array.help_find_cure);

        ((TextView) dialog.findViewById(R.id.more_info_tv1)).setText(texts[0]);
        ((TextView) dialog.findViewById(R.id.more_info_tv2)).setText(texts[1]);
        ((TextView) dialog.findViewById(R.id.more_info_tv3)).setText(texts[2]);

        dialog.findViewById(R.id.bt_wizard_done).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    /**
     * Start Summary Activity with extra sign in.
     */
    private void startSummaryActivityWithSignIn() {
        MiscPref.setWizardFinished();
        final Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra("login_ggs", true);
        startActivity(intent);
        finishAffinity();
    }
}
