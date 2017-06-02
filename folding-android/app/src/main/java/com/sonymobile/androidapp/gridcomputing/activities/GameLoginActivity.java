/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.activities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.gamification.GameHelper;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.preferences.GamePref;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;
import com.sonymobile.androidapp.gridcomputing.utils.NetworkUtils;

/**
 * Activity that handles login into google Play games Services.
 */
public class GameLoginActivity extends SocialActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Request code to use when launching the resolution activity.
     */
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    /**
     * Request code to use when launching the activity of achievements or leaderboards.
     */
    private static final int RC_UNUSED = 5001;

    /**
     * Unique tag for the state resolving error.
     */
    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";

    /**
     * Unique tag for the state sign in flow.
     */
    private static final String STATE_SIGN_IN_FLOW = "STATE_SIGN_IN_FLOW";

    /**
     * Unique tag for the error dialog fragment.
     */
    private static final String DIALOG_ERROR = "dialog_error";

    /**
     * Flag to resolving error.
     */
    private boolean mResolvingError = false;

    /**
     * Flag to sign in flow.
     */
    private boolean mInSignInFlow = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
            mInSignInFlow = savedInstanceState.getBoolean(STATE_SIGN_IN_FLOW, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocalScore();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mInSignInFlow && !GamePref.getSignedOutExplicitly()) {
            // auto sign in
            firstTimeConnect();
        }
    }

    @Override
    protected void onStop() {
        GameHelper.unregisterCallbacks(this, this);
        super.onStop();
    }

    @Override
    public final void onConnected(final Bundle bundle) {
        mInSignInFlow = false;
        invalidateOptionsMenu();
        GamePref.setSignedOutExplicitly(false);
        Games.setViewForPopups(GameHelper.getApiClient(), getWindow().getDecorView()
                .findViewById(android.R.id.content));
        updateLocalScore();
    }

    /**
     * Updates the local time with the Games Service time.
     */
    protected final void updateLocalScore() {
        if (isConnected()) {
            try {
                Games.Leaderboards.loadCurrentPlayerLeaderboardScore(GameHelper.getApiClient(),
                        ApplicationData.getAppContext()
                                .getString(R.string.leaderboard_leaderboards),
                        LeaderboardVariant.TIME_SPAN_ALL_TIME,
                        LeaderboardVariant.COLLECTION_PUBLIC)
                        .setResultCallback(
                                new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                                    @Override
                                    public void onResult(final Leaderboards.LoadPlayerScoreResult
                                                                 scoreResult) {
                                        if (scoreResult.getScore() != null
                                                && scoreResult.getScore().getRawScore() > 0L) {
                                            final long scoreToSubmit = GamePref.getScoreToSubmit();
                                            Log.d("submitScore > updateLocalScore: "
                                                    + scoreToSubmit + ", "
                                                    + scoreResult.getScore().getRawScore());
                                            final long currentScore = scoreToSubmit
                                                    + scoreResult.getScore().getRawScore();
                                            RunningPref.setAccumulatedTime(currentScore);
                                        }
                                    }
                                });
            } catch (SecurityException ex) {
                tryToDisconnect();
            }
        }
    }

    @Override
    public final void onConnectionSuspended(final int i) {
        mInSignInFlow = false;
    }

    @Override
    public final void onConnectionFailed(final ConnectionResult connectionResult) {
        if (!mResolvingError) {
            if (connectionResult.hasResolution()) {
                try {
                    mResolvingError = true;
                    connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                    mInSignInFlow = true;
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    GameHelper.tryToConnect();
                    mInSignInFlow = true;
                }
            } else {
                // Show dialog using GooglePlayServicesUtil.getErrorDialog()
                showErrorDialog(connectionResult.getErrorCode());
                mResolvingError = true;
            }
        }
    }

    @Override
    protected final void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putBoolean(STATE_SIGN_IN_FLOW, mResolvingError);
    }

    /**
     * Verifies that the google API is connected.
     *
     * @return true if connected and false not connected
     */
    protected final boolean isConnected() {
        return GameHelper.getApiClient().isConnected();
    }

    /**
     * Tries to login. This should be called when the user explicitly asks to login.
     */
    protected final void doLogin() {
        if (NetworkUtils.isConnected()) {
            GamePref.setSignedOutExplicitly(false);
            firstTimeConnect();
        } else {
            NetworkUtils.showNoNetworkError(this);
        }
    }

    /**
     * Create the Google Api Client with access to the Play Game services and try to connect.
     */
    private void firstTimeConnect() {
        GameHelper.registerCallbacks(this, this);
        tryToConnect();
    }

    /**
     * Tries to connect with the Google Game service.
     */
    private void tryToConnect() {
        if (!mResolvingError && GameHelper.tryToConnect()) {
            GameHelper.getApiClient().connect();
            mInSignInFlow = true;
        }
    }

    /**
     * Try to disconnect from the Google Game service.
     */
    protected final void tryToDisconnect() {
        if (NetworkUtils.isConnected()) {
            if (GameHelper.tryToDisconnect()) {
                invalidateOptionsMenu();
                Toast.makeText(this, R.string.toast_logout, Toast.LENGTH_SHORT).show();
            }
        } else {
            NetworkUtils.showNoNetworkError(this);
        }
    }

    /**
     * Starts the leaderboard intent.
     */
    protected final void startLeaderBoardIntent() {
        if (isConnected()) {
            startActivityForResult(
                    Games.Leaderboards.getLeaderboardIntent(GameHelper.getApiClient(),
                            ApplicationData.getAppContext()
                                    .getString(R.string.leaderboard_leaderboards)), RC_UNUSED);
        }
    }

    /**
     * Starts the achievements intent.
     */
    protected final void startAchievementsIntent() {
        if (isConnected()) {
            startActivityForResult(
                    Games.Achievements.getAchievementsIntent(GameHelper.getApiClient()), RC_UNUSED);
        }
    }

    /**
     * Creates a dialog for an error message.
     *
     * @param errorCode the error code
     */
    private void showErrorDialog(final int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), DIALOG_ERROR);
    }

    /**
     * Called from ErrorDialogFragment when the dialog is dismissed.
     */
    protected final void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    protected final void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                GameHelper.tryToConnect();
            } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                tryToDisconnect();
            } else if (resultCode == RESULT_CANCELED) {
                GamePref.setSignedOutExplicitly(true);
            }
            // If the user logout by google games screen.
        } else if (requestCode == RC_UNUSED
                && resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            GamePref.setSignedOutExplicitly(true);
            GameHelper.getApiClient().disconnect();
            invalidateOptionsMenu();
        }
    }

    /**
     * Gets the logged user's account name.
     *
     * @return the logged user's account name.
     */
    protected final String getAccountUserName() {
        Player p = Games.Players.getCurrentPlayer(GameHelper.getApiClient());
        if (p != null) {
            return p.getDisplayName();
        }
        return "";
    }

    /**
     * A fragment to display an error dialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public final Dialog onCreateDialog(final Bundle savedInstanceState) {
            /* Get the error code and retrieve the appropriate dialog */
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public final void onDismiss(final DialogInterface dialog) {
            if (isAdded()) {
                ((SummaryActivity) getActivity()).onDialogDismissed();
            }
        }
    }
}
