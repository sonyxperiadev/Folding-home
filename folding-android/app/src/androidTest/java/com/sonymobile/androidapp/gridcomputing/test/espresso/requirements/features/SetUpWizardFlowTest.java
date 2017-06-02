/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.espresso.requirements.features;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.WizardActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test a successful modification of the start time.
 */
@RunWith(AndroidJUnit4.class)
public class SetUpWizardFlowTest {

    @Rule
    public ActivityTestRule<WizardActivity> mActivityRule =
            new ActivityTestRule(WizardActivity.class);

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSetupWizardFlow() throws Exception {
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withText(R.string.private_date_safe_title)).check(matches(isDisplayed()));

        pressBack();
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));

        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withText(R.string.private_date_safe_title)).check(matches(isDisplayed()));

        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());

        onView(withId(R.id.finish_tv)).perform(click());

        onView(withId(R.id.summary_donated_time_tv)).check(matches(isDisplayed()));
    }

    @Test
    public void testAccessWizardFromSummary() throws Exception {
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
        onView(withId(R.id.wizard_pager)).perform(swipeLeft());
        onView(withText(R.string.private_date_safe_title)).check(matches(isDisplayed()));

        onView(withId(R.id.skip_tv)).perform(click());

        openActionBarOverflowOrOptionsMenu(mActivityRule.getActivity());
        onView(withText(R.string.setup_guide)).perform(click());
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
    }

    @Test
    public void testReadMore() throws Exception {
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_wizard_read_more)).perform(click());
        pressBack();

        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
        onView(withId(R.id.bt_wizard_read_more)).perform(click());
        onView(withText(R.string.more_info)).check(matches(isDisplayed()));

        onView(withId(R.id.bt_wizard_done)).perform(click());
        onView(withText(R.string.welcome)).check(matches(isDisplayed()));
    }

}
