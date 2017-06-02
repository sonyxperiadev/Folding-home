/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.espresso.requirements.features;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.SummaryActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LegalPresentationTest {

    @Rule
    public ActivityTestRule<SummaryActivity> mActivityRule =
            new ActivityTestRule(SummaryActivity.class);

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLegalPresentation() {
        openActionBarOverflowOrOptionsMenu(mActivityRule.getActivity());
        onView(withText(R.string.legal)).perform(click());

        onView(withText(R.string.legal)).inRoot(isDialog()).check(matches(isDisplayed()));

        pressBack();
    }
}
