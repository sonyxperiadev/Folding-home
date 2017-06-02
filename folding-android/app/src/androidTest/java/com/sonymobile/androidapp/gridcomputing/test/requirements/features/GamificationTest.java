/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
package com.sonymobile.androidapp.gridcomputing.test.requirements.features;

import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.games.achievement.Achievement;
import com.sonymobile.androidapp.gridcomputing.gamification.Scores;
import com.sonymobile.androidapp.gridcomputing.test.classes.MockAchievement;
import com.sonymobile.androidapp.gridcomputing.test.classes.MockAchievements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by italo on 28/10/15.
 */
@RunWith(AndroidJUnit4.class)
public class GamificationTest {

    private final long STEP_INCREMENT_ACHIEVEMENT = TimeUnit.MINUTES.toMillis(10);
    private final List<Achievement> mAchievementList =  new ArrayList<>();
    private MockAchievements mAchievements;

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");

        MockAchievement aminoacid = new MockAchievement("3 hours", 18);
        MockAchievement peptide = new MockAchievement("9 hours", 54);
        MockAchievement polypeptide = new MockAchievement("27 hours", 162);
        MockAchievement protein = new MockAchievement("81 hours", 486);
        MockAchievement enzyme = new MockAchievement("243 hours", 1458);

        mAchievementList.add(aminoacid);
        mAchievementList.add(peptide);
        mAchievementList.add(polypeptide);
        mAchievementList.add(protein);
        mAchievementList.add(enzyme);

        mAchievements = new MockAchievements(mAchievementList);
    }

    @After
    public void after() {
        resetSteps();
    }

    private void resetSteps() {
        for (Achievement achievement : mAchievementList) {
            ((MockAchievement) achievement).setCurrentSteps(0);
        }
    }

    private void unlockAchievement(final int hours) {
        final long score = TimeUnit.HOURS.toMillis(hours);
        Scores.unlockIncrementalAchievements(null, score, mAchievements, mAchievementList);
    }

    @Test
    public void testFirstCase() {
        unlockAchievement(12);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(), 49);
        assertEquals(mAchievementList.get(3).getCurrentSteps(), 48);
        assertEquals(mAchievementList.get(4).getCurrentSteps(), 48);
    }

    @Test
    public void testSecondCase() {
        unlockAchievement(120);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(),
                     mAchievementList.get(2).getTotalSteps());
        assertEquals(mAchievementList.get(3).getCurrentSteps(),
                     mAchievementList.get(3).getTotalSteps());
        assertEquals(mAchievementList.get(4).getCurrentSteps(), 481);
    }

    @Test
    public void testThirdCase() {
        unlockAchievement(12);
        unlockAchievement(5);
        unlockAchievement(2);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(), 49);
        assertEquals(mAchievementList.get(3).getCurrentSteps(), 48);
        assertEquals(mAchievementList.get(4).getCurrentSteps(), 48);
    }

    @Test
    public void testFourthCase() {
        unlockAchievement(5);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertNotSame(mAchievementList.get(1).getCurrentSteps(),
                      mAchievementList.get(1).getTotalSteps());
        assertNotSame(mAchievementList.get(2).getCurrentSteps(),
                      mAchievementList.get(2).getTotalSteps());
        assertNotSame(mAchievementList.get(3).getCurrentSteps(),
                      mAchievementList.get(3).getTotalSteps());
        assertNotSame(mAchievementList.get(4).getCurrentSteps(),
                      mAchievementList.get(4).getTotalSteps());
    }

    @Test
    public void testFifthCase() {
        unlockAchievement(15);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertNotSame(mAchievementList.get(2).getCurrentSteps(),
                      mAchievementList.get(2).getTotalSteps());
        assertNotSame(mAchievementList.get(3).getCurrentSteps(),
                      mAchievementList.get(3).getTotalSteps());
        assertNotSame(mAchievementList.get(4).getCurrentSteps(),
                      mAchievementList.get(4).getTotalSteps());
    }

    @Test
    public void testSixthCase() {
        unlockAchievement(45);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(),
                     mAchievementList.get(2).getTotalSteps());
        assertNotSame(mAchievementList.get(3).getCurrentSteps(),
                      mAchievementList.get(3).getTotalSteps());
        assertNotSame(mAchievementList.get(4).getCurrentSteps(),
                      mAchievementList.get(4).getTotalSteps());
    }

    @Test
    public void testSeventhCase() {
        unlockAchievement(130);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(),
                     mAchievementList.get(2).getTotalSteps());
        assertEquals(mAchievementList.get(3).getCurrentSteps(),
                     mAchievementList.get(3).getTotalSteps());
        assertNotSame(mAchievementList.get(4).getCurrentSteps(),
                      mAchievementList.get(4).getTotalSteps());
    }

    @Test
    public void testEighthCase() {
        unlockAchievement(400);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(),
                     mAchievementList.get(1).getTotalSteps());
        assertEquals(mAchievementList.get(2).getCurrentSteps(),
                     mAchievementList.get(2).getTotalSteps());
        assertEquals(mAchievementList.get(3).getCurrentSteps(),
                     mAchievementList.get(3).getTotalSteps());
        assertEquals(mAchievementList.get(4).getCurrentSteps(),
                     mAchievementList.get(4).getTotalSteps());
    }

    @Test
    public void testNinthCase() {
        unlockAchievement(3);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(), 13);
        assertEquals(mAchievementList.get(2).getCurrentSteps(), 12);
        assertEquals(mAchievementList.get(3).getCurrentSteps(), 12);
        assertEquals(mAchievementList.get(4).getCurrentSteps(), 12);

        unlockAchievement(3);
        assertEquals(mAchievementList.get(0).getCurrentSteps(),
                     mAchievementList.get(0).getTotalSteps());
        assertEquals(mAchievementList.get(1).getCurrentSteps(), 13);
        assertEquals(mAchievementList.get(2).getCurrentSteps(), 12);
        assertEquals(mAchievementList.get(3).getCurrentSteps(), 12);
        assertEquals(mAchievementList.get(4).getCurrentSteps(), 12);
    }
}
