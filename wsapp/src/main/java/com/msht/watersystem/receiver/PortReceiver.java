package com.msht.watersystem.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.utils.ServicesUtils;
import com.mcloyal.serialport.utils.logs.LogUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.RestartAppUtil;


/**
 * 利用的系统广播是Intent.ACTION_TIME_TICK，这个广播每分钟发送一次，我们可以每分钟检查一次Service的运行状态，如果已经被结束了，
 * 就重新启动Service
 */
public class PortReceiver extends BroadcastReceiver {
    private int hour1=12,hour2=12;
    private int minute1=10,minute2=11;
    private final static String TAG = PortReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            //一分钟检测一次状态>>>>>>>>>>>
            AppLibsContext appLibsContext = (AppLibsContext) context.getApplicationContext();
            ServicesUtils.startPortServices(appLibsContext, null);
            /*if (DateTimeUtils.isCheckTime(hour1,hour2,minute1,minute2)){
                RestartAppUtil.restartApp(context);
            }*/
        }
    }
}
