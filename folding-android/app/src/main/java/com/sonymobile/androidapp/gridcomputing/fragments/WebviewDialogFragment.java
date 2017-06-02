/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sonymobile.androidapp.gridcomputing.R;

/**
 * Class to load a url and displays in a webview.
 */
public class WebviewDialogFragment extends DialogFragment {

    /**
     * Tag used when adding this fragment.
     */
    public static final String DIALOG_TAG = "webview_tag";

    /**
     * Key string for the url extra sent to this fragment inside arguments().
     */
    private static final String URL_KEY = "com.sonymobile.androidapp.gridcomputing.URL_KEY";

    /**
     * Creates a new instance of this fragment.
     *
     * @param url the url to load.
     * @return this fragment instance.
     */
    public static DialogFragment newInstance(final String url) {
        final DialogFragment fragment = new WebviewDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(URL_KEY, url);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public final View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                                   final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.webview_fragment, container, false);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final WebView webView = (WebView) view.findViewById(R.id.webview);
        final View progress = view.findViewById(R.id.webview_progress);

        final String researchUrl = getArguments().getString(URL_KEY);

        progress.setVisibility(View.VISIBLE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                if (TextUtils.isEmpty(researchUrl) || researchUrl.equals(url)) {
                    return false;
                } else {
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(url)));
                    return true;
                }
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                progress.setVisibility(View.GONE);
            }
        });

        webView.loadUrl(researchUrl);
    }

    @Override
    public final Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public final void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public final void show(final FragmentManager manager, final String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }
}
