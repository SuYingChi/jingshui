package com.mcloyal.serialport.utils;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.logs.LogUtils;

import java.util.Observer;

/**
 * Created by rain on 2017/7/19.
 */

public final class ComServiceConnection implements ServiceConnection {
    private final static String TAG = ComServiceConnection.class.getSimpleName();
    private PortService service = null;
    private Observer observer;
    private ConnectionCallBack connectionCallBack;

    public ComServiceConnection(Observer observer, ConnectionCallBack callBack) {
        this.observer = observer;
        this.connectionCallBack = callBack;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((PortService.LocalBinder) binder).getService();
        service.addObserver(observer);
        LogUtils.d(TAG, "onServiceConnected ()");
        if (connectionCallBack != null) {
            connectionCallBack.onServiceConnected(service);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    public PortService getService() {
        return service;
    }
    
    /**
     * 服务启动的事件回调
     */
    public interface ConnectionCallBack {
        void onServiceConnected(PortService service);
    }
}
