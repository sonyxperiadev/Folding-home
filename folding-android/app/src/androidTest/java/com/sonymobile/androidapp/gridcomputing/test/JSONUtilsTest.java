/*
 * Licensed under the LICENSE.
 * Copyright 2017, Sony Mobile Communications Inc.
 */

package com.sonymobile.androidapp.gridcomputing.test;

import android.support.test.runner.AndroidJUnit4;

import com.sonymobile.androidapp.gridcomputing.utils.JSONUtils;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class JSONUtilsTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty("Env", "JUnit");
    }

    @Test
    public void testJSONPref() throws Exception {
        final String jsonStr = "{\"key1\":\"value1\",\"obj1\":{\"sub_key1\":\"sub_value1\"},\"array1\":[1,2,3],\"array2\":[\"text1\",\"text2\",\"text3\"],\"array3\":[{\"sub_key2\":\"sub_value2\"},{\"sub_key2\":\"sub_value2\"}],\"key2\":99,\"key3\":false,\"key4\":123456789,\"key5\":4.5,\"key6\":\"2015-09-30T18:46:19-0300\",\"key7\":null}";
        final String jsonArray = "[" + jsonStr + "," + jsonStr + "," + jsonStr + "]";

        final byte[] bytes = jsonStr.getBytes("UTF-8");
        final byte[] jsonArrayBytes = jsonArray.getBytes("UTF-8");

        final JSONObject mainObj = JSONUtils.parseJSONObject(jsonStr);
        JSONObject subObj = JSONUtils.getJSONObject(mainObj, "obj1");

        assertEquals(jsonStr, JSONUtils.parseJSONObject(jsonStr).toString().replace(" ", ""));
        assertEquals(jsonStr, JSONUtils.parseJSONObject(bytes).toString().replace(" ", ""));
        assertEquals("{}", JSONUtils.parseJSONObject(new byte[0]).toString().replace(" ", ""));
        assertEquals("{}", JSONUtils.parseJSONObject("").toString().replace(" ", ""));

        assertEquals(3, JSONUtils.parseJSONArray(jsonArray).length());
        assertEquals(3, JSONUtils.parseJSONArray(jsonArrayBytes).length());
        assertEquals(0, JSONUtils.parseJSONArray("").length());
        assertEquals(0, JSONUtils.parseJSONArray(new byte[0]).length());

        assertEquals(mainObj.toString(), JSONUtils.parseJSONArray(jsonArray).get(0).toString());
        assertEquals(mainObj.toString(), JSONUtils.parseJSONArray(jsonArray).get(1).toString());
        assertEquals(mainObj.toString(), JSONUtils.parseJSONArray(jsonArray).get(2).toString());

        assertEquals("value1", mainObj.getString("key1"));

        assertEquals(2, JSONUtils.getJSONArray(mainObj, "array3").length());
        assertEquals(0, JSONUtils.getJSONArray(mainObj, "no_key").length());

        assertEquals(1, JSONUtils.getIntArray(mainObj, "array1")[0]);
        assertEquals(2, JSONUtils.getIntArray(mainObj, "array1")[1]);
        assertEquals(3, JSONUtils.getIntArray(mainObj, "array1")[2]);
        assertEquals(0, JSONUtils.getIntArray(mainObj, "no_key").length);

        assertEquals("text1", JSONUtils.getStringArray(mainObj, "array2").get(0));
        assertEquals("text2", JSONUtils.getStringArray(mainObj, "array2").get(1));
        assertEquals("text3", JSONUtils.getStringArray(mainObj, "array2").get(2));
        assertEquals(0, JSONUtils.getStringArray(mainObj, "no_key").size());

        assertEquals("{\"sub_key1\":\"sub_value1\"}", subObj.toString().replace(" ", ""));

        assertEquals("sub_value1", JSONUtils.getString(subObj, "sub_key1", ""));

        assertEquals("defaulttext", JSONUtils.getString(mainObj, "no_key", "defaulttext"));
        assertEquals("defaulttext", JSONUtils.getString(mainObj, "key7", "defaulttext"));

        assertEquals(99, JSONUtils.getInt(mainObj, "key2", 0));
        assertEquals(0, JSONUtils.getInt(mainObj, "no_key", 0));

        Assert.assertFalse(JSONUtils.getBoolean(mainObj, "key3", true));
        Assert.assertTrue(JSONUtils.getBoolean(mainObj, "no_key", true));

        assertEquals(123456789, JSONUtils.getLong(mainObj, "key4", 0L));
        assertEquals(15L, JSONUtils.getLong(mainObj, "no_key", 15L));

        Assert.assertTrue(4.5 == JSONUtils.getDouble(mainObj, "key5", 1.0));
        Assert.assertTrue(2.0 == JSONUtils.getDouble(mainObj, "no_key", 2.0f));

        //2015-09-30T18:46:19-0300
        final Date jsonDate = JSONUtils.getDate(mainObj, "key6");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-3"));
        calendar.setTime(jsonDate);
        assertEquals(2015, calendar.get(Calendar.YEAR));
        //JANURAY is 0, so september (9th month of year) is 8
        assertEquals(9 - 1, calendar.get(Calendar.MONTH));
        assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(18, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(46, calendar.get(Calendar.MINUTE));
        assertEquals(19, calendar.get(Calendar.SECOND));

        assertEquals(0L, JSONUtils.getDate(mainObj, "no_key").getTime());
    }
}
