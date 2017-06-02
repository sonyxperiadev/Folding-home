/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.games.achievement.Achievement;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.views.AchievementViewHolder;

import java.util.ArrayList;
import java.util.List;


/**
 * Achievements adapter class.
 */
public class AchievementsAdapter extends RecyclerView.Adapter<AchievementViewHolder> {

    /**
     * List of achievements.
     */
    private List<Achievement> mAchievementsList = new ArrayList<Achievement>();

    /**
     * Listener.
     */
    private OnItemClickListener mListener;

    /**
     * The class constructor.
     * @param listener the listener
     */
    public AchievementsAdapter(final OnItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public AchievementViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.achievement_list_item, parent, false);
        return new AchievementViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(final AchievementViewHolder holder, final int position) {
        Achievement achievement = mAchievementsList.get(position);
        holder.bindAchievement(achievement);
    }

    @Override
    public int getItemCount() {
        return mAchievementsList.size();
    }

    public void setAchievements(final List<Achievement> achievementsList) {
        mAchievementsList = achievementsList;
    }

    public interface OnItemClickListener {
        void onItemClicked(View v, Achievement achievement);
    }
}

