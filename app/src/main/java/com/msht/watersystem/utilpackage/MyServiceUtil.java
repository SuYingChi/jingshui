package com.msht.watersystem.utilpackage;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.service.ResendDataService;

import static android.content.Context.BIND_AUTO_CREATE;
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2019/4/9 
 */
public class MyServiceUtil {

    private final static String PORT_SERVICES_CLS =ResendDataService.class.getName();

    /**
     * 启动串口监听服务
     * @param waterApplicationContext
     */
    public static void startResendDataServices(Context waterApplicationContext) {
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) waterApplicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            String infoClassName = serviceInfo.service.getClassName();
            if (TextUtils.equals(PORT_SERVICES_CLS, infoClassName)) {
                isServiceRunning = true;
            }
        }
        //服务还未启动
        if (!isServiceRunning) {
            /**开启一个新的服务，*/
            Intent resendDataService=new Intent(waterApplicationContext,ResendDataService.class);
            waterApplicationContext.startService(resendDataService);
        }
    }
}
