/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.espresso.requirements.features;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.SummaryActivity;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.utils.FormatUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PresentationContributedEffortTest {

    private static final String RESEARCH = "CANCER";
    private static final int USERS = 100;

    @Rule
    public ActivityTestRule<SummaryActivity> mActivityRule =
            new ActivityTestRule(SummaryActivity.class);

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");

        RunningPref.setNumberOfUsers(USERS);
        RunningPref.setResearchType(RESEARCH);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testContribution() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityRule.getActivity().loadJobsStats(RunningPref.getNumberOfUsers(),
                        RunningPref.getResearchType(),
                        RunningPref.getAccumulatedTime());
            }
        });


        onView(withId(R.id.summary_donated_time_tv)).check(matches(isDisplayed()));
        onView(withText(FormatUtils.getMainTimeString(0)))
            .check(matches(withId(R.id.summary_donated_time_tv)));

        onView(withId(R.id.summary_num_people_helping)).check(matches(isDisplayed()));
        onView(withText(Integer.toString(USERS)))
            .check(matches(withId(R.id.summary_num_people_helping)));

        onView(withId(R.id.summary_research_type)).check(matches(isDisplayed()));
        onView(withText(RESEARCH)).check(matches(withId(R.id.summary_research_type)));
    }

}
