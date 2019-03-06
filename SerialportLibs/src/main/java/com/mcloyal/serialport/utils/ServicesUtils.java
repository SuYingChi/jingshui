package com.mcloyal.serialport.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.service.PortService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * 邮箱：mcloyal@163.com
 */
public class ServicesUtils {
    private final static String PORT_SERVICES_CLS = PortService.class.getName();
    
    /**
     * 启动串口监听服务
     *
     * @param context
     */
    public static void startPortServices(AppLibsContext context, ComServiceConnection.ConnectionCallBack connectionCallBack) {
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            String infoClassName = serviceInfo.service.getClassName();
            if (TextUtils.equals(PORT_SERVICES_CLS, infoClassName)) {
                isServiceRunning = true;
            }
        }
        if (!isServiceRunning) {//服务还未启动
            ComServiceConnection serviceConnection = new ComServiceConnection(context, connectionCallBack);
            context.bindService(new Intent(context, PortService.class), serviceConnection,
                    BIND_AUTO_CREATE);
        }
    }
}
