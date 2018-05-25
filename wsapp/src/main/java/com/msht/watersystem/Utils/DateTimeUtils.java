package com.msht.watersystem.Utils;

import android.content.Context;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.Settings;

import com.msht.watersystem.entity.OrderInfo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by hong on 2018/3/5.
 */

public class DateTimeUtils {
    /**
     * 设置年月日时分秒
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @throws IOException
     * @throws InterruptedException
     */
    public static void setDateTime(Context context,int year, int month, int day, int hour, int minute, int second) throws IOException, InterruptedException {
        requestPermission();
        Settings.System.putString(context.getContentResolver(),Settings.System.TIME_12_24,"");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        long when = c.getTimeInMillis();
        long now = Calendar.getInstance().getTimeInMillis();
        if (now - when > 1000) {
            throw new IOException("Failed to set Date.");
        }
        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
    }
    //获取权限
    private static void requestPermission() throws InterruptedException, IOException {
        createSuProcess("chmod 666 /dev/alarm").waitFor();
    }
    private static Process createSuProcess(String cmd) throws IOException {
        DataOutputStream os = null;
        Process process = createSuProcess();
       // Process process = Runtime.getRuntime().exec("su");
        try {
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit $?\n");
            os.flush();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return process;
    }
    private static Process createSuProcess() throws IOException {
        File rootUser = new File("/system/xbin/ru");
        if(rootUser.exists()) {
            return Runtime.getRuntime().exec(rootUser.getAbsolutePath());
        } else {
            return Runtime.getRuntime().exec("su");
        }
    }
    public static boolean IsRoot(){
        boolean res=false;
        try{
            if ((!new File("/system/bin/su").exists())&&(!new File("/system/xbin/su").exists())){
                res=false;
            }else {
                res=true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    public static boolean isCheckTime(int hour1,int hour2,int minute1,int minute2){
        Calendar calendar=Calendar.getInstance();
        int hour=calendar.get(Calendar.HOUR_OF_DAY);
        int minute=calendar.get(Calendar.MINUTE);
        int minuteOfDay=hour*60+minute;
        final int start=hour1*60+minute1;
        final int end=hour2*60+minute2;
        if (minuteOfDay>=start&&minuteOfDay<=end){
            return true;
        }else {
            return false;
        }
    }
}
