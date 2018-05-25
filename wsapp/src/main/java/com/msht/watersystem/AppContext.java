package com.msht.watersystem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.utils.logs.LogUtils;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.Utils.CaughtExceptionTool;
import com.msht.watersystem.gen.DaoMaster;
import com.msht.watersystem.gen.DaoSession;
import com.msht.watersystem.receiver.PortReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rain on 2017/11/6.
 */
public class AppContext extends AppLibsContext {
    private List<Activity> mActivityList;
    private DaoMaster.DevOpenHelper mHelper;
    private SQLiteDatabase db;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    public static AppContext instances;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mActivityList=new ArrayList<>();
        LogUtils.initLogs(this, true, true, true, true, true, true);
        initPortBroadcast();
        CaughtExceptionTool.getInstance().init(this);  //异常捕获
        instances = this;
        mContext = getApplicationContext();
       // GreenDaoManager.getInstance();    //数据库存储订单数据
        //setDatabase();
       /* ArrayList<byte[]> types = new ArrayList<>();
        types.add(new byte[]{0x01, 0x04});//如果需要新增其他类型的特例则使用 add 方法叠加即可
        SpecialUtils.addTypes(types);*/
    }
    /**
     * 初始化广播事件以及后台服务事件监听串口接收程序
     */
    public void initPortBroadcast() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        PortReceiver receiver = new PortReceiver();
        registerReceiver(receiver, filter);
    }
    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        // 低内存的时候执行
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        super.onTrimMemory(level);
    }
    public static AppContext getInstances(){
        return instances;
    }

    public static Context getContext() {
        return mContext;
    }
    /**
     * 设置greenDao
     */
    private void setDatabase() {
        // 通过 DaoMaster 的内部类 DevOpenHelper，你可以得到一个便利的 SQLiteOpenHelper 对象。
        // 可能你已经注意到了，你并不需要去编写「CREATE TABLE」这样的 SQL 语句，因为 greenDAO 已经帮你做了。
        // 注意：默认的 DaoMaster.DevOpenHelper 会在数据库升级时，删除所有的表，意味着这将导致数据的丢失。
        // 所以，在正式的项目中，你还应该做一层封装，来实现数据库的安全升级。
        mHelper = new DaoMaster.DevOpenHelper(this, "orderdata-db", null);
        db = mHelper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。
        mDaoMaster = new DaoMaster(db);
        mDaoSession = mDaoMaster.newSession();
    }
    public DaoSession getDaoSession() {
        return mDaoSession;
    }
    public SQLiteDatabase getDb() {
        return db;
    }

    public void addActivity(Activity activity){
        if (!mActivityList.contains(activity)){
            mActivityList.add(activity);
        }
    }
    public void removeAllActivity(){
        for (Activity activity:mActivityList){
            if (activity!=null){
                activity.finish();
            }
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    public void removeActivity(Activity activity){
        if (mActivityList.contains(activity)){
            mActivityList.remove(activity);
            if (activity!=null){
               activity.finish();
            }
        }
    }
}
