package com.msht.watersystem.utilpackage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 *
 * @author hong
 * @date 2018/1/12
 */

public class CachePreferencesUtil {
    private static final String SP_FIST ="open_app";
    private static final String SP_FILE_NAME = "AppData";
    public static final String FIRST_OPEN = "first_open";
    public static final String VOLUME ="volume";
    public static final String OUT_WATER_TIME ="time";
    public static final String CHARGE_MODE ="chargemode";
    public static final String SHOW_TDS ="showtds";
    public static final String WATER_NUM="waterNum";
    public static final String WATER_OUT_TIME="waterOutTime";
    public static final String DEDUCT_AMOUNT="DeductAmount";
    public static final String PRICE="price";
    public static Boolean getBoolean(Context context, String strKey,
                                     Boolean strDefault) {//strDefault  boolean: Value to return if this preference does not exist.
        SharedPreferences setPreferences = context.getSharedPreferences(
                SP_FIST, Context.MODE_PRIVATE);
        return setPreferences.getBoolean(strKey, strDefault);
    }
    public static void putBoolean(Context context, String strKey,
                                  Boolean strData) {
        SharedPreferences activityPreferences = context.getSharedPreferences(
                SP_FIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putBoolean(strKey, strData);
        editor.apply();
    }

    public static int getChargeMode(Context context,String strKey,int strDefault){
        SharedPreferences setPreferences = context.getSharedPreferences(
                SP_FILE_NAME, Context.MODE_PRIVATE);
        return setPreferences.getInt(strKey, strDefault);
    }
    public static void putChargeMode(Context context, String strKey, int strData){
            SharedPreferences activityPreferences = context.getSharedPreferences(
                    SP_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = activityPreferences.edit();
            editor.putInt(strKey, strData);
            editor.apply();
    }
    public static String getStringData(Context context, String strKey,
                                       String strDefault) {//strDefault  boolean: Value to return if this preference does not exist.
        SharedPreferences setPreferences = context.getSharedPreferences(
                SP_FILE_NAME, Context.MODE_PRIVATE);
        return setPreferences.getString(strKey, strDefault);

    }
    public static void putStringData(Context context, String strKey,
                                     String strData) {
        SharedPreferences activityPreferences = context.getSharedPreferences(
                SP_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putString(strKey, strData);
        editor.apply();
    }
    public static int getIntData(Context context, String strKey, int strDefault){
        SharedPreferences setPreferences = context.getSharedPreferences(
                SP_FILE_NAME, Context.MODE_PRIVATE);
        return setPreferences.getInt(strKey, strDefault);
    }
    public static void putIntData(Context context, String strKey, int strData){
        SharedPreferences activityPreferences = context.getSharedPreferences(
                SP_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putInt(strKey, strData);
        editor.apply();
    }
    public static void clear(Context context, String strKey){
        SharedPreferences activityPreferences= context.getSharedPreferences(strKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=activityPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
