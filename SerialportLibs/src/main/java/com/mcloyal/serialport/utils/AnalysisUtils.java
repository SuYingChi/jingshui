package com.mcloyal.serialport.utils;

import com.mcloyal.serialport.constant.ConstantUtil;
import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.AnalysisException;
import com.mcloyal.serialport.service.PortService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * 数据包拆分
 *
 * @author rain
 * @date 2017/5/13
 */
public class AnalysisUtils {

    /**
     * 拆分数据包
     *
     * @param buffer
     * @param length
     * @return
     * @throws AnalysisException
     */
    public static Packet analysisFrame(byte[] buffer, int length) throws AnalysisException {
        if (buffer == null || buffer.length != length) {
            throw new AnalysisException("data为空或者长度不是" + length + "位");
        }
        Packet packet = null;
        try {
            packet = new Packet();
            //包头
            packet.setStart(buffer[0]);
            byte[] len = Arrays.copyOfRange(buffer, 1, 3);
            //长度,去除包头以外的长度
            packet.setLen(len);
            //版本
            byte version = buffer[3];
            packet.setVersion(version);
            byte back1 = buffer[4];
            //备用1字节
            packet.setBack1(back1);
            byte[] frame = Arrays.copyOfRange(buffer, 5, 9);
            //数据帧序
            packet.setFrame(frame);
            byte[] back2 = Arrays.copyOfRange(buffer, 9, 11);
            //备用2字节
            packet.setBack2(back2);
            byte[] cmd = Arrays.copyOfRange(buffer, 11, 13);
            //数据类型
            packet.setCmd(cmd);
            byte[] data = Arrays.copyOfRange(buffer, 13, buffer.length - 2);
            if (data != null && data.length > 0) {
                ArrayList<Byte> bData = new ArrayList<>();
                for (byte temp : data) {
                    bData.add(temp);
                }
                //数据内容
                packet.setData(bData);
            }
            byte crc[] = Arrays.copyOfRange(buffer, buffer.length - 2, buffer.length);
            packet.setCrc(crc);
        } catch (Exception e) {
            throw new AnalysisException("截取数据错误");
        }
        return packet;
    }

    public static boolean isCmd104ConsumptionType3(Packet packet){
        if (!Arrays.equals(packet.getCmd(),new byte[]{0x01,0x04})){
            return true;
        }else if (ByteUtils.byteToInt(packet.getData().get(ConstantUtil.VALUE15))!=ConstantUtil.VALUE3){
            return true;
        }/*else if (!isEvent(ByteUtils.intToBinary(ByteUtils.byteToInt(packet.getData().get(ConstantUtil.VALUE44))),ConstantUtil.VALUE3)){
            return true;
        }*/else {
           return  !isEvent(ByteUtils.intToBinary(ByteUtils.byteToInt(packet.getData().get(ConstantUtil.VALUE44))),ConstantUtil.VALUE3);
           // return false;
        }
    }
    /**判断Flag位 */
    public static boolean isEvent(String s,int index){
        boolean isOneValue;
        if (s.length()>=8){
            char charIndex=s.charAt(index);
            String stringIndex=String.valueOf(charIndex);
            isOneValue=stringIndex.equals(ConstantUtil.ONE_VALUE);
        }else {
            isOneValue=false;
        }
        return isOneValue;
    }
}
