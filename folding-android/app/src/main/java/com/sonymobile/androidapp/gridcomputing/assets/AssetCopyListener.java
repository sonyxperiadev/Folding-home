/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.assets;

/**
 * Interface definition for a callback to be invoked when the assets copy is
 * finished.
 */
public interface AssetCopyListener {

    /**
     * Notifies when the assets are copied successfully.
     */
    void onAssetCopySuccess();

    /**
     * Notifies when the assets are NOT copied successfully.
     */
    void onAssetCopyError();
}
