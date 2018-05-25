package com.msht.watersystem.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.functionView.SplashActivity;

/**
 * Created by hong on 2017/10/28.
 */

public class RestartAppUtil {
   public static void restartApp(Context mContext){
       ((AppContext)mContext.getApplicationContext()).removeAllActivity();
       Intent intent=new Intent(mContext.getApplicationContext(), SplashActivity.class);
       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       mContext.startActivity(intent);
       PendingIntent restartIntent = PendingIntent.getActivity(
               mContext.getApplicationContext(), 0, intent, 0);
       //退出程序
       AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
       mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 600,
               restartIntent); // 1秒钟后重启应用
       ((AppContext)mContext.getApplicationContext()).removeAllActivity();
    }
}
