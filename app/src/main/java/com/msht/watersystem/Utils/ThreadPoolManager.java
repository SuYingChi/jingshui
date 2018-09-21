package com.msht.watersystem.Utils;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/8  
 */
public class ThreadPoolUtil {
    private static final int HANDLE_UPDATE_INDEX = 0;
    private static ScheduledThreadPoolExecutor scheduledThreadPool = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable  r) {
            return new Thread(r,Thread.currentThread().getName());
        }
    });
    public static  void onThreadPoolStart(final Handler handler, int intervalTime) {
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        handler.obtainMessage(HANDLE_UPDATE_INDEX).sendToTarget();
                                                    }
                                                }, intervalTime, intervalTime,
                TimeUnit.SECONDS);
    }
    public static void onShutDown(){
        if (scheduledThreadPool!=null){
            scheduledThreadPool.shutdown();
        }
    }
    public static void onShutDownNow(){
        if (scheduledThreadPool!=null){
            scheduledThreadPool.shutdownNow();
        }
    }
}
