package com.mcloyal.serialport.utils.logs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.AppManager;
import com.mcloyal.serialport.constant.ConstantUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 错误日志信息的管理操作
 */
public class CrashHandler implements UncaughtExceptionHandler {
    private static CrashHandler crashHandler;
    private Context context;
    private AppLibsContext appLibsContext;
    private String timeStamp;
    private String errorLog;
    private File errlogDir = null;

    private CrashHandler() {

    }

    /**
     * 获取CrashHandler单例模式
     *
     * @return
     */
    public static synchronized CrashHandler getInstance() {
        if (crashHandler == null) {
            crashHandler = new CrashHandler();
        }
        return crashHandler;
    }

    public void init(Context context) {
        this.context = context;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        appLibsContext = (AppLibsContext) context.getApplicationContext();
        timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
                .format(new Date());
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        errorLog = writer.toString();
        // 判断是否挂载了SD卡
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            errlogDir = new File(ConstantUtil.ERROR_PATH);// 日志文件夹
            if (!errlogDir.exists()) {
                errlogDir.mkdirs();
            }
        } else {
            return;
        }
        // 输出裁剪的临时文件
        String fileName = timeStamp + ".txt";
        File logFile = new File(errlogDir, fileName);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(logFile);
            byte data[] = errorLog.getBytes("utf-8");
            fileOutputStream.write(data, 0, data.length);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        AppManager.getAppManager().AppExit(appLibsContext);
    }
}