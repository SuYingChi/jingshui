package com.msht.watersystem.utilpackage;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;


import com.msht.watersystem.AppContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;



/**
 * 工具类
 *
 * @author hong
 * @date 2018/06/05
 */
public class AppPackageUtil {
    public static String getPackageVersionName() {
       String versionName="" ;
        try {
            PackageManager pm = AppContext.getWaterApplicationContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(AppContext.getWaterApplicationContext().getPackageName(), 0);
            versionName=pi.versionName;
        }catch (Exception e){
            e.printStackTrace();
        }
        return versionName;
    }

    public static int getPackageVersionCode(){
        try {
            PackageInfo pi = AppContext.getWaterApplicationContext().getPackageManager().getPackageInfo(AppContext.getWaterApplicationContext().getPackageName(), PackageManager.GET_CONFIGURATIONS);
                return pi.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        return 1;
    }
}
