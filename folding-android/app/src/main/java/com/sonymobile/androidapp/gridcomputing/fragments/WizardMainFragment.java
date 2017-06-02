/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.WizardActivity;
import com.sonymobile.androidapp.gridcomputing.adapters.WizardPageAdapter;

import java.util.Locale;

/**
 * Wizard activity that shows the necessary requirements to contribute in the
 * grid project.
 */
public class WizardMainFragment extends Fragment implements
        OnPageChangeListener, OnClickListener {

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    public static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    /**
     * Verifies if current displayed language is Right-to-left.
     *
     * @return true if current displayed language is Right-to-left, false
     * otherwise.
     */
    public static boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Get the direction of the page transition based on RTL or LTR languages.
     *
     * @param page number of the displayed page.
     * @return next page index.
     */
    public static int getDirectionPageIndex(final int page) {
        int nextPage = page;
        if (isRtl()) {
            nextPage = (NUM_PAGES - 1) - page;
        }
        return nextPage;
    }

    @Override
    public final void onActivityCreated(final Bundle savedInstance) {
        super.onActivityCreated(savedInstance);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) getActivity().findViewById(R.id.wizard_pager);
        mPager.addOnPageChangeListener(this);

        if (mPagerAdapter == null) {
            mPagerAdapter = new WizardPageAdapter(getFragmentManager());
            mPager.setAdapter(mPagerAdapter);
        }
        setRetainInstance(true);

        changeStep(0);

        changePage(getDirectionPageIndex(0));

        getActivity().findViewById(R.id.finish_tv).setOnClickListener(
                this);
        getActivity().findViewById(R.id.skip_tv).setOnClickListener(
                this);

    }

    @Override
    public final View onCreateView(final LayoutInflater inflater,
                                   final ViewGroup container, final Bundle savedInstanceState) {

        final View rootView = inflater
                .inflate(R.layout.fragment_main_wizard, container, false);

        LinearLayout pagerIndicatorContainer = (LinearLayout) rootView.findViewById(
                R.id.wizard_page_indicator_container);

        for (int i = 0; i < NUM_PAGES; i++) {
            View.inflate(getActivity(), R.layout.view_pager_indicator, pagerIndicatorContainer);
        }

        return rootView;
    }

    /**
     * On back button pressed within this fragment. Returns one page.
     *
     * @return true if there's a page to return. False if it's first page.
     */
    public final boolean onBackPressed() {
        boolean result;
        if (getPage() == 0) {
            result = false;
        } else {
            // Otherwise, select the previous step.
            stepPage(-1);
            result = true;
        }
        return result;
    }

    /**
     * Action to capture clicking events in these activity views.
     *
     * @param view the source of the clicking event.
     */
    @Override
    public final void onClick(final View view) {
        switch (view.getId()) {
            case R.id.finish_tv:
            case R.id.skip_tv:
                ((WizardActivity) getActivity()).finishWizard();
                break;
            default:
                break;
        }
    }

    /**
     * change to the next or previous page.
     *
     * @param direction direction of the next or previous page.
     */
    private void stepPage(final int direction) {
        final int currentPage = mPager.getCurrentItem();
        if (direction > 0) {
            changePage(currentPage + 1);
        } else if (direction < 0) {
            if (isRtl()) {
                changePage(currentPage + 1);
            } else {
                changePage(currentPage - 1);
            }
        }

    }

    /**
     * change current page.
     *
     * @param page next page to be displayed.
     */
    private void changePage(final int page) {
        mPager.setCurrentItem(page);
    }

    /**
     * Get current page.
     *
     * @return current page.
     */
    private int getPage() {
        final int ltrPage = mPager.getCurrentItem();
        if (isRtl()) {
            return NUM_PAGES - 1 - ltrPage;
        }
        return ltrPage;
    }

    /**
     * Change the wizard step.
     *
     * @param page the requested page.
     */
    private void changeStep(final int page) {
        int nextPage = getDirectionPageIndex(page);

        if (nextPage == NUM_PAGES - 1) {
            getActivity().findViewById(R.id.finish_tv).setVisibility(
                    View.VISIBLE);
            getActivity().findViewById(R.id.skip_tv).setVisibility(
                    View.INVISIBLE);
        } else {
            getActivity().findViewById(R.id.finish_tv).setVisibility(
                    View.INVISIBLE);
            getActivity().findViewById(R.id.skip_tv).setVisibility(
                    View.VISIBLE);
        }
        updatePageIndicator(nextPage);
    }

    /**
     * Update page indicator to match current page.
     *
     * @param page current page.
     */
    private void updatePageIndicator(final int page) {
        // update page indicator.
        LinearLayout container = (LinearLayout) getActivity().findViewById(
                R.id.wizard_page_indicator_container);

        for (int i = 0; i < NUM_PAGES; i++) {
            final View view = container.getChildAt(i);
            view.setSelected(false);
            if (i == page) {
                view.setSelected(true);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(final int arg0) {
        // Not used
    }

    @Override
    public void onPageScrolled(final int arg0, final float arg1, final int arg2) {
        // Not used
    }

    @Override
    public final void onPageSelected(final int pageNumber) {
        changeStep(pageNumber);
    }
}
