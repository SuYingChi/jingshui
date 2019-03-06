package com.mcloyal.serialport.utils;

import com.mcloyal.serialport.exception.CRCException;

/**
 * CRC校验计算
 * 作者：huangzhong on 2017/2/12 14:04
 * 邮箱：mcloyal@163.com
 */

public class Crc16 {

    /**
     * CRC16校验码计算(x25计算公式)
     *
     * @param bytes
     * @param len
     * @return
     */
    private static int CRC16(byte[] bytes, int len) {
        int crc;
        int i, temp;
        crc = 0xffff;
        for (i = 0; i < len; i++) {
            crc = crc ^ byteToInteger(bytes[i]);
            for (temp = 0; temp < 8; temp++) {
                if ((crc & 0x01) == 1) {
                    crc = (crc >> 1) ^ 0x8408;//0x8408   0xa0
                }else {
                    crc = crc >> 1;
                }
            }
        }
        return ~crc;
    }

    private static int byteToInteger(byte b) {
        int value;
        value = b & 0xff;
        return value;
    }

    /**
     * 计算CRC 返回两个字节的字节数组
     *
     * @param address
     * @return
     */
    public static byte[] crcCalculateByte(byte[] address) throws CRCException {
        if (address == null || address.length == 0) {
            throw new CRCException("待计算字节数组为空");
        }
        int s = CRC16(address, address.length);
        return NumberUtil.unsignedShortToByte2(s);
    }
}
