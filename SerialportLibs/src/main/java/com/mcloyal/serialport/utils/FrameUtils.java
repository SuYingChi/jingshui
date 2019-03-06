package com.mcloyal.serialport.utils;

import android.content.Context;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.AppPreferences;

/**
 * 读取帧的序列号
 */

public class FrameUtils {
    private final static String FRAME_NAME = "frame_name";

    /**
     * 读取帧序号
     *
     * @param mContext
     * @return
     */
    public static byte[] getFrame(Context mContext) {
        AppPreferences appPreferences = AppPreferences.getInstance((AppLibsContext) mContext.getApplicationContext());
        int frame = appPreferences.getInt(FRAME_NAME, 1);
        int temp = frame + 1;
        if (temp >= Integer.MAX_VALUE) {
            temp = 1;
        }
        appPreferences.putInt(FRAME_NAME, temp);
        return NumberUtil.intToByte4(temp);
    }
}
