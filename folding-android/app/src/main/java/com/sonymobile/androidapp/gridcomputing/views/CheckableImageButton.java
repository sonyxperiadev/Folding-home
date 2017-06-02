/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

/**
 * Class to use an ImageButton with checked state.
 */
public class CheckableImageButton extends ImageButton implements Checkable {

    /**
     * Checked drawable state.
     */
    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    /**
     * Indicates if the button is checked.
     */
    private boolean mChecked;

    /**
     * Default constructor.
     *
     * @param context the context.
     */
    public CheckableImageButton(final Context context) {
        super(context);
    }

    /**
     * Default constructor.
     *
     * @param context the context.
     * @param attrs   the attribute set.
     */
    public CheckableImageButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Default constructor.
     *
     * @param context      the context.
     * @param attrs        the attribute set.
     * @param defStyleAttr the default style.
     */
    public CheckableImageButton(final Context context, final AttributeSet attrs,
                                final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public final boolean isChecked() {
        return mChecked;
    }

    @Override
    public final void setChecked(final boolean checked) {
        mChecked = checked;
        refreshDrawableState();
    }

    @Override
    public final void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public final boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override
    public final int[] onCreateDrawableState(final int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

}
