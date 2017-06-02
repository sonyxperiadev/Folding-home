/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
package com.sonymobile.androidapp.gridcomputing.activities;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.Achievements;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.adapters.AchievementsAdapter;
import com.sonymobile.androidapp.gridcomputing.fragments.AchievementDialogFragment;
import com.sonymobile.androidapp.gridcomputing.gamification.GameHelper;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.views.AchievementItemDecoration;

import java.util.ArrayList;
import java.util.List;


/**
 * Achievements.
 */
public class AchievementsActivity extends SocialActivity
        implements AchievementsAdapter.OnItemClickListener {

    /**
     * Recycler view.
     */
    private RecyclerView mRecyclerView;
    /**
     * Achievements adapter.
     */
    private AchievementsAdapter mAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Activity > AchievementsActivity onCreate");

        setContentView(R.layout.progress_bar);
        loadAchievements();
    }

    /**
     * Sets UI.
     */
    private void setUI() {
        setContentView(R.layout.activity_achievements);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.achievements_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new AchievementItemDecoration());

        mAdapter = new AchievementsAdapter(this);
    }

    /**
     * Load achievements from Google Games.
     */
    private void loadAchievements() {
        Log.d("Loading achievements");
        Games.Achievements.load(GameHelper.getApiClient(), true)
                .setResultCallback(
                        new ResultCallback<Achievements.LoadAchievementsResult>() {
                            @Override
                            public void onResult(
                                    final Achievements.LoadAchievementsResult
                                            loadAchievementsResult) {
                                android.util.Log.d("printAchievements", "onResult");

                                setUI();

                                List<Achievement> achievementList = new ArrayList<Achievement>();
                                int unlockedCount = 0;
                                for (Achievement achievement : loadAchievementsResult
                                        .getAchievements()) {
                                    if (achievement.getState() == Achievement.STATE_UNLOCKED) {
                                        unlockedCount++;
                                    }
                                    achievementList.add(achievement);
                                    android.util.Log.d("printAchievements",
                                            "Achievement: " + achievement.getDescription()
                                                    + " getState: "
                                                    + achievement.getState()
                                                    + ", getRevealedImageUrl: "
                                                    + achievement.getRevealedImageUrl()
                                                    + ", getUnlockedImageUrl: "
                                                    + achievement.getUnlockedImageUrl()
                                                    + ", getRevealedImageUri: "
                                                    + achievement.getRevealedImageUri()
                                                    + ", getUnlockedImageUri: "
                                                    + achievement.getUnlockedImageUri()
                                                    + ", getType: " + achievement.getType()
                                    );
                                }
                                mAdapter.setAchievements(achievementList);
                                mRecyclerView.setAdapter(mAdapter);
                                updateAchievementProgress(unlockedCount, achievementList.size());
                            }
                        });
    }

    /**
     * Update total achievements progress bar.
     * @param unlockedCount number of unlocked achievements.
     * @param total total achievements count.
     */
    private void updateAchievementProgress(final int unlockedCount, final int total) {
        final ProgressBar progressBar =
                ((ProgressBar) findViewById(R.id.achievements_total_progress_bar));
        progressBar.setMax(total);
        progressBar.setProgress(unlockedCount);

        final TextView textView =
                ((TextView) findViewById(R.id.achievement_text_counter));

        String achievementCounterStr = unlockedCount + "/" + total;
        textView.setText(achievementCounterStr);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onItemClicked(final View view, final Achievement achievement) {
        final AchievementDialogFragment dialogFragment =
                AchievementDialogFragment.newInstance(achievement);
        final android.app.FragmentManager fm = getFragmentManager();
        dialogFragment.show(fm, AchievementDialogFragment.TAG);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
