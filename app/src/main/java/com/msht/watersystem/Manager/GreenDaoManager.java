package com.msht.watersystem.Manager;

import com.msht.watersystem.AppContext;
import com.msht.watersystem.gen.DaoMaster;
import com.msht.watersystem.gen.DaoSession;

/**
 *
 * @author hong
 * @date 2018/2/2
 */

public class GreenDaoManager {
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    /**单例*/
    private static GreenDaoManager mInstance;


    private GreenDaoManager(){
        if (mInstance == null) {
            DaoMaster.DevOpenHelper devOpenHelper = new
                    //此处为自己需要处理的表
                    DaoMaster.DevOpenHelper(AppContext.getContext(), "orderdata-db", null);
            mDaoMaster = new DaoMaster(devOpenHelper.getWritableDatabase());
            mDaoSession = mDaoMaster.newSession();
        }
    }

    public static GreenDaoManager getInstance() {
        if (mInstance == null) {
            //保证异步处理安全操作
            synchronized (GreenDaoManager.class) {

                if (mInstance == null) {
                    mInstance = new GreenDaoManager();
                }
            }
        }
        return mInstance;
    }

    public DaoMaster getMaster() {
        return mDaoMaster;
    }
    public DaoSession getDaoSession() {
        return mDaoSession;
    }
    public DaoSession getNewSession() {
        mDaoSession = mDaoMaster.newSession();
        return mDaoSession;
    }
}
