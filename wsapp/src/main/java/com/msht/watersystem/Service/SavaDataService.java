package com.msht.watersystem.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import java.util.List;

public class SavaDataService extends Service   {

    public SavaDataService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            OrderInfoDao mOrderInfoDao = AppContext.getInstances().getDaoSession().getOrderInfoDao();
            List<OrderInfo> infos = mOrderInfoDao.loadAll();
            if (infos.size()>=1&&infos!=null){

            }

        }
        return super.onStartCommand(intent, flags, startId);
    }
}
