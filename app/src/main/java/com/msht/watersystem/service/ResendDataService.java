package com.msht.watersystem.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.Interface.ResendDataEvent;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.ThreadPoolManager;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import java.util.List;


/**
 * Demo class
 *
 * @author hong
 * @date 2018/06/07
 */
public class ResendDataService extends Service {
    private static final  int MINI_LENGTH=1;
    private static final int HOUR_ONE=0, HOUR_TWO =1;
    private static final int MINUTE_ONE =30, MINUTE_TWO =30;
    public ResendDataEvent events = BaseActivity.event;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            ThreadPoolManager.getInstance(getApplicationContext()).onThreadPoolDateStart();
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    private class RegularlyCheckTimeTask implements Runnable {
        @Override
        public void run() {
            if (DateTimeUtils.isCheckTime(HOUR_ONE, HOUR_TWO, MINUTE_ONE, MINUTE_TWO)){
                if (VariableUtil.sendStatus){
                    onGetOrderData();
                }
            }
        }
    }
    private void onGetOrderData(){
        List<OrderInfo> orderData = getOrderDao().loadAll();
        if (orderData!=null&&orderData.size()>=MINI_LENGTH){
            events.onHaveDataChange(orderData);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
