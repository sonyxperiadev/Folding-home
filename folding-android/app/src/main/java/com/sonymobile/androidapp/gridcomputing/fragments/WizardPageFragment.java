/*
android:layout_width="@dimen/wizard_button_width"
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.utils.ViewUtils;

/**
 * Fragment class to handle all the wizard configuration flow.
 */
public final class WizardPageFragment extends Fragment {

    /**
     * Shared preferences key to current wizard page position.
     */
    public static final String PAGE_POSITION = "PAGE_POSITION";

    /**
     * Current layoutId.
     */
    private int mLayoutId;

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        final int currentPosition = getArguments().getInt(PAGE_POSITION);
        final int newPosition = WizardMainFragment.getDirectionPageIndex(currentPosition);

        switch (newPosition) {
            case 0:
                mLayoutId = R.layout.fragment_wizard_welcome;
                break;
            case 1:
                mLayoutId = R.layout.fragment_wizard_settings;
                break;
            case 2:
                mLayoutId = R.layout.fragment_wizard_whats_new_status_indicators;
                break;
            case 3:
                mLayoutId = R.layout.fragment_wizard_whats_new_power_button;
                break;
            case 4:
                mLayoutId = R.layout.fragment_wizard_whats_new_login;
                break;
            default:
                mLayoutId = R.layout.fragment_wizard_welcome;
                break;
        }

        return inflater.inflate(mLayoutId, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        setupView(mLayoutId);
    }

    /**
     * Setup view for the selected page.
     *
     * @param layoutId layout id.
     */
    private void setupView(final int layoutId) {
        if (layoutId == R.layout.fragment_wizard_whats_new_power_button) {
            ViewUtils.updateStatusBar(getView().findViewById(R.id.status_bar_paused), true, true,
                    false, false, false);
            ViewUtils.updateStatusBar(getView().findViewById(R.id.status_bar_stand_by), true, false,
                    false, true, true);
            ViewUtils.updateStatusBar(getView().findViewById(R.id.status_bar_contributing), true,
                    false, true, true, true);

            getView().findViewById(R.id.status_bar_paused)
                    .findViewById(R.id.summary_menu_power_toggle).setClickable(false);
            getView().findViewById(R.id.status_bar_stand_by)
                    .findViewById(R.id.summary_menu_power_toggle).setClickable(false);
            getView().findViewById(R.id.status_bar_contributing)
                    .findViewById(R.id.summary_menu_power_toggle).setClickable(false);

        }
    }
}
