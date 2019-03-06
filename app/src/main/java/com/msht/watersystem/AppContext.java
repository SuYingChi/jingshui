package com.msht.watersystem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.utils.logs.LogUtils;
import com.msht.watersystem.gen.DaoMaster;
import com.msht.watersystem.gen.DaoSession;
import com.msht.watersystem.receiver.PortReceiver;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author rain
 * @date 2017/11/6
 */
public class AppContext extends AppLibsContext {
    private List<Activity> mActivityList;
    public static AppContext instances;

    private DaoSession mDaoSession;
    /** 日志输出控制开关
     *    E级别日志输出
     *    i级别日志输出
     *    w级别日志输出
     *    d级别日志输出
     *    是否将日志输出到文件内
     *    crash是否记录运行异常日志
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mActivityList=new ArrayList<>();
        initPortBroadcast();
       // CaughtExceptionTool.getInstance().init(this);  //异常捕获
        instances = this;
       // mContext = getApplicationContext();
        /*初始化视频播放*/
       // Vitamio.initialize(getApplicationContext());
       // LogUtils.initLogs(this,true,true,true,true,true,true);
        LogUtils.initLogs(this,false,false,false,false,false,true);
       // GreenDaoManager.getInstance();    //数据库存储订单数据
        setDatabase();
       /* ArrayList<byte[]> types = new ArrayList<>();
        types.add(new byte[]{0x01, 0x04});//如果需要新增其他类型的特例则使用 add 方法叠加即可
        SpecialUtils.addTypes(types);*/
       if (BuildConfig.DEBUG){
           StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                   .detectCustomSlowCalls()
                   .detectDiskReads()
                   .detectDiskWrites()
                   .detectNetwork()
                   .penaltyLog()
                   .build());
           StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                   .detectActivityLeaks()
                   .detectLeakedClosableObjects()
                   .detectLeakedSqlLiteObjects()
                   .penaltyLog()
                   .build());
       }
    }

    private void setDatabase() {

        DaoMaster.DevOpenHelper devOpenHelper = new
                //此处为自己需要处理的表
                DaoMaster.DevOpenHelper(AppContext.getContext(), "orderdata-db", null);
        DaoMaster mDaoMaster = new DaoMaster(devOpenHelper.getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();
    }
    public DaoSession getDaoSession() {
        return mDaoSession;
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
    public static AppContext getInstance(){
        return instances;
    }
    public static Context getContext() {
        return instances.getApplicationContext();
    }
    public static Context getWaterApplicationContext() {
        return instances.getApplicationContext();
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
    }
    public void removeActivity(Activity activity){
        if (mActivityList.contains(activity)){
            mActivityList.remove(activity);
            if (activity!=null){
               activity.finish();
            }
        }
    }
    public void onKillProcess(){
        android.os.Process.killProcess(android.os.Process.myPid());
       // System.exit(0);
    }
}
