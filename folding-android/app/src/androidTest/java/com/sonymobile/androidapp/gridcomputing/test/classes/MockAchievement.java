package com.sonymobile.androidapp.gridcomputing.test.classes;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.games.Player;
import com.google.android.gms.games.achievement.Achievement;

/**
 * Created by italo on 28/10/15.
 */
public class MockAchievement implements Achievement {

    public static final Parcelable.Creator<MockAchievement> CREATOR = new Parcelable.Creator<MockAchievement>() {

        @Override
        public MockAchievement createFromParcel(Parcel source) {
            return new MockAchievement(source);
        }

        @Override
        public MockAchievement[] newArray(int size) {
            return new MockAchievement[size];
        }
    };
    private String mId;
    private int mSteps;
    private int mCurrentSteps = 0;

    public MockAchievement(final String id, int steps) {
        mId = id;
        this.mSteps = steps;
    }

    public MockAchievement(final Parcel source) {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MockAchievement && mId.equals(((MockAchievement) o).mId);
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public String getAchievementId() {
        return mId;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void getName(CharArrayBuffer charArrayBuffer) {

    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void getDescription(CharArrayBuffer charArrayBuffer) {

    }

    @Override
    public Uri getUnlockedImageUri() {
        return null;
    }

    @Override
    public String getUnlockedImageUrl() {
        return null;
    }

    @Override
    public Uri getRevealedImageUri() {
        return null;
    }

    @Override
    public String getRevealedImageUrl() {
        return null;
    }

    @Override
    public int getTotalSteps() {
        return mSteps;
    }

    @Override
    public String getFormattedTotalSteps() {
        return null;
    }

    @Override
    public void getFormattedTotalSteps(CharArrayBuffer charArrayBuffer) {

    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public int getCurrentSteps() {
        return mCurrentSteps;
    }

    public void setCurrentSteps(int steps) {
        mCurrentSteps = Math.min(steps, getTotalSteps());
    }

    @Override
    public String getFormattedCurrentSteps() {
        return null;
    }

    @Override
    public void getFormattedCurrentSteps(CharArrayBuffer charArrayBuffer) {

    }

    @Override
    public long getLastUpdatedTimestamp() {
        return 0;
    }

    @Override
    public long getXpValue() {
        return 0;
    }

    @Override
    public Achievement freeze() {
        return null;
    }

    @Override
    public boolean isDataValid() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
