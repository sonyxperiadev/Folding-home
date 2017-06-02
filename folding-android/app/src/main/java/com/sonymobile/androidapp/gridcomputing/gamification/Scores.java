/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.gamification;

import android.os.SystemClock;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.sonymobile.androidapp.gridcomputing.BuildConfig;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.preferences.GamePref;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Helper class to submit scores to leaderboards and unlock achievements.
 */
public final class Scores {

    /**
     * Invalid score flag.
     */
    private static final int INVALID_SCORE = 1;

    /**
     * Nine Minutes.
     */
    private static final long NINE_MINUTES = 9;

    /**
     * Thirty seconds.
     */
    private static final long THIRTY_SECONDS = 30;

    /**
     * Accumulates the score time and just send when it is greater than the this time. This is
     * necessary to avoid high bandwidth consumption. The time should be 10 minutes, but we have to
     * take into account an error margin (30 seconds).
     */
    private static final long SEND_SCORE_TIME_THRESHOLD = TimeUnit.MINUTES.toMillis(NINE_MINUTES)
            + TimeUnit.SECONDS.toMillis(THIRTY_SECONDS);
    /**
     * Error minutes to compare with the last time contributed.
     */
    private static final int ERROR_MINUTES = 2;
    /**
     * Six hours to unlock the achievement.
     */
    private static final int SIX_HOURS_STRAIGHT_BREAST_CANCER = 6;
    /**
     * Twelve hours to unlock the achievement.
     */
    private static final int TWELVE_HOURS_STRAIGHT_BREAST_CANCER = 12;
    /**
     * Eighteen hours to unlock the achievement.
     */
    private static final int EIGHTEEN_HOURS_STRAIGHT_BREAST_CANCER = 18;
    /**
     * Twenty-four to unlock the achievement.
     */
    private static final int TWENTY_FOUR_HOURS_STRAIGHT_BREAST_CANCER = 24;

    /**
     * Step to increment an achievement.
     */
    private static final long STEP_INCREMENT_ACHIEVEMENT = TimeUnit.MINUTES.toMillis(10);
    /**
     * Id breast cancer research.
     */
    private static final String ID_BREAST_CANCER_RESEARCH = "<PLACE_YOUR_BREAST_CANCER_ID_HERE>";
    /**
     * Flag to indicated if is submiting score.
     */
    private static final AtomicBoolean SUBMITING_SCORE = new AtomicBoolean();
    /**
     * Last time a score was submitted.
     */
    private static long sLastTimeContributed = 0;
    /**
     * Accumulates the straight time of contribution.
     */
    private static long sTimeContributedStraight = 0;

    /**
     * Private constructor.
     */
    private Scores() {
    }

    /**
     * Updates score and user achievements.
     *
     * @param googleApiClient the GoogleApiClient used to send the score.
     */
    public static void submitScore(final GoogleApiClient googleApiClient) {
        final boolean isConnected = googleApiClient != null && googleApiClient.isConnected();
        Log.d("SubmitScore > isConnected: " + isConnected);
        final long currentLocalScore = GamePref.getScoreToSubmit();
        final boolean timeThresholdCondition = BuildConfig.TEST_MODE
                || currentLocalScore > SEND_SCORE_TIME_THRESHOLD;
        Log.d("SubmitScore > flag: " + SUBMITING_SCORE.get());
        if (timeThresholdCondition && isConnected && !SUBMITING_SCORE.get()) {
            SUBMITING_SCORE.set(true);
            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(googleApiClient,
                    ApplicationData.getAppContext()
                            .getString(R.string.leaderboard_leaderboards),
                    LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC)
                    .setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                        @Override
                        public void onResult(final Leaderboards.LoadPlayerScoreResult resultScore) {
                            long newScore = currentLocalScore;
                            Log.d("SubmitScore > newScore: " + newScore);
                            unlockAchievementsStraight(googleApiClient, newScore);

                            Log.d("SubmitScore > LoadPlayerScoreResult status: " + resultScore
                                    .getStatus().getStatusMessage());

                            final LeaderboardScore playerScore = resultScore.getScore();
                            if (playerScore == null) {
                                Log.d("SubmitScore > playerScore is currently null");
                                newScore = INVALID_SCORE;
                            } else {
                                Log.d("SubmitScore > Leaderboard score: " + playerScore
                                        .getRawScore());
                                newScore += playerScore.getRawScore();
                                RunningPref.setAccumulatedTime(newScore);
                            }

                            unlockIncrementalAchievements(googleApiClient, newScore);

                        }

                    }, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * Unlock incremental scores achievements.
     *
     * @param googleApiClient the GoogleApiClient used to send the score.
     * @param score           Current score to be sent.
     */
    private static void unlockIncrementalAchievements(final GoogleApiClient googleApiClient,
                                                      final long score) {
        Log.d("SubmitScore > totalScore: " + score);
        final String leaderboardId = ApplicationData.getAppContext()
                .getString(R.string.leaderboard_leaderboards);
        final PendingResult<Leaderboards.SubmitScoreResult> result =
                Games.Leaderboards.submitScoreImmediate(googleApiClient, leaderboardId, score);
        result.setResultCallback(
                new ResultCallback<Leaderboards.SubmitScoreResult>() {
                    @Override
                    public void onResult(
                            final Leaderboards.SubmitScoreResult submitScoreResult) {
                        if (submitScoreResult.getStatus().isSuccess()) {
                            if (score != INVALID_SCORE) {
                                GamePref.resetScore();
                            }
                            setIncrementalAchievements(googleApiClient, score);
                        }
                        SUBMITING_SCORE.set(false);
                        Log.d("submitScoreResult: " + submitScoreResult.getStatus().isSuccess());
                    }
                }, 1, TimeUnit.MINUTES);
    }

    /**
     * Method to unlock the achievement to share on facebook.
     */
    public static void unlockAchievementShareOnFacebook() {
        if (GameHelper.getApiClient().isConnected()) {
            Games.Achievements.unlock(GameHelper.getApiClient(), ApplicationData.getAppContext()
                    .getString(R.string.achievement_share_on_facebook));
        }
    }

    /**
     * Method to unlock the achievement of opening the description of a specific research.
     *
     * @param id the research id.
     */
    public static void unlockAchievementResearchDescription(final String id) {

        if (GameHelper.getApiClient().isConnected()) {
            if (id.equals(ID_BREAST_CANCER_RESEARCH)) {
                Games.Achievements.unlock(GameHelper.getApiClient(), ApplicationData.getAppContext()
                        .getString(R.string.achievement_open_the_breast_cancer_details));
            }
        }
    }

    /**
     * Method to unlock achievements of straight contributions.
     *
     * @param googleApiClient the GoogleApiClient used to unlock achievements.
     * @param score           the score will be increased.
     */
    private static void unlockAchievementsStraight(
            final GoogleApiClient googleApiClient, final long score) {

        if (sLastTimeContributed == 0) {
            sLastTimeContributed = SystemClock.elapsedRealtime();
        } else {
            if ((sLastTimeContributed - TimeUnit.MINUTES.toMillis(ERROR_MINUTES))
                    <= (SystemClock.elapsedRealtime() - score)
                    && (SystemClock.elapsedRealtime() - score)
                    <= (sLastTimeContributed + TimeUnit.MINUTES.toMillis(ERROR_MINUTES))) {
                sTimeContributedStraight += score;
            } else {
                sTimeContributedStraight = 0;
            }
            sLastTimeContributed = SystemClock.elapsedRealtime();
        }

        if (RunningPref.getResearchId().equals(ID_BREAST_CANCER_RESEARCH)) {
            if (TimeUnit.MILLISECONDS.toHours(sTimeContributedStraight)
                    >= SIX_HOURS_STRAIGHT_BREAST_CANCER) {
                Games.Achievements.unlock(googleApiClient, ApplicationData.getAppContext()
                        .getString(R.string.achievement_6_hours_straight_breast_cancer));
            }

            if (TimeUnit.MILLISECONDS.toHours(sTimeContributedStraight)
                    >= TWELVE_HOURS_STRAIGHT_BREAST_CANCER) {
                Games.Achievements.unlock(googleApiClient, ApplicationData.getAppContext()
                        .getString(R.string.achievement_12_hours_straight_breast_cancer));
            }

            if (TimeUnit.MILLISECONDS.toHours(sTimeContributedStraight)
                    >= EIGHTEEN_HOURS_STRAIGHT_BREAST_CANCER) {
                Games.Achievements.unlock(googleApiClient, ApplicationData.getAppContext()
                        .getString(R.string.achievement_18_hours_straight_breast_cancer));
            }

            if (TimeUnit.MILLISECONDS.toHours(sTimeContributedStraight)
                    >= TWENTY_FOUR_HOURS_STRAIGHT_BREAST_CANCER) {
                Games.Achievements.unlock(googleApiClient, ApplicationData.getAppContext()
                        .getString(R.string.achievement_24_hours_straight_breast_cancer));
            }
        } else {
            sTimeContributedStraight = 0;
        }
    }

    /**
     * Method to sort the incremental achievements.
     *
     * @param loadAchievementsResult All achievements.
     *
     * @return a list of ordered achievements.
     */
    private static List<Achievement> sortedAchievements(
            final Achievements.LoadAchievementsResult loadAchievementsResult) {
        List<Achievement> result = new ArrayList<>();

        Achievement aminoacid = null;
        Achievement peptide = null;
        Achievement polypeptide = null;
        Achievement protein = null;
        Achievement enzyme = null;

        for (Achievement achievement : loadAchievementsResult.getAchievements()) {
            if (achievement.getAchievementId().equals(ApplicationData.getAppContext()
                    .getString(R.string.achievement_aminoacid))) {
                aminoacid = achievement;
            } else if (achievement.getAchievementId().equals(ApplicationData.getAppContext()
                    .getString(R.string.achievement_peptide))) {
                peptide = achievement;
            } else if (achievement.getAchievementId().equals(ApplicationData.getAppContext()
                    .getString(R.string.achievement_polypeptide))) {
                polypeptide = achievement;
            } else if (achievement.getAchievementId().equals(ApplicationData.getAppContext()
                    .getString(R.string.achievement_protein))) {
                protein = achievement;
            } else if (achievement.getAchievementId().equals(ApplicationData.getAppContext()
                    .getString(R.string.achievement_enzyme))) {
                enzyme = achievement;
            }
        }

        result.add(aminoacid);
        result.add(peptide);
        result.add(polypeptide);
        result.add(protein);
        result.add(enzyme);

        return result;
    }

    /**
     * Method for increment achievements.
     *
     * @param googleApiClient the GoogleApiClient used to unlock achievements.
     * @param score           the time that the user has already contributed.
     */
    public static void setIncrementalAchievements(
            final GoogleApiClient googleApiClient, final long score) {


        Games.Achievements.unlock(googleApiClient, ApplicationData.getAppContext()
                .getString(R.string.achievement_start_folding));

        Games.Achievements.load(googleApiClient, false)
                .setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
                    @Override
                    public void onResult(
                            final Achievements.LoadAchievementsResult loadAchievementsResult) {
                        unlockIncrementalAchievements(googleApiClient, score, Games.Achievements,
                                sortedAchievements(loadAchievementsResult));
                    }
                });
    }

    /**
     * @param googleApiClient    the GoogleApiClient used to unlock achievements.
     * @param score              the time that the user has already contributed.
     * @param achievements       incremental achievements.
     * @param sortedAchievements sorted Achievement list.
     */
    public static void unlockIncrementalAchievements(final GoogleApiClient googleApiClient,
                                                     final long score,
                                                     final Achievements achievements,
                                                     final List<Achievement> sortedAchievements) {
        final long totalSteps = score / STEP_INCREMENT_ACHIEVEMENT;
        int accumulatedSteps = 0;
        for (Achievement achievement : sortedAchievements) {
            if (achievement != null) {
                accumulatedSteps += achievement.getTotalSteps();
                int stepsToSubmit = calculateStepsToSubmit(
                        totalSteps, accumulatedSteps,
                        achievement.getTotalSteps());
                if (stepsToSubmit > 0) {
                    achievements.setSteps(
                            googleApiClient, achievement.getAchievementId(),
                            stepsToSubmit);
                }
            }
        }
    }

    /**
     * Calculates the steps to submit based on the number.
     *
     * @param totalSteps       total number of steps based on the current contribution time.
     * @param accumulatedSteps accumulated steps based on other achievements.
     * @param steps            current achievement steps.
     *
     * @return the number of steps to submit to Google Play Games API.
     */
    public static int calculateStepsToSubmit(final long totalSteps,
                                             final int accumulatedSteps,
                                             final int steps) {
        final float stepsRatio = Math.min(1F, (float) totalSteps
                / (float) accumulatedSteps);
        return (int) (stepsRatio * steps);
    }
}
