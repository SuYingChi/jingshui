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
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    @SuppressLint("NewApi")
    public static boolean isNotificationEnabled(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //8.0手机以上
            if (((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class appOpsClass = null;

        /* Context.APP_OPS_MANAGER */
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod =
                    appOpsClass.getMethod(CHECK_OP_NO_THROW,
                            Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (Integer) opPostNotificationValue.get(Integer.class);

            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) ==
                    AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    /**
     * 判断应用是否已经启动
     *
     * @param context     一个context
     * @return boolean
     */
    public static boolean isAppAlive(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfo
                = activityManager.getRunningAppProcesses();
        for (int i = 0; i < processInfo.size(); i++) {
            if (processInfo.get(i).processName.equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获得屏幕的宽度
     *
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }
}
