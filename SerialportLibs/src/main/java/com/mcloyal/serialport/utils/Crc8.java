package com.mcloyal.serialport.utils;

import com.mcloyal.serialport.exception.CRCException;

/**
 * Crc8计算工具类
 */
public class Crc8 {

    public static void main(String[] args) {
        byte[] dd = new byte[]{0x5a, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x02, 0x01};
        byte[] dd2 = new byte[]{0x5a, 0x18, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x02, 0x02, (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00};
//        System.out.println(ByteUtils.toHex(CRC8(dd)));
//        System.out.println(ByteUtils.toHex(CRC8(dd2)));
    }

    /**
     * crc8计算算法
     * @param buffer
     * @return
     * @throws CRCException
     */
    public static byte CRC8(byte[] buffer) throws CRCException {
        if (buffer == null || buffer.length == 0) {
            throw new CRCException("计算CRC8错误,字节为空");
        }
        int crci = 0;//计算起始位
        for (int j = 0; j < buffer.length; j++) {
            crci ^= buffer[j] & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crci & 1) != 0) {
                    crci >>= 1;
                    crci ^= 0xa0;//检查向参数
                } else {
                    crci >>= 1;
                }
            }
        }
        return (byte) crci;
    }
}
