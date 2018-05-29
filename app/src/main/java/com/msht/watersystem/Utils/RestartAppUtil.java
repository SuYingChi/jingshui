package com.msht.watersystem.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.functionView.SplashActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by hong on 2017/10/28.
 */

public class RestartAppUtil {

   public static void restartApp(Context mContext){
       ((AppContext)mContext.getApplicationContext()).removeAllActivity();
       Intent restartIntent =new Intent(mContext.getApplicationContext(), SplashActivity.class);
       restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       mContext.startActivity(restartIntent);
       PendingIntent pendingIntent = PendingIntent.getActivity(
               mContext.getApplicationContext(), 0, restartIntent, 0);
       //退出程序
       AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
       mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 600,
               pendingIntent); // 6m秒钟后重启应用
      ((AppContext) mContext.getApplicationContext()).KillProcess();
    }
    public static void  restartSystem(){
        try {
            Log.w("super", "开始重启android系统");
            Process localProcess = Runtime.getRuntime().exec("su");
            OutputStream localOutputStream = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(localOutputStream);
            String str = null;
            if (android.os.Build.MODEL.equals("SABRESD-MX6DQ")) {
                str = "busybox reboot -f\n";
            } else {
                str = "reboot\n";
            }
            localDataOutputStream.writeBytes(str);
            localDataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
