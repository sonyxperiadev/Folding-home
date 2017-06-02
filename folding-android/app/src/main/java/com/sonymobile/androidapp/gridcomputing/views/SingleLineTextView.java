/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;

import android.content.Context;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Textview that adjusts the font size to fit the text in a single line.
 */
public class SingleLineTextView extends TextView {

    /**
     * Default constructor.
     *
     * @param context the context.
     */
    public SingleLineTextView(final Context context) {
        super(context);
        init();
    }

    /**
     * Default constructor.
     *
     * @param context the context.
     * @param attrs   the attribute set.
     */
    public SingleLineTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    /**
     * Default constructor.
     *
     * @param context      the context.
     * @param attrs        the attribute set.
     * @param defStyleAttr the default style.
     */
    public SingleLineTextView(final Context context, final AttributeSet attrs,
                              final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialization method.
     */
    private void init() {
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    protected final void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final Layout layout = getLayout();
        if (layout != null) {
            final int lineCount = layout.getLineCount();
            if (lineCount > 0) {
                final int ellipsisCount = layout.getEllipsisCount(lineCount - 1);
                if (ellipsisCount > 0) {

                    final float textSize = getTextSize();

                    // textSize is already expressed in pixels
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize - 1));

                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }
    }
}
