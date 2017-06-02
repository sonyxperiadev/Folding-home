/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.games.achievement.Achievement;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.utils.AchievementUtils;
import com.sonymobile.androidapp.gridcomputing.views.AchievementProgressView;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

/**
 * Dialog showing achievement info and share options.
 */
public class AchievementDialogFragment extends DialogFragment implements View.OnClickListener {

    /**
     * AchievementDialogFragment TAG.
     */
    public static final String TAG = "AchievementDialogFragment";

    /**
     * Achievement instance that will be exhibited within this dialog.
     */
    private Achievement mAchievement;

    /**
     * Creates a new instance of this DialogFragment using the Achievement param.
     *
     * @param achievement achievement to be shown within this dialog.
     *
     * @return a new instance of AchievementDialogFragment.
     */
    public static AchievementDialogFragment newInstance(final Achievement achievement) {
        AchievementDialogFragment fragment = new AchievementDialogFragment();
        fragment.setAchievement(achievement);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE,
                android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_achievement_item_dialog, container);

        AchievementUtils.updateAchievementText(mAchievement,
                (TextView) view.findViewById(R.id.achievement_item_title),
                (TextView) view.findViewById(R.id.achievement_item_description),
                (TextView) view.findViewById(R.id.achievement_item_xp),
                (TextView) view.findViewById(R.id.achievement_item_date));

        final ImageView imageView =
                ((ImageView) view.findViewById(R.id.achievement_item_image));
        final AchievementProgressView achievementProgressView =
                (AchievementProgressView) view.findViewById(R.id.achievement_progress_view);

        AchievementUtils.updateAchievementIcon(
                getActivity(), imageView, mAchievement, achievementProgressView);
        updateShareableStatus(view);

        return view;
    }

    /**
     * Update shareable button statuses.
     */
    private void updateShareableStatus(final View rootView) {
        if (mAchievement.getState() == Achievement.STATE_UNLOCKED) {
            rootView.findViewById(R.id.achievement_bt_share_facebook).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.achievement_bt_share_facebook).setOnClickListener(this);
            rootView.findViewById(R.id.achievement_bt_share_twitter).setOnClickListener(this);
        } else {
            rootView.findViewById(R.id.achievement_bt_share_facebook).setVisibility(View.GONE);
        }

    }

    /**
     * Achievement setter.
     *
     * @param achievement dialog fragment achievement.
     */
    private void setAchievement(final Achievement achievement) {
        mAchievement = achievement;
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.achievement_bt_share_facebook:
                final ShareLinkContent content = new ShareLinkContent.Builder()
                        .setContentUrl(Uri.parse(getString(R.string.stanford_folding_url)))
                        .setImageUrl(Uri.parse(mAchievement.getUnlockedImageUrl()))
                        .setContentTitle(mAchievement.getName())
                        .setContentDescription(mAchievement.getDescription())
                        .build();

                ShareDialog.show(getActivity(), content);
                break;
            case R.id.achievement_bt_share_twitter:
                final String url = getString(R.string.google_play_url_shortened);
                TweetComposer.Builder builder = new TweetComposer.Builder(getActivity())
                        .text(mAchievement.getName() + "\n\n"
                                  + mAchievement.getDescription() + "\n" + url);
                builder.show();

                break;
            default:
                // ignore
                break;
        }
    }
}
