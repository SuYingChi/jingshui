package com.msht.watersystem.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Created by hong on 2018/1/12.
 */

public class CachePreferencesUtil {
    private static final String SP_FIST ="open_app";
    private static final String SP_FILE_NAME = "AppData";
    public static final String FIRST_OPEN = "first_open";
    public static final String VOLUME ="volume";
    public static final String OUT_WATER_TIME ="time";
    public static final String CHARGEMODE ="chargemode";
    public static final String SHOWTDS ="showtds";

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

    public static boolean savebitmapToShareprefence(Context context, String strKey, Bitmap bitmap) {
       // Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        SharedPreferences activityPreferences = context.getSharedPreferences(SP_FILE_NAME,
                Context.MODE_PRIVATE);
        paraCheck(activityPreferences,strKey);
        if (bitmap == null || bitmap.isRecycled()){
            return false;
        }else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            SharedPreferences.Editor editor = activityPreferences.edit();
            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            editor.putString(strKey,imageBase64 );
            return editor.commit();
        }
    }

    private static void paraCheck(SharedPreferences sp, String key) {
        if (sp == null) {
            throw new IllegalArgumentException();
        }
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException();
        }
    }
    public static void clear(Context context, String strKey){
        SharedPreferences activityPreferences= context.getSharedPreferences(strKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=activityPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
