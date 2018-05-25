package com.msht.watersystem.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by hong on 2018/1/12.
 */

public class CachePreferencesUtil {
    private static final String spFist="open_app";
    private static final String spFileName = "AppData";
    public static final String FIRST_OPEN = "first_open";
    public static final String Volume="volume";
    public static final String outWaterTime="time";
    public static final String ChargeMode="chargemode";
    public static final String ShowTds="showtds";
    public static final String BitMap="bitmap";

    public static Boolean getBoolean(Context context, String strKey,
                                     Boolean strDefault) {//strDefault  boolean: Value to return if this preference does not exist.
        SharedPreferences setPreferences = context.getSharedPreferences(
                spFist, Context.MODE_PRIVATE);
        Boolean result = setPreferences.getBoolean(strKey, strDefault);
        return result;
    }
    public static void putBoolean(Context context, String strKey,
                                  Boolean strData) {
        SharedPreferences activityPreferences = context.getSharedPreferences(
                spFist, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putBoolean(strKey, strData);
        editor.commit();
    }

    public static int getChargeMode(Context context,String strKey,int strDefault){
        SharedPreferences setPreferences = context.getSharedPreferences(
                spFileName, Context.MODE_PRIVATE);
        int result = setPreferences.getInt(strKey, strDefault);
        return result;
    }
    public static void putChargeMode(Context context, String strKey, int strData){
            SharedPreferences activityPreferences = context.getSharedPreferences(
                    spFileName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = activityPreferences.edit();
            editor.putInt(strKey, strData);
            editor.commit();
    }
    public static String getStringData(Context context, String strKey,
                                       String strDefault) {//strDefault  boolean: Value to return if this preference does not exist.
        SharedPreferences setPreferences = context.getSharedPreferences(
                spFileName, Context.MODE_PRIVATE);
        String result = setPreferences.getString(strKey, strDefault);
        return result;
    }
    public static void putStringData(Context context, String strKey,
                                     String strData) {
        SharedPreferences activityPreferences = context.getSharedPreferences(
                spFileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        editor.putString(strKey, strData);
        editor.commit();
    }

    public static boolean saveBitMap(Context context,String strKey,Bitmap bitmap) {
       // Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        SharedPreferences activityPreferences = context.getSharedPreferences(spFileName,
                Context.MODE_PRIVATE);
        paraCheck(activityPreferences,strKey);
        if (bitmap == null || bitmap.isRecycled()){
            return false;
        }else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            SharedPreferences.Editor editor = activityPreferences.edit();
            String imageBase64 = new String(Base64.encodeToString(baos.toByteArray(),Base64.DEFAULT));
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
    public static void Clear(Context context, String strKey){
        SharedPreferences activityPreferences= context.getSharedPreferences(strKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=activityPreferences.edit();
        editor.clear();
        editor.commit();
    }
}
