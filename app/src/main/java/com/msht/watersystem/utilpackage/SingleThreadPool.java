package com.msht.watersystem.utilpackage;

import android.content.Context;
import android.support.annotation.NonNull;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.eventmanager.DateMassageEvent;
import com.msht.watersystem.eventmanager.MyAppManage;
import com.msht.watersystem.eventmanager.RestartAppEvent;
import com.msht.watersystem.gen.OrderInfoDao;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public class SingleThreadPool {
    private static volatile SingleThreadPool mInstance;
    private ExecutorService executorService;
    private SingleThreadPool(Context context){
        executorService=  Executors.newFixedThreadPool(1);
    }
    /**
     * 获取单例引用
     * @return
     */
    public static SingleThreadPool getInstance(Context context){
        SingleThreadPool inst = mInstance;
        if (inst == null) {
            synchronized (ThreadPoolManager.class) {
                inst = mInstance;
                if (inst == null) {
                    inst = new SingleThreadPool(context.getApplicationContext());
                    mInstance = inst;
                }
            }
        }
        return inst;
    }
    public void onThreadSaveData(byte[] packet){
        executorService.execute(new SaveDataThreadTask( packet));
    }
    private class SaveDataThreadTask implements Runnable {
        private byte[] packet1;
        public SaveDataThreadTask(byte[] packet){
           packet1=packet;
        }
        @Override
        public void run() {
            try {
                OrderInfo insertData = new OrderInfo(null, packet1);
                getOrderDao().insert(insertData);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    private static OrderInfoDao getOrderDao() {
        return AppContext.getInstance().getDaoSession().getOrderInfoDao();
    }
    public  void onShutDown(){
        if (executorService!=null){
            executorService.shutdownNow();
        }
    }
}
