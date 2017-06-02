/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.assets;

import android.content.Context;

import com.sonymobile.androidapp.gridcomputing.log.Log;
import com.sonymobile.androidapp.gridcomputing.utils.ApplicationData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;


/**
 * Helper class which asynchronously copies the runtime data for script
 * execution to a private mutable location.
 */
public final class CopyAssets implements Runnable {

    /**
     * Execution file directory path.
     */
    public static final String EXEC_DIR = "execdir";

    /**
     * Openmm plugins dir namee.
     */
    public static final String LIB_OPENMM_PLUGIN_DIR = "plugins";

    public static final String GCOMP = "libgcomp_node.so";
    public static final String CLIENT_JS_FILE = "client.js";


    private static final String[] ASSETS_FILES = {
            "fsm.js", "environment.js", "consts_client.js", "script_prepend.js", CLIENT_JS_FILE
    };

    /**
     * The OpenMM Plugin Libraries.
     */
    private static final String[] LIB_OPENMM_PLUGIN_FILES = {
            "libOpenMMPME.so", "libOpenMMRPMDReference.so", "libOpenMMDrudeReference.so"
    };
    /**
     * Size of the buffer to use when copying files.
     */
    private static final int BUFFER_SIZE = 65535;
    /**
     * A context used to read from the assets and create a private dir.
     */
    private final Context mContext;
    /**
     * A callback that will be called when the assets are copied.
     */
    private final AssetCopyListener mListener;

    /**
     * Constructs a CopyAssets.
     *
     * @param context  Execution context.
     * @param listener the callback that will be called when the assets are
     *                 copied.
     */
    public CopyAssets(final Context context, final AssetCopyListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    /**
     * Checks if all files were copied.
     *
     * @return true if all files were copied.
     */
    public static boolean filesCopied() {
        final Context context = ApplicationData.getAppContext();
        final File dir = context.getFilesDir();
        final File execDir = new File(dir, EXEC_DIR);
        final File pluginsDir = new File(execDir, LIB_OPENMM_PLUGIN_DIR);
        if (execDir.exists() && execDir.isDirectory()
                && pluginsDir.exists() && pluginsDir.isDirectory()) {
            final String[] fileNames = execDir.list();
            if (fileNames != null) {
                final List<String> execFiles = Arrays.asList(fileNames);
                for (String file : ASSETS_FILES) {
                    if (!execFiles.contains(file)) {
                        return false;
                    }
                }
            }

            final String[] pluginFileNames = pluginsDir.list();
            if (pluginFileNames != null) {
                final List<String> pluginFiles = Arrays.asList(pluginFileNames);
                for (String file : LIB_OPENMM_PLUGIN_FILES) {
                    if (!pluginFiles.contains(file)) {
                        return false;
                    }
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Deletes all files from exec dir.
     */
    public static void deleteFiles() {
        final Context context = ApplicationData.getAppContext();
        final File execDir = context.getDir(EXEC_DIR, Context.MODE_PRIVATE);
        deleteFilesRecursive(execDir);
    }

    /**
     * Deletes a file recursively.
     *
     * @param fileOrDirectory the file to delete.
     */
    private static void deleteFilesRecursive(final File fileOrDirectory) {
        final File[] filesList = fileOrDirectory.listFiles();
        if (filesList != null) {
            for (File child : filesList) {
                deleteFilesRecursive(child);
            }
        }
        final boolean delete = fileOrDirectory.delete();
        if (!delete) {
            Log.d("failed to delete files!");
        }
    }

    /**
     * Copies a file from assets to a private directory (maintaining the name)
     * if the file is not required, ignore it in case of Exception.
     *
     * @param filename the name of the file from the assets.
     * @param optional flag indicating if this file is optional or required.
     * @throws IOException in case any IOException occurs when copying the file.
     */
    private void copyAssetsFile(final String filename, final boolean optional) throws IOException {
        try {
            final InputStream inputStream = mContext.getAssets().open(filename);
            copyFile(inputStream, filename, false);
        } catch (IOException exception) {
            if (!optional) {
                throw exception;
            }
        }
    }

    /**
     * Copies a native lib file to a private directory and sets the file to be
     * executable (or not).
     *
     * @param libName    the name of the file from the lib folder.
     * @param outputFile the name of the output file.
     * @param exec       flag indicating if this file is executable or not.
     * @throws IOException in case any IOException occurs when copying the file.
     */
    private void copyLibFile(final String libName, final String outputFile, final boolean exec)
            throws IOException {
        final InputStream inputStream = getInputStreamFromNativeLib(libName);
        copyFile(inputStream, outputFile, exec);
    }

    /**
     * Copies a file from assets to a private directory and sets the file to be
     * executable (or not).
     *
     * @param fileInputStream the input stream of the file.
     * @param outputFile      the name of the output file.
     * @param exec            flag indicating if this file is executable or not.
     * @throws IOException in case any IOException occurs when copying the file.
     */
    private void copyFile(final InputStream fileInputStream, final String outputFile,
                          final boolean exec)
            throws IOException {
        File file = null;
        File directory = null;
        OutputStream out = null;
        try {
            file = new File(mContext.getDir(EXEC_DIR,
                    Context.MODE_PRIVATE), outputFile);

            // Create directories if needed
            directory = file.getParentFile();
            if (directory == null) {
                throw new IOException("Could not create directory.");
            } else if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create directory.");
            }

            out = new FileOutputStream(file);

            final byte[] byteArray = new byte[BUFFER_SIZE];
            int size = 0;
            while ((size = fileInputStream.read(byteArray)) > 0) {
                out.write(byteArray, 0, size);
            }
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (final IOException eIn) {
                if (out != null) {
                    out.close();
                }
                throw eIn;
            }

            if (out != null) {
                out.close();
            }
            if (file == null || !file.setExecutable(exec, true)) {
                throw new IOException("set executable failure");
            }
        }
    }

    /**
     * Gets the input stream from a file inside the native library dir.
     *
     * @param libName the name of the lib.
     * @return the input stream.
     * @throws FileNotFoundException if the specified file doesn't exists.
     */
    private InputStream getInputStreamFromNativeLib(final String libName)
            throws FileNotFoundException {
        final String nativeLibraryDir = ApplicationData.getAppContext()
                .getApplicationInfo().nativeLibraryDir;
        final File libraryFolder = new File(nativeLibraryDir);
        final File[] files = libraryFolder.listFiles();
        if (files != null) {
            for (final File curr : files) {
                if (curr.isFile() && curr.getName().contains(libName)) {
                    return new FileInputStream(curr);
                }
            }
        }
        throw new FileNotFoundException(
                "The specified file doesn't exists inside the native library dir: " + libName);
    }

    @Override
    public void run() {
        try {
            for (String file : ASSETS_FILES) {
                copyAssetsFile(file, false);
            }
            for (String file : LIB_OPENMM_PLUGIN_FILES) {
                copyLibFile(file, LIB_OPENMM_PLUGIN_DIR + File.separator + file, false);
            }
            mListener.onAssetCopySuccess();
        } catch (final IOException exception) {
            Log.e(exception.getLocalizedMessage());
            exception.printStackTrace();
            mListener.onAssetCopyError();
        }
    }
}
