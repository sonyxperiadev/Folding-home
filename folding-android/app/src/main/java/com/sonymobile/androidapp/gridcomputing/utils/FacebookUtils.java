/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import android.content.Context;

import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;

import java.util.Collections;
import java.util.List;

/**
 * Facebook helper class.
 */
public final class FacebookUtils {

    /**
     * Facebook permissions of the logged user.
     */
    public static final List<String> PERMISSIONS = Collections.singletonList("publish_actions");

    /**
     * Facebook app namespace job.
     */
    private static final String POST_OBJECT_PATH = "<PLACE_YOUR_FACEBOOK_OPEN_GRAPH_TYPE_HERE app:action>";

    /**
     * Facebook app namespace job.
     */
    private static final String GRAPH_OBJECT = "<PLACE_YOUR_FACEBOOK_OPEN_GRAPH_TYPE_HERE>";

    /**
     * Facebook app namespace action.
     */
    private static final String POST_ACTION_PATH = "<PLACE_YOUR_FACEBOOK_OPEN_GRAPH_TYPE_HERE app:actionPath>";

    /**
     * Icon image url.
     */
    private static final String POST_ACTION_IMAGE_URL =
            "<PLACE_YOUR_IMAGE_URL_HERE>";


    /**
     * Empty constructor.
     */
    private FacebookUtils() {

    }

    /**
     * Create open graph object.
     *
     * @return open graph object.
     */
    public static ShareOpenGraphObject createOpenGraphObject() {
        final Context context = ApplicationData.getAppContext();

        final long timeMillis = RunningPref.getAccumulatedTime();
        final long nUsers = RunningPref.getNumberOfUsers();
        final String time = FormatUtils.getMainTimeString(timeMillis);

        ShareOpenGraphObject.Builder builder = new ShareOpenGraphObject.Builder();
        builder.putString("og:type", POST_OBJECT_PATH);
        builder.putString("og:title", context.getString(R.string.app_name));
        builder.putString("og:description",
                context.getString(R.string.share_object_message, nUsers, time) + "\n\n"
                        + context.getString(R.string.google_play_url));
        builder.putString("og:url", context.getString(R.string.stanford_folding_url));
        builder.putString("og:image", POST_ACTION_IMAGE_URL);
        builder.putLong("gridcomputing:number_users", nUsers);
        builder.putString("gridcomputing:time_spent", time);

        return builder.build();
    }

    /**
     * Create open graph action for the given object.
     *
     * @param object object used for the action.
     * @return open graph action.
     */
    public static ShareOpenGraphAction createOpenGraphAction(final ShareOpenGraphObject object) {
        final ShareOpenGraphAction.Builder builder = new ShareOpenGraphAction.Builder();

        builder.setActionType(POST_ACTION_PATH);
        builder.putObject(GRAPH_OBJECT, object);

        return builder.build();
    }

    /**
     * Create open graph content for the given action.
     *
     * @param action open graph action.
     * @return open graph content.
     */
    public static ShareOpenGraphContent createOpenGraphContent(final ShareOpenGraphAction action) {
        final ShareOpenGraphContent.Builder builder = new ShareOpenGraphContent.Builder();

        builder.setPreviewPropertyName(GRAPH_OBJECT);
        builder.setAction(action);

        return builder.build();
    }

}
