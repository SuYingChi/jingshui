package com.msht.watersystem.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public class KillSelfService extends Service {

    /**
     * 关闭应用后多久重新启动
     *
     */
   // private String mPackageName;
    private Handler handler;
    public KillSelfService() {
        handler = new Handler();
    }
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        long stopDelayed = intent.getLongExtra("Delayed", 600);
        final String mPackageName = intent.getStringExtra("PackageName");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(mPackageName);
                startActivity(launchIntent);
                KillSelfService.this.stopSelf();
            }
        }, stopDelayed);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}