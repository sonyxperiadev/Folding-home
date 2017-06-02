/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.conditions.ConditionType;

/**
 * Fragment used to show the detailed condition information.
 */
public class ConditionsSlidePageFragment extends Fragment {

    /**
     * Extra key used to retrieve the condition from the fragment arguments.
     */
    private static final String CONDITION_EXTRA_KEY =
            "com.sonymobile.androidapp.gridcomputing.CONDITION_EXTRA_KEY";

    /**
     * The condition used to build this fragment.
     */
    private ConditionType mType;

    /**
     * The image of the condition.
     */
    private ImageView mImage;

    /**
     * The title of the condition.
     */
    private TextView mTitle;

    /**
     * The text of the condition.
     */
    private TextView mText;


    /**
     * Creates a fragment instance based on a condition.
     *
     * @param type the condition to create a fragment.
     * @return a new fragment instance.
     */
    public static ConditionsSlidePageFragment newInstance(final ConditionType type) {
        Bundle args = new Bundle();
        args.putSerializable(CONDITION_EXTRA_KEY, type);
        ConditionsSlidePageFragment fragment = new ConditionsSlidePageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.condition_detail_item, container,
                                                          false);
        mImage = (ImageView) rootView.findViewById(R.id.condition_detail_image);
        mTitle = (TextView) rootView.findViewById(R.id.condition_detail_title);
        mText = (TextView) rootView.findViewById(R.id.condition_detail_text);
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mType = (ConditionType) getArguments().getSerializable(CONDITION_EXTRA_KEY);

        if (mType != null) {
            switch (mType) {
                case CHARGER:
                    setItem(R.drawable.conditions_detail_charger,
                            R.string.no_charger_tile, R.string.no_charger_text);
                    break;
                case BATTERY:
                    setItem(R.drawable.conditions_detail_battery,
                            R.string.low_battery_tile, R.string.low_battery_text);
                    break;
                case WIFI:
                    setItem(R.drawable.conditions_detail_wifi,
                            R.string.no_wifi_tile, R.string.no_wifi_text);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Sets the content of this fragment.
     *
     * @param imageId the image resource id.
     * @param titleId the title resource id.
     * @param textId  the text resource id.
     */
    private void setItem(final int imageId, final int titleId, final int textId) {
        mImage.setImageResource(imageId);
        mTitle.setText(titleId);
        mText.setText(textId);
    }
}
