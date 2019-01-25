package com.msht.watersystem.manager;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;


/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public class MyAppManage {

    public static void clearMyAppActivityMemory(Context context){
        if (context!=null){
            ActivityManager activityManager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager!=null){
                List<ActivityManager.RunningAppProcessInfo> appProcessInfoList=activityManager.getRunningAppProcesses();
                if (appProcessInfoList!=null){
                    for (int i=0;i<appProcessInfoList.size();i++){
                        ActivityManager.RunningAppProcessInfo appProcessInfo=appProcessInfoList.get(i);
                        String[] pkgList=appProcessInfo.pkgList;
                        if (appProcessInfo.importance>ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE){
                            for (String aPkgList : pkgList) {
                                activityManager.killBackgroundProcesses(aPkgList);
                            }
                        }
                    }
                }

            }
        }
    }

}
