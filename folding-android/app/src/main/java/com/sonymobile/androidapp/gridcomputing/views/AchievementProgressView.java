/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

/**
 * Class responsible for displaying the progress bar of achievements.
 */
public class AchievementProgressView extends TextView {

    /**
     * Sweep angle.
     */
    private static final float SWEEP_ANGLE = 360.0F;
    /**
     * Start angle.
     */
    private static final float START_ANGLE = 270.0F;
    /**
     * One hundred percent.
     */
    private static final int ONE_HUNDRED_PERCENT = 100;
    /**
     * Current steps.
     */
    private int mCurrentSteps;
    /**
     * Drawing Bounds.
     */
    private final Rect mDrawingBounds = new Rect();
    /**
     * Drawing Bounds.
     */
    private final RectF mDrawingBoundsF = new RectF();
    /**
     * Internal color of the arc.
     */
    private int mInnerArcColor;
    /**
     * Internal paint of the arc.
     */
    private Paint mInnerArcPaint;
    /**
     * Width of internal arc.
     */
    private float mInnerArcStrokeWidth;
    /**
     * Color outside the arc.
     */
    private int mOuterArcColor;
    /**
     * Paint outside the arc.
     */
    private Paint mOuterArcPaint;
    /**
     * width of the outer arc.
     */
    private float mOuterArcStrokeWidth;
    /**
     * Total steps.
     */
    private int mTotalSteps;

    /**
     * The class constructor.
     *
     * @param paramContext the context
     */
    public AchievementProgressView(final Context paramContext) {
        super(paramContext);
        initialize();
    }

    /**
     * The class constructor.
     *
     * @param paramContext      the context
     * @param paramAttributeSet the attribute set
     */
    public AchievementProgressView(final Context paramContext,
                                   final AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        initialize();
    }

    /**
     * The class constructor.
     *
     * @param paramContext      the context.
     * @param paramAttributeSet the attribute set.
     * @param paramInt          the int value.
     */
    public AchievementProgressView(final Context paramContext, final AttributeSet paramAttributeSet,
                                   final int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        initialize();
    }

    /**
     * Method to initialize.
     */
    private void initialize() {
        final Context context = ApplicationData.getAppContext();
        this.mOuterArcStrokeWidth = getResources().getDimension(R.dimen.achievement_progress_outer);
        this.mInnerArcStrokeWidth = getResources().getDimension(R.dimen.achievement_progress_inner);
        this.mOuterArcColor = ContextCompat.getColor(context, R.color.sony_gray_text);
        this.mInnerArcColor = ContextCompat.getColor(context, R.color.green);
        this.mOuterArcPaint = new Paint();
        this.mOuterArcPaint.setAntiAlias(true);
        this.mOuterArcPaint.setColor(this.mOuterArcColor);
        this.mOuterArcPaint.setStrokeWidth(this.mOuterArcStrokeWidth);
        this.mOuterArcPaint.setStyle(Paint.Style.STROKE);
        this.mInnerArcPaint = new Paint(this.mOuterArcPaint);
        this.mInnerArcPaint.setStrokeWidth(this.mInnerArcStrokeWidth);
        this.mInnerArcPaint.setColor(this.mInnerArcColor);
    }

    /**
     * Gets percentage text.
     *
     * @param paramInt1 the current steps
     * @param paramInt2 the total steps
     *
     * @return the string.
     */
    public final String getPercentageText(final int paramInt1, final int paramInt2) {
        this.mCurrentSteps = paramInt1;
        this.mTotalSteps = paramInt2;
        int percentage = this.mCurrentSteps * ONE_HUNDRED_PERCENT / this.mTotalSteps;
        return getResources().getString(R.string.pause, new Object[]{Integer.valueOf(percentage)});
    }

    /**
     * Method that draws the progress bar.
     *
     * @param paramCanvas the canvas
     */
    public void onDraw(final Canvas paramCanvas) {
        getDrawingRect(this.mDrawingBounds);
        this.mDrawingBoundsF.set(this.mDrawingBounds);
        float f = this.mOuterArcStrokeWidth / 2.0F;
        this.mDrawingBoundsF.inset(f, f);
        paramCanvas.drawArc(this.mDrawingBoundsF, 0.0F, SWEEP_ANGLE, false, this.mOuterArcPaint);
        f = this.mOuterArcStrokeWidth / 2.0F + this.mInnerArcStrokeWidth / 2.0F;
        this.mDrawingBoundsF.inset(f, f);
        f = this.mCurrentSteps * SWEEP_ANGLE / this.mTotalSteps;
        paramCanvas.drawArc(this.mDrawingBoundsF, START_ANGLE, f, false, this.mInnerArcPaint);
        super.onDraw(paramCanvas);
    }

    /**
     * Sets steps.
     *
     * @param paramInt1 the current steps
     * @param paramInt2 the total steps
     */
    public final void setSteps(final int paramInt1, final int paramInt2) {
        setText(getPercentageText(paramInt1, paramInt2));
    }
}
