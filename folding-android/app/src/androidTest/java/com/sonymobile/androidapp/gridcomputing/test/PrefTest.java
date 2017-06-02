/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test;

import android.content.SharedPreferences;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.sonymobile.androidapp.gridcomputing.preferences.GamePref;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.preferences.PrefUtils;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.service.ServiceManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class PrefTest {

    private Map<String, ?> mGameMap;
    private Map<String, ?> mRunningSet;
    private Map<String, ?> mMiscSet;
    private Map<String, ?> mSettingsSet;

    public static void copyMapToSharedPreferences(Map<String, ?> map,
                                                  SharedPreferences toPreferences) {
        SharedPreferences.Editor toEditor = toPreferences.edit();
        toEditor.clear();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (value instanceof String) {
                toEditor.putString(key, ((String) value));
            } else if (value instanceof Set) {
                toEditor.putStringSet(key, (Set<String>) value);
                // EditorImpl.putStringSet already creates a copy of the set
            } else if (value instanceof Integer) {
                toEditor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                toEditor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                toEditor.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                toEditor.putBoolean(key, (Boolean) value);
            }
        }
        toEditor.apply();
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
        mGameMap = PrefUtils.getSharedPreferences(GamePref.PREF_FILE).getAll();
        mRunningSet = PrefUtils.getSharedPreferences(RunningPref.PREF_FILE).getAll();
        mMiscSet = PrefUtils.getSharedPreferences(MiscPref.PREF_FILE).getAll();
        mSettingsSet = PrefUtils.getSharedPreferences(SettingsPref.PREF_FILE).getAll();
    }

    @After
    public void tearDown() throws Exception {
        copyMapToSharedPreferences(mGameMap,
                                   PrefUtils.getSharedPreferences(GamePref.PREF_FILE));
        copyMapToSharedPreferences(mRunningSet,
                                   PrefUtils.getSharedPreferences(RunningPref.PREF_FILE));
        copyMapToSharedPreferences(mMiscSet,
                                   PrefUtils.getSharedPreferences(MiscPref.PREF_FILE));
        copyMapToSharedPreferences(mSettingsSet,
                                   PrefUtils.getSharedPreferences(SettingsPref.PREF_FILE));
    }

    @Test
    public void testSettingsPref() throws InterruptedException {
        SettingsPref.setExecutionEnabled(false);
        Assert.assertFalse(SettingsPref.isExecutionEnabled());

        SettingsPref.setExecutionEnabled(true);
        Assert.assertTrue(SettingsPref.isExecutionEnabled());
    }

    @Test
    public void testGamePref() throws InterruptedException {
        Assert.assertTrue(GamePref.getSignedOutExplicitly());
        GamePref.setSignedOutExplicitly(false);
        Assert.assertFalse(GamePref.getSignedOutExplicitly());
        GamePref.setSignedOutExplicitly(true);
        Assert.assertTrue(GamePref.getSignedOutExplicitly());

        GamePref.resetScore();
        Assert.assertEquals(0L, GamePref.getScoreToSubmit());

        GamePref.incrementScoreToSubmit(1000);
        Assert.assertEquals(1000L, GamePref.getScoreToSubmit());

        GamePref.incrementScoreToSubmit(1000);
        Assert.assertEquals(2000L, GamePref.getScoreToSubmit());

        GamePref.incrementScoreToSubmit(1000);
        Assert.assertEquals(3000L, GamePref.getScoreToSubmit());

        GamePref.resetScore();
        Assert.assertEquals(0L, GamePref.getScoreToSubmit());
    }

    @Test
    public void testRunningPref() throws InterruptedException {
        RunningPref.setResearchUrl("researchUrl");
        Assert.assertEquals("researchUrl", RunningPref.getResearchUrl());

        RunningPref.setResearchId("researchId");
        Assert.assertEquals("researchId", RunningPref.getResearchId());

        RunningPref.setResearchType("researchType");
        Assert.assertEquals("researchType", RunningPref.getResearchType());

        PrefUtils.getSharedPreferences(RunningPref.PREF_FILE)
                .edit().remove(RunningPref.RESEARCH_TYPE).commit();
        Assert.assertTrue(TextUtils.isEmpty(RunningPref.getResearchType()));
        ServiceManager.startComputeService();
        Thread.sleep(200);
        Assert.assertTrue(TextUtils.isEmpty(RunningPref.getResearchType()));

        RunningPref.setNumberOfUsers(100);
        Assert.assertEquals(100, RunningPref.getNumberOfUsers());

        RunningPref.setAccumulatedTime(0L);
        Assert.assertEquals(0, RunningPref.getAccumulatedTime());

        RunningPref.incrementAccumulatedTime(1000);
        Assert.assertEquals(1000, RunningPref.getAccumulatedTime());

        RunningPref.incrementAccumulatedTime(1000);
        Assert.assertEquals(2000, RunningPref.getAccumulatedTime());
    }

    @Test
    public void testMiscPref() throws InterruptedException {
        Assert.assertFalse(MiscPref.getWizardFinished());
        MiscPref.setWizardFinished();
        Assert.assertTrue(MiscPref.getWizardFinished());

        MiscPref.setDisabledApp(true);
        Assert.assertTrue(MiscPref.getDisabledApp());
        MiscPref.setDisabledApp(false);
        Assert.assertFalse(MiscPref.getDisabledApp());

        final String uuid = MiscPref.getUUID();
        Assert.assertNotNull(uuid);
        Assert.assertEquals(uuid, MiscPref.getUUID());

        Assert.assertFalse(MiscPref.checkAndSetLatestVersion());
        Assert.assertTrue(MiscPref.checkAndSetLatestVersion());
    }
}
