/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * StatusBar that changes indeterminate drawable according with each status.
 */
public class StatusProgressBar extends ProgressBar {

    /**
     * Current progress status.
     */
    private ProgressStatus mCurrentStatus = ProgressStatus.NONE;

    /**
     * View default constructor.
     *
     * @param context application context.
     */
    public StatusProgressBar(final Context context) {
        super(context);
    }

    /**
     * View default constructor.
     *
     * @param context application context.
     * @param attrs   view attributes.
     */
    public StatusProgressBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * View default constructor.
     *
     * @param context      application context.
     * @param attrs        view attributes.
     * @param defStyleAttr style attributes.
     */
    public StatusProgressBar(final Context context,
                             final AttributeSet attrs,
                             final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Progress bar status.
     */
    public enum ProgressStatus {
        RUNNING,
        STAND_BY,
        NONE
    }


    /**
     * Transition through states of progress bar.
     *
     * @param mode Mode to transition to.
     */
    public void changeMode(final ProgressStatus mode) {
        if (mCurrentStatus != mode && !ApplicationData.isJUnit()) {
            mCurrentStatus = mode;
            final Drawable drawable;
            final Animation anim;
            setVisibility(View.VISIBLE);
            clearAnimation();
            switch (mode) {
                case RUNNING:
                    drawable =
                            ContextCompat.getDrawable(getContext(), R.drawable.img_processing);
                    anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_spinner_anim);
                    startAnimation(anim);
                    break;
                case STAND_BY:
                    drawable =
                            ContextCompat.getDrawable(getContext(), R.drawable.ic_proc_pause);
                    anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_anim);
                    startAnimation(anim);
                    break;
                case NONE:
                    drawable = null;
                    setVisibility(View.INVISIBLE);
                    break;
                default:
                    drawable = null;
                    setVisibility(View.INVISIBLE);
                    break;
            }
            setIndeterminateDrawable(drawable);
            requestLayout();
        }
    }
}
