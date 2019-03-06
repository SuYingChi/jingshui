package com.mcloyal.serialport.utils;

import android.text.TextUtils;

/**
 * 字节和位之间的相互转换
 * Created by rain on 2017/5/13.
 */
public class BitsUtils {
    /**
     * 将字节转换为8位
     *
     * @param b
     * @return
     */
    public static String byteToBit(byte b) {
        return "" + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1) + (byte) ((b >> 5) & 0x1)
                + (byte) ((b >> 4) & 0x1) + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1) + (byte) ((b >> 1) & 0x1)
                + (byte) ((b >> 0) & 0x1);
    }

    /**
     * Bit转Byte
     *
     * @param byteStr
     * @return
     */
    public static byte bitsToByte(String byteStr) {
        int re, len;
        if (null == byteStr) {
            return 0;
        }
        len = byteStr.length();
        if (len != 4 && len != 8) {
            return 0;
        }
        if (len == 8) {// 8 bit处理
            if (byteStr.charAt(0) == '0') {// 正数
                re = Integer.parseInt(byteStr, 2);
            } else {// 负数
                re = Integer.parseInt(byteStr, 2) - 256;// -256
            }
        } else {// 4 bit处理
            re = Integer.parseInt(byteStr, 2);
        }
        return ByteUtils.getUnsignedByte((byte) re);
    }

    /**
     * 将字符串进行前后倒置排序
     *
     * @param str
     * @return
     */
    public static String strReverse(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        return new StringBuffer(str).reverse().toString();
    }

}
