/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.androidapp.gridcomputing.R;
import com.sonymobile.androidapp.gridcomputing.adapters.ConditionsSlidePagerAdapter;
import com.sonymobile.androidapp.gridcomputing.conditions.ConditionType;
import com.sonymobile.androidapp.gridcomputing.conditions.ConditionsHandler;
import com.sonymobile.androidapp.gridcomputing.database.JobCheckpointsContract;
import com.sonymobile.androidapp.gridcomputing.gamification.Scores;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.messages.ConditionMessage;
import com.sonymobile.androidapp.gridcomputing.messages.JobExecutionMessage;
import com.sonymobile.androidapp.gridcomputing.notifications.NotificationHelper;
import com.sonymobile.androidapp.gridcomputing.notifications.NotificationStatus;
import com.sonymobile.androidapp.gridcomputing.preferences.MiscPref;
import com.sonymobile.androidapp.gridcomputing.preferences.RunningPref;
import com.sonymobile.androidapp.gridcomputing.preferences.SettingsPref;
import com.sonymobile.androidapp.gridcomputing.service.ServiceManager;
import com.sonymobile.androidapp.gridcomputing.utils.AlarmUtils;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;
import com.sonymobile.androidapp.gridcomputing.utils.FormatUtils;
import com.sonymobile.androidapp.gridcomputing.utils.NetworkUtils;
import com.sonymobile.androidapp.gridcomputing.utils.ViewUtils;
import com.sonymobile.androidapp.gridcomputing.views.CheckableImageButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary activity that presents the number of jobs contributed and the total
 * contributed time.
 */
public class SummaryActivity extends GameLoginActivity implements
        OnClickListener {

    /**
     * Legal filename.
     */
    private static final String LEGAL_FILENAME = "Legal.txt";

    /**
     * Alpha value used to dim the background image when the conditions viewpager is visible.
     */
    private static final float BACKGROUND_IMAGE_ALPHA = .1F;

    /**
     * Status bar view.
     */
    private View mStatusView;

    /**
     * Main switch used to turn on/off the contribution.
     */
    private CheckableImageButton mMenuSwitch;

    /**
     * Text View of description.
     */
    private TextView mTvDescription;

    /**
     * Text View of view more.
     */
    private TextView mTvViewMore;

    /**
     * Text View of title research type.
     */
    private TextView mTvResearchType;

    /**
     * View of description and view more.
     */
    private View mDetailsLayout;

    /**
     * Background image.
     */
    private ImageView mBackgroundImage;

    /**
     * The parent view onf the conditions layout.
     */
    private View mConditionsLayout;

    /**
     * The viewpager used to render the conditions.
     */
    private ViewPager mConditionsViewPager;

    /**
     * The viewgroup that contains all the indicators.
     */
    private ViewGroup mConditionsIndicator;

    /**
     * The conditions page adapter.
     */
    private ConditionsSlidePagerAdapter mAdapter;


    @Override
    protected final void onCreate(final Bundle savedState) {
        super.onCreate(savedState);
        Log.d("Activity > SummaryActivity onCreate");
        overridePendingTransition(0, 0);

        setContentView(R.layout.activity_summary);
        mStatusView = findViewById(R.id.status_bar);
        mBackgroundImage = (ImageView) findViewById(R.id.summary_image_background);
        mConditionsLayout = findViewById(R.id.summary_conditions_layout);
        mConditionsViewPager = (ViewPager) findViewById(R.id.summary_view_pager);
        mConditionsIndicator = (ViewGroup) findViewById(R.id.summary_page_indicator_container);

        mConditionsLayout.setVisibility(View.GONE);

        findViewById(R.id.summary_research_type_layout).setOnClickListener(this);
        findViewById(R.id.summary_contributed_time_layout).setOnClickListener(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        mMenuSwitch = (CheckableImageButton) findViewById(R.id.summary_menu_power_toggle);
        mMenuSwitch.setOnClickListener(this);

        toggleShareButtons();

        turnOff();
        AlarmUtils.createAlarm(AlarmUtils.AlarmType.REPEAT_1_MIN);

        if (getIntent().getBooleanExtra("login_ggs", false)) {
            doLogin();
        }

        mTvResearchType = (TextView) findViewById(R.id.title_research_type);

        LinearLayout linearLayout = (LinearLayout) findViewById(
                R.id.summary_research_type_internal_layout);
        mDetailsLayout = View.inflate(this, R.layout.research_details, linearLayout);
        mTvDescription = (TextView) mDetailsLayout.findViewById(R.id.description);
        mTvViewMore = (TextView) mDetailsLayout.findViewById(R.id.view_more);
        mTvViewMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                showResearchDescription();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ApplicationData.getBus().registerSticky(this);
        ConditionsHandler.getInstance().notifyConditionChanged(true);
        loadJobsStats(RunningPref.getNumberOfUsers(),
                RunningPref.getResearchType(), RunningPref.getAccumulatedTime());
    }

    @Override
    protected void onPause() {
        ApplicationData.getBus().unregister(this);
        super.onPause();
    }

    /**
     * Toggle the share buttons.
     */
    private void toggleShareButtons() {
        invalidateOptionsMenu();
    }

    @Override
    public final boolean onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.summary_share).setVisible(true);
        menu.findItem(R.id.summary_menu_active_research).setVisible(
                !TextUtils.isEmpty(RunningPref.getResearchUrl()));

        final boolean isConnected = isConnected();
        final boolean isEnabled = isEnabled();
        menu.findItem(R.id.action_login).setVisible(!isConnected);
        menu.findItem(R.id.action_logout).setVisible(isConnected);
        menu.findItem(R.id.action_enable).setVisible(!isEnabled);
        menu.findItem(R.id.action_disable).setVisible(isEnabled);
        if (isConnected) {
            menu.findItem(R.id.action_logout).setTitle(String.format(
                    getString(R.string.action_logout), getAccountUserName()));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.summary_menu, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        Intent intent;
        boolean result;
        switch (item.getItemId()) {
            case R.id.action_bar_facebook:
                if (NetworkUtils.isConnected()) {
                    shareOnFacebook();
                } else {
                    NetworkUtils.showNoNetworkError(this);
                }
                result = true;
                break;
            case R.id.action_bar_twitter:
                if (NetworkUtils.isConnected()) {
                    shareOnTwitter();
                } else {
                    NetworkUtils.showNoNetworkError(this);
                }
                result = true;
                break;
            case R.id.summary_menu_about:
                showAboutDialog();
                result = true;
                break;
            case R.id.summary_menu_wizard:
                intent = new Intent(this, WizardActivity.class);
                startActivity(intent);
                result = true;
                break;
            case R.id.summary_menu_active_research:
                showResearchDescription();
                result = true;
                break;
            case R.id.action_login:
                doLogin();
                result = true;
                break;
            case R.id.action_logout:
                tryToDisconnect();
                result = true;
                break;
            case R.id.action_leaderboards:
                if (isConnected()) {
                    startLeaderBoardIntent();
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(
                            R.string.toast_leaderboards_disabled), Toast.LENGTH_SHORT).show();
                }
                result = true;
                break;
            case R.id.action_achievements:
                if (isConnected()) {
                    //startAchievementsIntent();
                    startActivity(new Intent(this, AchievementsActivity.class));
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(
                            R.string.toast_achievements_disabled), Toast.LENGTH_SHORT).show();
                }
                result = true;
                break;
            case R.id.action_enable:
                ServiceManager.resume();
                result = true;
                break;
            case R.id.action_disable:
                SettingsPref.setExecutionEnabled(false);
                MiscPref.setLastBatteryPlateauTime(0);
                result = true;
                break;
            default:
                result = super.onOptionsItemSelected(item);
                break;
        }
        return result;
    }

    /**
     * Show about dialog.
     */
    private void showAboutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.legal);
        builder.setPositiveButton(android.R.string.ok, null);
        final LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final RelativeLayout relLayout = new RelativeLayout(this);

        final View content = inflater.inflate(R.layout.dialog_about, relLayout);

        builder.setView(content);

        final AsyncTask<Void, Void, List<String>> task = new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(final Void... params) {
                return getLegalString();
            }

            @Override
            protected void onPostExecute(final List<String> result) {
                try {
                    final ListView messageListView = (ListView) content
                            .findViewById(R.id.dialog_lv);
                    if (messageListView != null) {
                        messageListView.setAdapter(new ArrayAdapter<>(
                                SummaryActivity.this, R.layout.about_item,
                                result));
                    }
                    builder.create().show();
                } catch (final Exception ex) {
                    Log.e("Tried to show the Legal disclaimer dialog after"
                            + "the activity was finished.");
                }
            }
        };
        task.execute();
    }

    /**
     * Load and set the contributed time and contributed number of jobs.
     *
     * @param numberOfUsers   the number of users.
     * @param title           the title.
     * @param accumulatedTime the accumulated time.
     */
    public void loadJobsStats(final long numberOfUsers, final String title,
                              final long accumulatedTime) {

        if (!TextUtils.isEmpty(RunningPref.getResearchType())) {
            findViewById(R.id.summary_first_line).setVisibility(View.VISIBLE);
            findViewById(R.id.summary_second_line).setVisibility(View.VISIBLE);
            findViewById(R.id.summary_research_type_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.summary_people_helping_out_layout)
                    .setVisibility(View.VISIBLE);

            ((TextView) findViewById(R.id.summary_num_people_helping))
                    .setText(String.valueOf(numberOfUsers));
            ((TextView) findViewById(R.id.summary_research_type)).setText(title);
        } else {
            findViewById(R.id.summary_first_line).setVisibility(View.GONE);
            findViewById(R.id.summary_second_line).setVisibility(View.GONE);
            findViewById(R.id.summary_research_type_layout)
                    .setVisibility(View.GONE);
            findViewById(R.id.summary_people_helping_out_layout)
                    .setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.summary_donated_time_tv))
                .setText(FormatUtils.getMainTimeString(accumulatedTime));
    }

    @Override
    public final void onClick(final View view) {
        if (view.getId() == R.id.summary_menu_power_toggle) {
            if (isEnabled()) {
                if (SettingsPref.isPaused()) {
                    ServiceManager.resume();
                } else {
                    ServiceManager.pause();
                }
            } else {
                MiscPref.setLastBatteryPlateauTime(0);
                ServiceManager.resume();
            }
        } else if (view.getId() == R.id.summary_research_type_layout) {
            showResearchDescription();
            //uncomment the lines below to add expand/collapse behavior
//            if (mTvDescription.getVisibility() == View.GONE) {
//                mTvDescription.setText(RunningPref.getResearchDescription());
//                mTvDescription.setVisibility(View.VISIBLE);
//                mTvViewMore.setVisibility(View.VISIBLE);
//
//                mTvResearchType.setCompoundDrawablesRelativeWithIntrinsicBounds(
//                        null, null,
//                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_up),
//                        null);
//            } else {
//                mTvDescription.setVisibility(View.GONE);
//                mTvViewMore.setVisibility(View.GONE);
//
//                mTvResearchType.setCompoundDrawablesRelativeWithIntrinsicBounds(
//                        null, null, ContextCompat.getDrawable(getApplicationContext(),
//                                R.drawable.ic_arrow_down), null);
//            }
        } else if (view.getId() == R.id.summary_contributed_time_layout) {
            final Intent intent = new Intent(this, ReportsActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Shows a dialog with the research details.
     */
    private void showResearchDescription() {
        final String url = RunningPref.getResearchUrl();
        if (!TextUtils.isEmpty(url)) {
            String id = RunningPref.getResearchId();
            Scores.unlockAchievementResearchDescription(id);
            //DialogFragment newFragment = WebviewDialogFragment.newInstance(url);
            //newFragment.show(getFragmentManager(), WebviewDialogFragment.DIALOG_TAG);
            final Intent intent = new Intent(this, ProjectDetailsActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Read Legal text from Assets.
     *
     * @return Legal text as String.
     */
    private List<String> getLegalString() {
        final List<String> list = new ArrayList<>();
        final AssetManager assetsManager = getAssets();
        BufferedReader bufferedReader = null;
        String line;

        try {
            final InputStream inputStream = assetsManager.open(LEGAL_FILENAME);
            bufferedReader = new BufferedReader(new InputStreamReader(
                    inputStream, "UTF-8"));
            while ((line = bufferedReader.readLine()) != null) {
                list.add(line);
            }
        } catch (final IOException e) {
            Log.e(e.getLocalizedMessage());
        } finally {
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (final IOException exception) {
                    Log.e(exception.getLocalizedMessage());
                }
            }
        }

        return list;
    }

    /**
     * Turns off the execution.
     */
    private void turnOff() {
        ViewUtils.updateStatusBar(mStatusView, false, false, false, false, false);
    }

    /**
     * Verifies that folding is enabled.
     *
     * @return true if folding is enabled.
     */
    protected final boolean isEnabled() {
        return SettingsPref.isExecutionEnabled();
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(final ConditionMessage message) {
        final boolean enabled = !message.getNotMetConditions().contains(ConditionType.ENABLED);
        final boolean charger = !message.getNotMetConditions().contains(ConditionType.CHARGER);
        final boolean battery = !message.getNotMetConditions().contains(ConditionType.BATTERY);
        final boolean wifi = !message.getNotMetConditions().contains(ConditionType.WIFI);
        final boolean paused = message.getNotMetConditions().contains(ConditionType.PAUSED);
        ViewUtils.updateStatusBar(mStatusView, enabled, paused, battery, charger, wifi);

        if (!enabled && JobCheckpointsContract.get24HourAccumulatedTime() > 0) {
            NotificationHelper.showNotification(NotificationStatus.STATUS_FINISHED);
        }
        setupConditionsLayout(message);
    }

    /**
     * Setup the not met conditions pager.
     * @param message the message fired when the conditions changes.
     */
    private void setupConditionsLayout(final ConditionMessage message) {
        final boolean charger = !message.getNotMetConditions().contains(ConditionType.CHARGER);
        final boolean battery = !message.getNotMetConditions().contains(ConditionType.BATTERY);
        final boolean wifi = !message.getNotMetConditions().contains(ConditionType.WIFI);
        final boolean allConditionsMet = charger && battery && wifi;
        if (allConditionsMet) {
            mConditionsLayout.setVisibility(View.INVISIBLE);
            mBackgroundImage.setAlpha(1F);
        } else {
            mConditionsLayout.setVisibility(View.VISIBLE);
            mBackgroundImage.setAlpha(BACKGROUND_IMAGE_ALPHA);

            if (mAdapter == null) {
                mAdapter = new ConditionsSlidePagerAdapter(getSupportFragmentManager());
                mConditionsViewPager.setAdapter(mAdapter);

                mConditionsViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(final int position,
                                               final float positionOffset,
                                               final int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(final int position) {
                        //updates the indicator selection
                        for (int i = 0; i < mConditionsIndicator.getChildCount(); i++) {
                            mConditionsIndicator.getChildAt(i).setSelected(i == position);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(final int state) {

                    }
                });
            }

            // Creates a new list because we're only interested in 3 conditions:
            // charger, battery, wifi.
            final List<ConditionType> notMetList = new ArrayList<>();
            if (!charger) {
                notMetList.add(ConditionType.CHARGER);
            }
            if (!battery) {
                notMetList.add(ConditionType.BATTERY);
            }
            if (!wifi) {
                notMetList.add(ConditionType.WIFI);
            }
            mAdapter.setConditionsNotMetList(notMetList);

            //adding the indicators and selecting the first one
            mConditionsIndicator.removeAllViews();
            if (notMetList.size() > 1) {
                for (int i = 0; i < notMetList.size(); i++) {
                    View.inflate(this, R.layout.view_pager_indicator, mConditionsIndicator);
                }
                mConditionsIndicator.getChildAt(0).setSelected(true);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(final JobExecutionMessage message) {
        loadJobsStats(message.getNumberOfUsers(), message.getTitle(), message.getContributedTime());
        //call invalidateOptionsMenu to check if the research's detail
        //is available and show a new icon in the menu to open the detail
        invalidateOptionsMenu();
    }
}
