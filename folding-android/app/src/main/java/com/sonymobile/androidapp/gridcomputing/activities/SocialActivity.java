/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */
package com.sonymobile.androidapp.gridcomputing.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.Sharer.Result;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.widget.ShareDialog;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.gamification.Scores;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;
import com.sonymobile.androidapp.gridcomputing.utils.FacebookUtils;
import com.sonymobile.androidapp.gridcomputing.utils.FormatUtils;
import com.sonymobile.androidapp.gridcomputing.utils.NetworkUtils;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import io.fabric.sdk.android.Fabric;

import java.util.Arrays;

/**
 * Base Activity that handles social network actions.
 */
public class SocialActivity extends AppCompatActivity {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "INSERT_KEY";
    private static final String TWITTER_SECRET = "INSERT_SECRET";

    /**
     * Facebook callback manager.
     */
    private CallbackManager mCallbackManager;

    /**
     * Facebook Access token tacker.
     */
    private AccessTokenTracker mTokenTracker;

    /**
     * Facebook share dialog.
     */
    private ShareDialog mShareDialog;

    /**
     * Facebook SDK automatically shows a Toast message after sharing using its native dialog. This
     * is used to control if we need to show a custom Toast or the Facebook SDK will handle it.
     */
    private boolean mShowToast;

    /**
     * Twitter authentication client.
     */
    private TwitterAuthClient mTwitterAuthClient;

    /**
     * Callback received when Twitter when you try to login with Twitter.
     */
    private final Callback<TwitterSession> TWITTER_LOGIN_CALLBACK = new Callback<TwitterSession>() {
        @Override
        public void success(final com.twitter.sdk.android.core.Result<TwitterSession> result) {
            Log.d("Twitter: Sign-in success!");
            shareOnTwitter();
        }

        @Override
        public void failure(final TwitterException e) {
            Log.e("Twitter: Sign-in Failed! " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        super.onCreate(savedInstanceState);

        mCallbackManager = CallbackManager.Factory.create();

        mTokenTracker = new AccessTokenTracker() {

            @Override
            protected void onCurrentAccessTokenChanged(final AccessToken oldAccessToken,
                                                       final AccessToken currentAccessToken) {
                Log.d("Facebook Access token changed. OLD: " + oldAccessToken + " NEW: "
                        + currentAccessToken);
                if (oldAccessToken != currentAccessToken && currentAccessToken == null) {
                    facebookLogin();
                } else {
                    AccessToken.setCurrentAccessToken(currentAccessToken);
                }
            }
        };
        mTokenTracker.startTracking();

        mShareDialog = new ShareDialog(this);
        // this part is optional
        mShareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {

            @Override
            public void onSuccess(final Result result) {
                if (mShowToast) {
                    if (result != null && !TextUtils.isEmpty(result.getPostId())) {
                        Toast.makeText(ApplicationData.getAppContext(),
                                R.string.share_facebook_success, Toast.LENGTH_SHORT).show();
                    }
                }
                Scores.unlockAchievementShareOnFacebook();
                Log.d("Facebook: " + result);
            }

            @Override
            public void onCancel() {
                Log.d("Facebook: " + "cancel");
            }

            @Override
            public void onError(final FacebookException error) {
                Log.d("Facebook: " + error.getLocalizedMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        mTokenTracker.stopTracking();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mCallbackManager != null) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        if (mTwitterAuthClient != null) {
            mTwitterAuthClient.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Shares contribution on Facebook.
     */
    public final void shareOnFacebook() {
        if (NetworkUtils.isConnected()) {
            final ShareOpenGraphObject object = FacebookUtils.createOpenGraphObject();
            final ShareOpenGraphAction action = FacebookUtils.createOpenGraphAction(object);
            final ShareOpenGraphContent content = FacebookUtils.createOpenGraphContent(action);

            mShowToast = !mShareDialog.canShow(content, ShareDialog.Mode.NATIVE);
            mShareDialog.show(content);
        } else {
            NetworkUtils.showNoNetworkError(this);
        }
    }

    /**
     * Do Facebook Login.
     */
    private void facebookLogin() {
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {

                    @Override
                    public void onSuccess(final LoginResult result) {
                        if (AccessToken.getCurrentAccessToken().getPermissions()
                                .containsAll(FacebookUtils.PERMISSIONS)) {
                            shareOnFacebook();
                        } else {
                            LoginManager.getInstance().logInWithPublishPermissions(
                                    SocialActivity.this, FacebookUtils.PERMISSIONS);
                        }
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onError(final FacebookException error) {
                    }
                });
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(""));
    }

    /**
     * Shares this contribution on Twitter.
     */
    public void shareOnTwitter() {
        if (Twitter.getSessionManager().getActiveSession() == null) {
            Log.d("Twitter: Requires sign-in!");
            mTwitterAuthClient = new TwitterAuthClient();
            mTwitterAuthClient.authorize(this, TWITTER_LOGIN_CALLBACK);
        } else {
            Log.d("Twitter: sending tweet!");
            final long timeMillis = RunningPref.getAccumulatedTime();
            final long nUsers = RunningPref.getNumberOfUsers();
            final String time = FormatUtils.getMainTimeString(timeMillis);

            final String statusStr = getString(R.string.share_object_message, nUsers, time) + "\n\n"
                    + getString(R.string.google_play_url);

            TweetComposer.Builder builder = new TweetComposer.Builder(this)
                    .text(statusStr);
            builder.show();

        }
    }
}

