package com.mcloyal.serialport.utils;

import com.mcloyal.serialport.utils.logs.LogUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public class StringUtils {
    private final static String TAG = StringUtils.class.getSimpleName();

    /**
     * 截取信号值
     *
     * @param context
     * @return
     */
    public static int getcqs(String context) {
        int cqs = -1;
        Pattern p = Pattern.compile("(\\d+)");
        Matcher m = p.matcher(context);
        String str = "";
        if (m.find()) {
            str = m.group(1);
        }
        LogUtils.d(TAG, "截取的信号长度为：" + str);
        try {
            cqs = Integer.parseInt(str);
        } catch (Exception e) {
            LogUtils.d(TAG, "信号强度转换成int出错");
        }
        return cqs;
    }
}
