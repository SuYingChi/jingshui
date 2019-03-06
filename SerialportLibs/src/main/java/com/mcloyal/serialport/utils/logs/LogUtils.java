package com.mcloyal.serialport.utils.logs;


import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.mcloyal.serialport.constant.ConstantUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日志控制工具类
 */
public class LogUtils {

    private static boolean LOG_ON_D = false;
    private static boolean LOG_ON_I = false;
    private static boolean LOG_ON_W = false;
    private static boolean LOG_ON_E = false;
    private static boolean TO_FILE = false;

    private static int LOG_MAX_BUFFER = 4 * 1024;//LOG 在内存中的最大缓存
    private static StringBuffer LOG_BUFFER = new StringBuffer();//LOG 緩存對象
    private static String LOG_DIR = ConstantUtil.LOGS_PATH;//Logger的打印文件夹

    /**
     * E级别日志输出
     *
     * @param tag
     * @param msg
     */
    public static void e(String tag, String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (LOG_ON_E) {
            Log.e(tag, msgMethod(msg));
        }
        if (TO_FILE) {
            log2Buffer("E", tag, msg);
        }
    }

    /**
     * info级别日志控制
     *
     * @param tag
     * @param msg
     */
    public static void i(String tag, String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (LOG_ON_I) {
            Log.i(tag, msgMethod(msg));
        }
        if (TO_FILE) {
            log2Buffer("I", tag, msg);
        }
    }

    /**
     * warn message
     *
     * @param tag
     * @param msg
     * @Title: w
     */
    public static void w(String tag, String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        if (LOG_ON_W) {
            Log.w(tag, msgMethod(msg));
        }
        if (TO_FILE) {
            log2Buffer("W", tag, msg);
        }
    }

    /**
     * debug级别日志控制
     *
     * @param tag
     * @param msg
     */
    public static void d(String tag, String msg) {
        if (LOG_ON_D) {
            Log.d(tag, msgMethod(msg));
        }
        if (TO_FILE) {
            log2Buffer("D", tag, msg);
        }
    }

    /**
     * Log 到 String Buffer中
     *
     * @param logType
     * @param tag
     * @param msg
     * @Title: log2Buffer
     */
    private static void log2Buffer(String logType, String tag, String msg) {
        try {
            StringBuffer sb = new StringBuffer();
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            if (stacks == null || stacks.length < 3) {
                return;
            }
            sb.append(logType).append("|")
                    .append(dateToString(new Date(), FORMAT_DATETIME)).append("|")
                    .append(stacks[2].getClassName()).append("|")
                    .append(stacks[2].getMethodName()).append("|")
                    .append(stacks[2].getLineNumber());
            LOG_BUFFER.append(sb.toString()).append("|").append(msg).append("\n");
            sb = null;
            if (LOG_BUFFER.length() >= LOG_MAX_BUFFER) {
                new Thread() {
                    public void run() {
                        writeLog2File(false);
                    }
                }.start();
            }
        } catch (Exception e) {

        }
    }

    /**
     * 写入到文件
     *
     * @param isForce 是否是強制寫入
     * @Title: write2File
     */
    private synchronized static void writeLog2File(boolean isForce) {
        if (!isForce) {
            if (LOG_BUFFER.length() < LOG_MAX_BUFFER) {
                return;
            }
        }
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return;
        }
        String fileName = dateToString(new Date(), FORMAT_DATE) + ".log";
        File fileDir = new File(Environment.getExternalStorageDirectory(),
                LOG_DIR);
        File logFile = new File(fileDir, fileName);
        String s = LOG_BUFFER.toString();
        LOG_BUFFER.delete(0, LOG_BUFFER.length());
        FileWriter fileOutputStream = null;
        try {
            if (!fileDir.exists()) {
                if (!fileDir.mkdirs()) {
                    return;
                }
            }
            if (!logFile.exists()) {
                if (!logFile.createNewFile()) {
                    return;
                }
            }
            fileOutputStream = new FileWriter(logFile, true);
            fileOutputStream.write(s);
            fileOutputStream.flush();
        } catch (Exception e) {
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                }
            }
        }
        cleanLogFile(fileDir);
    }

    /**
     * 文件日志控制开关
     *
     * @param context 上下文对象
     * @param e       E级别日志是否输出
     * @param i       I级别日志是否输出
     * @param w       W级别日志是否输出
     * @param d       D级别日志是否输出
     * @param file    是否将日志输出到文件内
     * @param crash   是否记录运行时异常日志
     */
    public static void initLogs(Context context, boolean e, boolean i, boolean w, boolean d, boolean file, boolean crash) {
        LogUtils.LOG_ON_E = e;
        LogUtils.LOG_ON_I = i;
        LogUtils.LOG_ON_W = w;
        LogUtils.LOG_ON_D = d;
        LogUtils.TO_FILE = file;
        if (crash) {//异常信息保存SD卡
            CrashHandler myCrashHandler = CrashHandler.getInstance();
            myCrashHandler.init(context);
            Thread.currentThread().setUncaughtExceptionHandler(myCrashHandler);
        }
    }

    /**
     * Date 2 String
     *
     * @param source
     * @param format
     * @return
     * @Title: dateToString
     */
    public static String dateToString(Date source, String format) {
        if (source == null) {
            return null;
        }
        String tmpString = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        try {
            tmpString = simpleDateFormat.format(source);
        } catch (Exception e) {
        }
        return tmpString;
    }

    /**
     * 取得今天和前几天时间数组
     *
     * @param x 前几天
     * @return
     */
    public static String[] getDateAndPrev(int x) {
        Calendar now = Calendar.getInstance();
        if (x <= 0) {
            return new String[]{dateToString(new Date(), FORMAT_DATE)};
        }
        String[] s = new String[x + 1];
        s[0] = dateToString(new Date(), FORMAT_DATE);
        for (int i = 0; i < x; i++) {
            now.roll(Calendar.DAY_OF_YEAR, -1);
            String sd = dateToString(now.getTime(), FORMAT_DATE);
            s[i + 1] = sd;
        }
        return s;
    }

    /**
     * 清除多余的日志文件
     *
     * @param fileDir
     */
    private static void cleanLogFile(File fileDir) {
        // 删除多余的文件
        String[] dates = getDateAndPrev(1);
        if (dates != null && dates.length > 0) {
            File[] files = fileDir.listFiles();
            int dal = dates.length;
            int length = (files != null ? files.length : 0);
            for (int i = 0; i < length; i++) {
                File ff = files[i];
                boolean isDelete = true;
                String ffName = ff.getName();
                for (int j = 0; j < dal; j++) {
                    String dd = dates[j];
                    if (ffName.startsWith(dd) && ffName.endsWith("log")) {
                        isDelete = false;
                        break;
                    }
                }
                if (isDelete) {
                    boolean f = ff.delete();
                }
            }
        }
    }

    public final static String FORMAT_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public final static String FORMAT_DATE = "yyyy-MM-dd";

    /**
     * @Title: clear
     */
    public static void clear() {
        new Thread() {
            public void run() {
                writeLog2File(true);
            }
        }.start();
    }

    /**
     * 处理字符串为null的情况
     *
     * @return
     */
    private static String msgMethod(String msgStr) {
        return msgStr == null ? "null" : msgStr;
    }
}
