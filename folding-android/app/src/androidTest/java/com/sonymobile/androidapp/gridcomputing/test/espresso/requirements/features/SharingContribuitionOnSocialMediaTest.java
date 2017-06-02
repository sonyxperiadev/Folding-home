/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.espresso.requirements.features;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.activities.SummaryActivity;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Espresso test Test sharing contribution on Facebook and Google+
 */
@RunWith(AndroidJUnit4.class)
public class SharingContribuitionOnSocialMediaTest {

    private static final String REMOTE_SERVICE = "http://www.facebook.com";

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

    /**
     * Test Facebook share.
     *
     * @throws InterruptedException
     */

    @Test
    public void testSharingContribution() throws InterruptedException {
        if (!isRemoteServiceAvaliable()) {
            Log.d("SharingContributionTest", REMOTE_SERVICE + "not avaliable");
            return;
        }

        try {
            onView(withId(R.id.summary_share)).check(matches(isDisplayed()));
        } catch (Exception exception) {
            openActionBarOverflowOrOptionsMenu(mActivityRule.getActivity());
            onView(withText(R.string.share)).check(matches(isDisplayed()));
        }
        onView(withText(R.string.share)).perform(click());
        Thread.sleep(250);

        try {
            ApplicationInfo info = ApplicationData.getAppContext().getPackageManager()
                    .getApplicationInfo("com.facebook.katana", 0);
            Assert.assertNotNull(info);
            // TODO: Implementation using the facebook app
            pressBack();
        } catch (PackageManager.NameNotFoundException e) {
            onView(withText(R.string.facebook)).perform(click());
            try {
                // Wait 10 seconds for the facebook webview, else test will fail.
                waitFacebookWebviewLoading(10000);
                onWebView().withElement(findElement(Locator.NAME, "email"))
                    .perform(clearElement());
//              onWebView().withElement(findElement(Locator.NAME, "email"))
//                  .perform(DriverAtoms.webKeys("embeddedsony@gmail.com"));
                onWebView().withElement(findElement(Locator.NAME, "pass"))
                    .perform(clearElement());
            } catch (Exception ex) {
            }
        }
    }

    private boolean isRemoteServiceAvaliable() {
        int statusCode = -1;
        try {
            URL url = new URL(REMOTE_SERVICE);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            statusCode = urlConnection.getResponseCode();
            urlConnection.disconnect();
        } catch (Exception e) {
        }

        return statusCode == HttpURLConnection.HTTP_OK;
    }

    private void waitFacebookWebviewLoading(int maxWait) {
        final int RETRY_INTERVAL = 500; // millis.
        int loops = maxWait / RETRY_INTERVAL;
        for (int i = 0; i < loops; i++) {
            try {
                Thread.sleep(RETRY_INTERVAL);
                onWebView().withElement(findElement(Locator.NAME, "email"));
                break;
            } catch (Exception e) {
                continue;
            }
        }
    }
}
