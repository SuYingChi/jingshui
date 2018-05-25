package com.msht.watersystem.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.utils.ServicesUtils;
import com.mcloyal.serialport.utils.logs.LogUtils;
import com.msht.watersystem.functionView.SplashActivity;

/**
 * 程序自启动服务
 * 作者：huangzhong on 2016/7/15 09:21
 * 邮箱：mcloyal@163.com
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent newIntent = new Intent(context, SplashActivity.class);
            LogUtils.d("BootReceiver", "自启动程序");
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //注意，必须添加这个标记，否则启动会失败
            context.startActivity(newIntent);
        }
        if (action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            AppLibsContext appLibsContext = (AppLibsContext) context.getApplicationContext();
            ServicesUtils.startPortServices(appLibsContext, null);
        }
    }
}
