package com.mcloyal.serialport.utils;

/**
 * 作者：huangzhong on 2017/2/12 10:16
 * 邮箱：mcloyal@163.com
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Locale;

/**
 * 字节工具类
 * @author hong
 */
public class ByteUtils {
    /**
     * 字节数组转换成十六进制输出
     *
     * @param buffer
     * @return
     */
    public static String byte2hex(byte[] buffer) {
        String h = "";
        for (int i = 0; i < buffer.length; i++) {
            String temp = Integer.toHexString(buffer[i] & 0xFF);
            if (temp.length() == 1) {
                temp = "0" + temp;
            }
            h = h + " " + temp;
        }
        return h;
    }

    /**
     * 将单个字节进行16进制输出
     *
     * @param b
     * @return
     */
    public static String toHex(byte b) {
        String result = Integer.toHexString(b & 0xFF);
        if (result.length() == 1) {
            result = '0' + result;
        }
        return result;
    }

    /**
     * 将data字节型数据转换为0~255 (0xFF即BYTE)。
     *
     * @param data
     * @return
     */
    public static byte getUnsignedByte(byte data) {
        return (byte) (data & 0xFF);
    }


    public static byte[] ByteTobyte(Byte[] data) {
        byte[] cc = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            cc[i] = data[i].byteValue();
        }
        return cc;
    }

    /**
     * byte 转Int
     *
     * @param b
     * @return
     */
    public static int byteToInt(byte b) {
        return (b) & 0xff;
    }

    public static int byte2ToInt(byte[] bytes) {
        int addr = (bytes[0] & 0xff) << 8 | bytes[1] & 0xff;
        return addr;
    }
    public static int byte3ToInt(byte[] bytes) {
        int addr =(bytes[0] & 0xff) << 16 | (bytes[1] & 0xff)<<8
                |bytes[2] & 0xff;
        return addr;
    }
    public static int byte4ToInt(byte[] bytes) {
        int addr =(bytes[0] & 0xff) << 24 | (bytes[1] & 0xff)<<16
                |(bytes[2] & 0xff)<<8| bytes[3] & 0xff;
        return addr;
    }
    public static int byte5ToInt(byte[] bytes) {
        int addr =(bytes[0]& 0xff)<< 32|(bytes[1] & 0xff) << 24 | (bytes[2] & 0xff)<<16
                |(bytes[3] & 0xff)<< 8| bytes[4] & 0xff;
        return addr;
    }
    public static int byte6ToInt(byte[] bytes) {
        int addr =(bytes[0] & 0xff) << 40
                | (bytes[1] & 0xff) << 32
                | (bytes[2] & 0xff) << 24
                | (bytes[3] & 0xff) << 16
                | (bytes[4] & 0xff) << 8
                |  bytes[5] & 0xff;
        return addr;
    }

    public static byte[] getBytes(Object obj) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        out.flush();
        byte[] bytes = bout.toByteArray();
        bout.close();
        out.close();
        return bytes;
    }
    public static byte[] toByteArray(Object object){
        byte[] bytes=null;
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        try{
            ObjectOutputStream objectOutputStream=new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            bytes=byteArrayOutputStream.toByteArray();
            objectOutputStream.close();
            byteArrayOutputStream.close();

        }catch (IOException e){
            e.printStackTrace();
        }
        return bytes;
    }

    /**十进制转八位二进制 */
    public static String intToBinary(int input) {
        //11110000
        String binaryString = Integer.toBinaryString(input);
        int binaryInt = Integer.parseInt(binaryString);
        return String.format(Locale.CHINA,"%08d",binaryInt);
    }
}
