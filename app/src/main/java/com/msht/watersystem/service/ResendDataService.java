package com.msht.watersystem.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ResendDataService extends Service {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_FOO = "com.msht.watersystem.Service.action.FOO";
    public static final String ACTION_BAZ = "com.msht.watersystem.Service.action.BAZ";

    // TODO: Rename parameters
    public static final String EXTRA_PARAM1 = "com.msht.watersystem.Service.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.msht.watersystem.Service.extra.PARAM2";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            List<OrderInfo> infos = getOrderDao().loadAll();
            if (infos.size()>=1&&infos!=null){

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
