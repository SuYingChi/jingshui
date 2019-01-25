package com.msht.watersystem.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.Interface.ResendDataEvent;
import com.msht.watersystem.utilpackage.DateTimeUtils;
import com.msht.watersystem.utilpackage.RestartAppUtil;

/**
 * Created by hong on 2018/4/13.
 */

public class TimeBroadcastReceiver extends BroadcastReceiver {
    private int hour1=0,hour2=0;
    private int minute1=0,minute2=30;
    private int minute3=31;
    public ResendDataEvent events = BaseActivity.event;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            boolean TimeFlag= DateTimeUtils.isCheckTime(hour1,hour2,minute1,minute2);
            if (DateTimeUtils.isCheckTime(hour1,hour2,minute2,minute3)){
                RestartAppUtil.restartApp(context);
            }
        }
    }
}
