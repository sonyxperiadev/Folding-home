/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.views;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.games.achievement.Achievement;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.adapters.AchievementsAdapter;
import com.sonymobile.androidapp.gridcomputing.utils.AchievementUtils;

/**
 * ViewHolder for Achievement items.
 */
public class AchievementViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    /**
     * Root view.
     */
    private View mRootView;
    /**
     * Click listener.
     */
    private AchievementsAdapter.OnItemClickListener mClickListener;
    /**
     * Achievement.
     */
    private Achievement mAchievement;

    /**
     * The class constructor.
     * @param itemView the item view.
     * @param clickListener the click listener.
     */
    public AchievementViewHolder(final View itemView,
                                 final AchievementsAdapter.OnItemClickListener clickListener) {
        super(itemView);

        mRootView = itemView;
        mRootView.setOnClickListener(this);
        mClickListener = clickListener;
    }

    /**
     * Bind achievement.
     * @param achievement the achievement
     */
    public void bindAchievement(final Achievement achievement) {
        mAchievement = achievement;

        AchievementUtils.updateAchievementText(achievement,
                (TextView) mRootView.findViewById(R.id.achievement_item_title),
                (TextView) mRootView.findViewById(R.id.achievement_item_description),
                (TextView) mRootView.findViewById(R.id.achievement_item_xp),
                (TextView) mRootView.findViewById(R.id.achievement_item_date));

        final ImageView imageView =
                ((ImageView) mRootView.findViewById(R.id.achievement_item_image));
        final AchievementProgressView achievementProgressView =
                (AchievementProgressView) mRootView.findViewById(R.id.achievement_progress_view);

        AchievementUtils.updateAchievementIcon(
                mRootView.getContext(), imageView, achievement, achievementProgressView);
    }

    @Override
    public void onClick(final View view) {
        mClickListener.onItemClicked(view, mAchievement);
    }
}
