package com.msht.watersystem.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.functionActivity.SplashActivity;
import com.msht.watersystem.service.KillSelfService;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author hong
 * @date 2017/10/28
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
               pendingIntent);
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

    /**
     * 重启整个APP
     * @param context 上下文
     * @param delayed 延迟多少毫秒
     */
    public static void restartWaterSystem(Context context,long delayed){

        /**开启一个新的服务，用来重启本APP*/
        Intent killSelfServiceIntent=new Intent(context,KillSelfService.class);
        killSelfServiceIntent.putExtra("PackageName",context.getPackageName());
        killSelfServiceIntent.putExtra("Delayed",delayed);
        context.startService(killSelfServiceIntent);
        /**杀死整个进程**/
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    /***重启整个APP*/
    public static void restartWaterSystem(Context context){
        restartWaterSystem(context,600);
    }


    public static void restartWaterApp(Context mContext){
        final Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

}
