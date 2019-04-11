package com.msht.watersystem.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.utils.ServicesUtils;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.utilpackage.DateTimeUtils;
import com.msht.watersystem.utilpackage.MyServiceUtil;
import com.msht.watersystem.utilpackage.RestartAppUtil;


/**
 * 利用的系统广播是Intent.ACTION_TIME_TICK，这个广播每分钟发送一次，我们可以每分钟检查一次Service的运行状态，如果已经被结束了，
 * 就重新启动Service
 */
public class PortReceiver extends BroadcastReceiver {

    private int hour1=3,hour2=3;
    private int minute1=9,minute2=9;
    private int minute3=10;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            //一分钟检测一次状态>>>>>>>>>>>
            AppLibsContext appLibsContext = (AppLibsContext) context.getApplicationContext();
            ServicesUtils.startPortServices(appLibsContext, null);
            MyServiceUtil.startResendDataServices(AppContext.getInstance());
        }

    }
}
