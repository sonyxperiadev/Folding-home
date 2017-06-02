/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.achievement.Achievement;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.views.AchievementProgressView;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;

/**
 * Helper class containing a few Achievement helper methods.
 */
public final class AchievementUtils {

    private AchievementUtils() { }

    public static void updateAchievementIcon(final Context context, final ImageView imageView,
                                             final Achievement achievement,
                                             final AchievementProgressView
                                                     achievementProgressView) {
        imageView.setVisibility(View.GONE);
        achievementProgressView.setVisibility(View.GONE);

        switch (achievement.getState()) {
            case Achievement.STATE_UNLOCKED:
                imageView.setVisibility(View.VISIBLE);
                ImageManager.create(context).loadImage(imageView,
                                                       achievement.getUnlockedImageUri());
                break;
            case Achievement.STATE_REVEALED:
                if (achievement.getType() == Achievement.TYPE_INCREMENTAL) {
                    achievementProgressView.setVisibility(View.VISIBLE);
                    achievementProgressView.setSteps(achievement.getCurrentSteps(),
                            achievement.getTotalSteps());
                    final int percentage = (int) (((float) achievement.getCurrentSteps()
                            / (float) achievement.getTotalSteps()) * 100);
                    achievementProgressView.setText(format("%d%%", percentage));
                } else {
                    imageView.setVisibility(View.VISIBLE);
                    ImageManager
                            .create(context).loadImage(imageView,
                                                       achievement.getRevealedImageUri());
                }
                break;
            case Achievement.STATE_HIDDEN:
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageResource(com.sonymobile.gamification.R.drawable.ic_lock_grey);
                break;
            default:
                break;
        }
    }

    public static void updateAchievementText(final Achievement achievement,
                                             final TextView tvName, final TextView tvDescription,
                                             final TextView tvXp, final TextView tvTime) {
        tvTime.setVisibility(View.INVISIBLE);
        tvXp.setVisibility(View.INVISIBLE);

        switch (achievement.getState()) {
            case Achievement.STATE_UNLOCKED:
                tvTime.setVisibility(View.VISIBLE);

                final long timestamp = achievement.getLastUpdatedTimestamp();
                if (timestamp > 0) {
                    final Date date = new Date(achievement.getLastUpdatedTimestamp());
                    final SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
                    tvTime.setText(formatter.format(date));
                }

                // Todo: Add Google Games XP logic.
                if (achievement.getXpValue() > 0) {
                    final String xpString = achievement.getXpValue() + " XP";
                    tvXp.setText(xpString);
                }
                tvName.setText(achievement.getName());
                tvDescription.setText(achievement.getDescription());
                break;
            case Achievement.STATE_REVEALED:
                tvName.setText(achievement.getName());
                tvDescription.setText(achievement.getDescription());
                break;
            case Achievement.STATE_HIDDEN:
                tvName.setText(R.string.games_achievement_hidden_name);
                tvDescription.setText(R.string.games_achievement_hidden_desc);
                break;
            default:
                break;
        }
    }
}
