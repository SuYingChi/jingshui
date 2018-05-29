package com.msht.watersystem.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;


public class KillSelfService extends Service {

    /**
     * 关闭应用后多久重新启动
     */
    private static long stopDelayed = 600;
    private Handler handler;
    private String PackageName;

    public KillSelfService() {
        handler = new Handler();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        stopDelayed = intent.getLongExtra("Delayed", 600);
        PackageName = intent.getStringExtra("PackageName");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(PackageName);
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