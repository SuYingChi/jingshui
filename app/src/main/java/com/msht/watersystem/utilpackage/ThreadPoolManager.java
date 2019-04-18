package com.msht.watersystem.utilpackage;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.eventmanager.DateMassageEvent;
import com.msht.watersystem.eventmanager.MessageEvent;
import com.msht.watersystem.eventmanager.MyAppManage;
import com.msht.watersystem.eventmanager.RestartAppEvent;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/8  
 */
public class ThreadPoolManager {
    private static final  int MINI_LENGTH=1;
    private static final int HOUR_ONE=0, HOUR_TWO =1;
    private static final int MINUTE_ONE =30, MINUTE_TWO =30;
    private static final int HANDLE_UPDATE_INDEX = 0;
    /**单利引用 **/
    private static volatile ThreadPoolManager mInstance;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private RegularlyCheckTimeTask regularlyCheckTimeTask;
    private ThreadPoolManager(Context context){
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable  r) {
                return new Thread(r,Thread.currentThread().getName());
            }
        });
    }
    /**
     * 获取单例引用
     * @return
     */
    public static ThreadPoolManager getInstance(Context context){
        ThreadPoolManager inst = mInstance;
        if (inst == null) {
            synchronized (ThreadPoolManager.class) {
                inst = mInstance;
                if (inst == null) {
                    inst = new ThreadPoolManager(context.getApplicationContext());
                    mInstance = inst;
                }
            }
        }
        return inst;
    }
    public  void onThreadPoolInitiate(final Handler handler, int intervalTime) {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        handler.obtainMessage(HANDLE_UPDATE_INDEX).sendToTarget();
                                                    }
                                                }, intervalTime, intervalTime,
                TimeUnit.SECONDS);
    }
    public void onThreadPoolDateStart(){
        if (regularlyCheckTimeTask==null){
            regularlyCheckTimeTask=new RegularlyCheckTimeTask();
            scheduledThreadPoolExecutor.scheduleAtFixedRate(new RegularlyCheckTimeTask(), 0, 3,
                    TimeUnit.MINUTES);
        }
    }
    private class RegularlyCheckTimeTask implements Runnable {
        @Override
        public void run() {
            if (DateTimeUtils.isCheckTime(HOUR_ONE, HOUR_TWO, MINUTE_ONE, MINUTE_TWO)){
                if (VariableUtil.sendStatus){
                    onGetOrderData();
                }
            }
            if (DateTimeUtils.isCheckTime(0,5,5,40)){
                /*关闭背光*/
                if (VariableUtil.isOpenBackLight){
                    EventBus.getDefault().post(new DateMassageEvent(2));
                }
            }else if (DateTimeUtils.isCheckTime(5,23,45,55)){
                /*开背光*/
                if (!VariableUtil.isOpenBackLight){
                    EventBus.getDefault().post(new DateMassageEvent(1));
                }
            }
            if (DateTimeUtils.isCheckTime(2,2,10,13)){
                if (VariableUtil.mFirstOpen){
                    EventBus.getDefault().post(new RestartAppEvent(true));
                }
            }
            if (DateTimeUtils.isCheckTime(0,0,1,4)){
                if (VariableUtil.mFirstClear){
                    MyAppManage.clearMyAppActivityMemory(AppContext.getContext());
                }
            }
            VariableUtil.mFirstClear=true;
            VariableUtil.mFirstOpen=true;
        }
    }
    private class RegularlyCheckForegroundTask implements Runnable {
        @Override
        public void run() {
            if (DateTimeUtils.isCheckTime(0,0,1,4)){
                if (VariableUtil.mFirstClear){
                    MyAppManage.clearMyAppActivityMemory(AppContext.getContext());
                }
            }
           /* *//*设置关闭视频播放*//*
            if (DateTimeUtils.isCheckTime(23,23,47,50)){
                EventBus.getDefault().post(new ControlVideoEvent(false));
            }
            *//*设置关闭视频播放*//*
            if (DateTimeUtils.isCheckTime(1,1,50,53)){
                EventBus.getDefault().post(new ControlVideoEvent(false));
            }
            *//*设置打开闭视频播放*//*
            if (DateTimeUtils.isCheckTime(5,5,50,53)){
                EventBus.getDefault().post(new ControlVideoEvent(true));
            }*/
            VariableUtil.mFirstClear=true;
            //startHomeTaskApp(AppContext.getWaterApplicationContext());
        }
    }
    private void onGetOrderData(){
        List<OrderInfo> orderData = getOrderDao().loadAll();
        if (orderData!=null&&orderData.size()>=MINI_LENGTH){
            EventBus.getDefault().post(new MessageEvent(orderData));
        }
    }
    private OrderInfoDao getOrderDao() {
        return AppContext.getInstance().getDaoSession().getOrderInfoDao();
    }
    public  void onShutDown(){
        if (scheduledThreadPoolExecutor!=null){
            if (regularlyCheckTimeTask!=null){
                scheduledThreadPoolExecutor.remove(regularlyCheckTimeTask);
            }
            scheduledThreadPoolExecutor.shutdown();
        }
    }
    public  void onShutDownNow(){
        if (scheduledThreadPoolExecutor!=null){
            scheduledThreadPoolExecutor.shutdownNow();
        }
    }

    /**
     * 判断本方法是否已经位于最前端
     *
     * @param context
     * @return 本应用已经位于最前端时，返回 true；否则返回 false
     */
    public static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (activityManager!=null){
            List<ActivityManager.RunningAppProcessInfo> appProcessInfoList = activityManager.getRunningAppProcesses();
            /**枚举进程*/
            for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfoList) {
                if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (appProcessInfo.processName.equals(context.getApplicationInfo().processName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public void startHomeTaskApp(Context context){
        if (!isRunningForeground(context)) {
            /**获取ActivityManager*/
            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            /**获得当前运行的task(任务)*/
            if (activityManager!=null){
                List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
                // List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.;
                for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                    /**找到本应用的 task，并将它切换到前台*/
                    if (taskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
                        activityManager.moveTaskToFront(taskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                        break;
                    }
                }
            }
        }
    }
}
