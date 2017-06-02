/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * Item decoration for achievements.
 */
public class AchievementItemDecoration extends RecyclerView.ItemDecoration {

    public AchievementItemDecoration() {
    }

    @Override
    public void getItemOffsets(final Rect outRect, final View view,
                               final RecyclerView parent, final RecyclerView.State state) {
        final int offset = ApplicationData.getAppContext().getResources()
                .getDimensionPixelSize(R.dimen.margin_small);
        outRect.left = offset;
        outRect.right = offset;
        outRect.bottom = offset;

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = offset;
        }
    }
}
