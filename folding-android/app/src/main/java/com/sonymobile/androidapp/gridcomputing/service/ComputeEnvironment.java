/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.service;

import android.content.Context;
import android.os.PowerManager;

import com.sonymobile.androidapp.gridcomputing.assets.CopyAssets;
import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.utils.JSONUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

/**
 * Class responsible for executing the node.
 */
public class ComputeEnvironment {

    /**
     * LD library path.
     */
    private static final String LD_LIBRARY_PATH = "LD_LIBRARY_PATH";
    /**
     * Encode.
     */
    private static final String CHARSET = "UTF-8";

    /**
     * Context.
     */
    private final Context mContext;
    /**
     * Job execution listener.
     */
    private final JobExecutionListener mJobExecutionListener;
    /**
     * Wake lock.
     */
    private final PowerManager.WakeLock mActiveLock;
    /**
     * Process.
     */
    private Process mExecProccess;

    /**
     * The class constructor.
     *
     * @param context  the context.
     * @param listener the listener.
     */
    public ComputeEnvironment(final Context context,
                              final JobExecutionListener listener) {
        mContext = context;
        mJobExecutionListener = listener;
        mActiveLock = ((PowerManager) context
                .getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "compute-running");
    }

    /**
     * Starts the process.
     *
     * @return the process.
     * @throws IOException the io exception.
     */
    private Process startProcess() throws IOException {
        ProcessBuilder processBuilder;
        final File dir = mContext.getDir(CopyAssets.EXEC_DIR, Context.MODE_PRIVATE);
        final String gcompExecDir = mContext.getApplicationInfo().nativeLibraryDir;
        final String gcompExecFile = gcompExecDir + "/" + CopyAssets.GCOMP;
        processBuilder = new ProcessBuilder(gcompExecFile, CopyAssets.CLIENT_JS_FILE);

        processBuilder.directory(dir);
        processBuilder.redirectErrorStream(true);


        final String ldLibrary = processBuilder.environment().get(LD_LIBRARY_PATH);
        processBuilder.environment()
                .put(LD_LIBRARY_PATH, ldLibrary + ":" + gcompExecDir);

        return processBuilder.start();
    }

    /**
     * Executes the job.
     */
    public final void runJob() {
        final Thread jobThread = new RunJobThread();
        jobThread.start();
    }

    /**
     * Method invoked when conditions change.
     *
     * @param active   the active
     * @param hardStop the hard stop
     */
    public void conditionChanged(final boolean active, final boolean hardStop) {
        Log.d("conditionChanged active: " + active + " hardStop: " + hardStop);
        if (active) {
            resumeJob();
        } else {
            stopJob(hardStop);
        }
    }

    /**
     * Resumes the job.
     */
    private void resumeJob() {
        try {
            if (mExecProccess != null) {
                getProcessOutputStream().write(EnvironmentMessenger
                        .getJsonResumeJobClient().getBytes(CHARSET));
            }
        } catch (final IOException e) {
            Log.e(e.getLocalizedMessage());
        }
    }

    /**
     * Stops the job.
     *
     * @param hardStop the hard stop
     */
    private void stopJob(final boolean hardStop) {
        try {
            if (mExecProccess != null) {
                getProcessOutputStream().write(EnvironmentMessenger
                        .getJsonKillClient(hardStop).getBytes(CHARSET));
            }
        } catch (final IOException e) {
            Log.e(e.getLocalizedMessage());
        }
    }

    /**
     * Stops process.
     */
    private void stopProcess() {
        if (mExecProccess != null) {
            mExecProccess.destroy();
            mExecProccess = null;

            if (isActiveLockHeld()) {
                mActiveLock.release();
            }
            if (mJobExecutionListener != null) {
                mJobExecutionListener.clientStopped();
            }
        }
    }

    /**
     * Method that checks the wake lock.
     *
     * @return true if is held
     */
    public final boolean isActiveLockHeld() {
        return mActiveLock.isHeld();
    }

    /**
     * Gets buffered reader.
     *
     * @return the buffered reader
     * @throws UnsupportedEncodingException the encoding exception
     */
    protected BufferedReader getReader() throws UnsupportedEncodingException {
        return new BufferedReader(
                new InputStreamReader(mExecProccess.getInputStream(), CHARSET));
    }

    /**
     * Gets output stream.
     *
     * @return the output stream
     */
    protected OutputStream getProcessOutputStream() {
        return mExecProccess.getOutputStream();
    }

    /**
     * Runs job thread.
     */
    private class RunJobThread extends Thread {
        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                mExecProccess = startProcess();

                mActiveLock.acquire();
                reader = getReader();
                String str = "";

                // reads the inputstream from the gcomp_node process
                while ((str = reader.readLine()) != null) {
                    Log.d("Read from Client > " + str);
                    final JSONObject jsonObject = JSONUtils.parseJSONObject(str);
                    final String action = JSONUtils.getString(jsonObject, "action", "");
                    final JSONObject content = JSONUtils.getJSONObject(jsonObject, "content");

                    if ("no_job_available".equalsIgnoreCase(action)) {
                    } else if ("number_of_users".equalsIgnoreCase(action)) {
                        mJobExecutionListener.numberOfUsersReceived(JSONUtils
                                .getLong(content, "number_of_users", 0L));
                    } else if ("research_details".equalsIgnoreCase(action)) {
                        mJobExecutionListener.researchDetailsReceived(content);
                    } else if ("limit_storage".equalsIgnoreCase(action)) {
                    } else if ("get_key".equalsIgnoreCase(action)) {
                        getProcessOutputStream()
                                .write(EnvironmentMessenger.getJsonKeyReply().getBytes(CHARSET));
                    } else if ("job_received".equalsIgnoreCase(action)) {
                    } else if ("job_finished".equalsIgnoreCase(action)) {
                    } else if ("key_accepted".equalsIgnoreCase(action)) {
                    } else if ("job_execution_error".equalsIgnoreCase(action)) {
                        try {
                            final String error = content.getString("error");
                            if (error != null && !error.isEmpty()
                                    && !"undefined".equalsIgnoreCase(error)) {
                                // Ignore error.
                            }
                        } catch (final Exception exception) {
                            Log.e(exception.getLocalizedMessage());
                        }
                    } else if ("client_killed".equalsIgnoreCase(action)) {
                        break;
                    }

                }
            } catch (final IOException e) {
                Log.e(e.getLocalizedMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (final IOException e) {
                    Log.e(e.getLocalizedMessage());
                }
                stopProcess();
            }
        }

    }
}
