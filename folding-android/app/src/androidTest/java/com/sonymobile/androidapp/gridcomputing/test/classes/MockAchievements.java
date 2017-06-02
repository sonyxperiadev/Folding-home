/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test.classes;

import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.games.achievement.Achievement;

import java.util.List;

public class MockAchievements implements com.google.android.gms.games.achievement.Achievements {

    private List<Achievement> mList;

    public MockAchievements(final List<Achievement> list) {
        mList = list;
    }

    private MockAchievement getAchievementById(final String id) {
        for (Achievement curr : mList) {
            if (curr.getAchievementId().equals(id)) {
                return (MockAchievement) curr;
            }
        }
        return null;
    }

    @Override
    public Intent getAchievementsIntent(GoogleApiClient googleApiClient) {
        return null;
    }

    @Override
    public PendingResult<LoadAchievementsResult> load(GoogleApiClient googleApiClient, boolean b) {
        return null;
    }

    @Override
    public void reveal(GoogleApiClient googleApiClient, String s) {

    }

    @Override
    public PendingResult<UpdateAchievementResult> revealImmediate(
            GoogleApiClient googleApiClient, String s) {
        return null;
    }

    @Override
    public void unlock(GoogleApiClient googleApiClient, String s) {

    }

    @Override
    public PendingResult<UpdateAchievementResult> unlockImmediate(
            GoogleApiClient googleApiClient, String s) {
        return null;
    }

    @Override
    public void increment(GoogleApiClient googleApiClient, String s, int i) {
        final MockAchievement achievement = getAchievementById(s);
        achievement.setCurrentSteps(achievement.getCurrentSteps() + i);
    }

    @Override
    public PendingResult<UpdateAchievementResult> incrementImmediate(
            GoogleApiClient googleApiClient, String s, int i) {
        return null;
    }

    @Override
    public void setSteps(GoogleApiClient googleApiClient, String s, int i) {
        final MockAchievement achievement = getAchievementById(s);
        if (i > achievement.getCurrentSteps()) {
            achievement.setCurrentSteps(i);
        }
    }

    @Override
    public PendingResult<UpdateAchievementResult> setStepsImmediate(
            GoogleApiClient googleApiClient, String s, int i) {
        return null;
    }
}
