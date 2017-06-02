/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.sonymobile.androidapp.gridcomputing.conditions.ConditionType;
import com.sonymobile.androidapp.gridcomputing.fragments.ConditionsSlidePageFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Page adapter used to show the not met conditions.
 */
public class ConditionsSlidePagerAdapter extends FragmentStatePagerAdapter {

    /**
     * Not met conditions list.
     */
    private List<ConditionType> mConditionsList;

    public ConditionsSlidePagerAdapter(final FragmentManager fm) {
        super(fm);
        mConditionsList = new ArrayList<>();
    }

    /**
     * Updates the not met condition list.
     *
     * @param conditionsList the new not met list.
     */
    public void setConditionsNotMetList(final List<ConditionType> conditionsList) {
        mConditionsList = conditionsList;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(final int position) {
        return ConditionsSlidePageFragment.newInstance(mConditionsList.get(position));
    }

    @Override
    public int getCount() {
        return mConditionsList.size();
    }

    @Override
    public int getItemPosition(final Object object) {
        return PagerAdapter.POSITION_NONE;
    }
}
