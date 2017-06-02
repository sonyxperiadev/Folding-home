/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")

/**
 * This is a utility class and we will probably need these methods later.
 */
public final class JSONUtils {

    /**
     * Deafult date format.
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    /**
     * Flag to print exceptions.
     */
    private static final boolean PRINT_EXCEPTIONS = false;

    /**
     * This class is not intended to be instantiated.
     */
    private JSONUtils() { }

    /**
     * Parsing response to json array.
     * @param response the response
     * @return the json array
     */
    public static JSONArray parseJSONArray(final byte[] response) {
        try {
            return new JSONArray(new String(response, "UTF-8"));
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONArray();
        }
    }

    /**
     * Parsing json string to json array.
     * @param json the json
     * @return the json array
     */
    public static JSONArray parseJSONArray(final String json) {
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONArray();
        }
    }

    /**
     * Parsing response to json object.
     * @param response the response
     * @return the json object
     */
    public static JSONObject parseJSONObject(final byte[] response) {
        try {
            return new JSONObject(new String(response, "UTF-8"));
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONObject();
        }
    }

    /**
     * Parsing json string to json object.
     * @param json the json
     * @return the json object
     */
    public static JSONObject parseJSONObject(final String json) {
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONObject();
        }
    }

    /**
     * Gets json array from the json object.
     * @param jobj the json object
     * @param key the key
     * @return the json array
     */
    public static JSONArray getJSONArray(final JSONObject jobj, final String key) {
        try {
            return jobj.getJSONArray(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONArray();
        }
    }

    /**
     * Gets int value from the json object.
     * @param jobj the json object
     * @param key the key
     * @param defaultValue the default value
     * @return the int value
     */
    public static int getInt(final JSONObject jobj, final String key, final int defaultValue) {
        try {
            return jobj.getInt(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return defaultValue;
        }
    }

    /**
     * Gets boolean value from the json object.
     * @param jobj the json object
     * @param key the key
     * @param defaultValue the default value
     * @return the boolean value
     */
    public static boolean getBoolean(final JSONObject jobj, final String key,
                                     final boolean defaultValue) {
        try {
            return jobj.getBoolean(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return defaultValue;
        }
    }

    /**
     * Gets long value from the json object.
     * @param jobj the json object
     * @param key the key
     * @param defaultValue the default value
     * @return the long value
     */
    public static long getLong(final JSONObject jobj, final String key, final long defaultValue) {
        try {
            return jobj.getLong(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return defaultValue;
        }
    }

    /**
     * Gets double value from the json object.
     * @param jobj the json object
     * @param key the key
     * @param defaultValue the default value
     * @return the double value
     */
    public static double getDouble(final JSONObject jobj, final String key,
                                   final double defaultValue) {
        try {
            return jobj.getDouble(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return defaultValue;
        }
    }

    /**
     * Gets string value from the json object.
     * @param jobj the json object
     * @param key the key
     * @param defaultValue the default value
     * @return the string value
     */
    public static String getString(final JSONObject jobj, final String key,
                                   final String defaultValue) {
        try {
            String str = jobj.getString(key);
            if (str.equalsIgnoreCase("null")) {
                return defaultValue;
            }
            return str;
        } catch (Exception e2) {
            if (PRINT_EXCEPTIONS) {
                e2.printStackTrace();
            }
            return defaultValue;
        }

    }

    /**
     * Gets list of string from the json object.
     * @param jobj the json object
     * @param key the key
     * @return the list of strings
     */
    public static List<String> getStringArray(final JSONObject jobj, final String key) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray jarr = jobj.getJSONArray(key);
            for (int i = 0; i < jarr.length(); i++) {
                list.add(jarr.getString(i));
            }

        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * Gets date value from the json object.
     * @param jobj the json object
     * @param key the key
     * @return the date value
     */
    public static Date getDate(final JSONObject jobj, final String key) {
        return getDate(jobj, key, DEFAULT_DATE_FORMAT);
    }

    /**
     * Gets formatted date from the json object.
     * @param jobj the json object
     * @param key the key
     * @param format the format
     * @return the date
     */
    public static Date getDate(final JSONObject jobj, final String key, final String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(jobj.getString(key));
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new Date(0L);
        }
    }

    /**
     * Gets int array from the json object.
     * @param jobj the json object
     * @param key the key
     * @return the int array
     */
    public static int[] getIntArray(final JSONObject jobj, final String key) {
        try {
            JSONArray jarr = jobj.getJSONArray(key);
            int[] intArr = new int[jarr.length()];
            for (int i = 0; i < jarr.length(); i++) {
                intArr[i] = jarr.getInt(i);
            }
            return intArr;
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new int[0];
        }
    }

    /**
     * Gets json object from the json object.
     * @param jobj the json object
     * @param key the key
     * @return the json object
     */
    public static JSONObject getJSONObject(final JSONObject jobj, final String key) {
        try {
            return jobj.getJSONObject(key);
        } catch (Exception e) {
            if (PRINT_EXCEPTIONS) {
                e.printStackTrace();
            }
            return new JSONObject();
        }
    }
}
