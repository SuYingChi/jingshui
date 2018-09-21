package com.msht.watersystem.Utils;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.Manager.DateMassageEvent;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.Manager.MessageEvent;
import com.msht.watersystem.Manager.RestartAppEvent;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
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
public class ThreadPoolManager {
    private static final  int MINI_LENGTH=1;
    private static final int HOUR_ONE=0, HOUR_TWO =1;
    private static final int MINUTE_ONE =30, MINUTE_TWO =30;
    private static final int HANDLE_UPDATE_INDEX = 0;
    /**单利引用 **/
    private static volatile ThreadPoolManager mInstance;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
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
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new RegularlyCheckTimeTask(), 0, 3,
                TimeUnit.MINUTES);
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
                EventBus.getDefault().post(new DateMassageEvent(2));
            }else if (DateTimeUtils.isCheckTime(5,23,45,55)){
                /*开背光*/
                EventBus.getDefault().post(new DateMassageEvent(1));
            }
            if (DateTimeUtils.isCheckTime(2,2,10,13)){
                if (VariableUtil.mFirstOpen){
                   // EventBus.getDefault().post(new RestartAppEvent(true));
                   // RestartAppUtil.restartWaterSystem(AppContext.getContext());
                }
            }
            VariableUtil.mFirstOpen=true;
        }
    }
    private void onGetOrderData(){
        List<OrderInfo> orderData = getOrderDao().loadAll();
        if (orderData!=null&&orderData.size()>=MINI_LENGTH){
            EventBus.getDefault().post(new MessageEvent(orderData));
        }
    }
    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
    public  void onShutDown(){
        if (scheduledThreadPoolExecutor!=null){
            scheduledThreadPoolExecutor.shutdown();
        }
    }
    public  void onShutDownNow(){
        if (scheduledThreadPoolExecutor!=null){
            scheduledThreadPoolExecutor.shutdownNow();
        }
    }
}
