package com.mcloyal.serialport.utils;

import android.content.Context;

import com.mcloyal.serialport.constant.ConstantUtil;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;

import java.util.ArrayList;

/**
 * 数据包封装
 *
 * @author rain
 * @date 2017/11/17
 */

public class PacketUtils {


    /**
     * 查询套餐业务数据
     *
     * @param mContext
     * @return
     * @throws FrameException
     * @throws CRCException
     * @throws CmdTypeException
     */
    public static byte[] queryPackage(Context mContext) throws FrameException, CRCException, CmdTypeException {
        return makePackage(FrameUtils.getFrame(mContext), new byte[]{0x01, 0x09}, null);
    }

    /**
     * 封装上报服务器的数据包
     *
     * @param frame 数据帧序
     * @param type  CMD指令类型
     * @param data  数据内容
     * @return
     * @throws CRCException     CRC校验
     * @throws FrameException   帧序异常
     * @throws CmdTypeException 类型异常
     */
    public static byte[] makePackage(byte[] frame, byte[] type, byte[] data) throws FrameException, CRCException, CmdTypeException {
        return getReply(frame, type, data);
    }

    /**
     * 封装上报服务器的数据包
     *
     * @param frame 数据帧序
     * @param type  CMD指令类型
     * @param data  数据内容
     * @return
     * @throws CRCException     CRC校验
     * @throws FrameException   帧序异常
     * @throws CmdTypeException 类型异常
     */
    private static byte[] getReply(byte[] frame, byte[] type, byte[] data) throws CRCException, FrameException, CmdTypeException {
        if (frame == null || frame.length != 4) {
            throw new FrameException("数据帧序为空或者长度错误");
        }
        if (type == null || type.length != 2) {
            throw new CmdTypeException("数据类型为空或者长度错误");
        }
        ArrayList<Byte> replyList = new ArrayList<>();
        //协议版本
        replyList.add((byte) 0x10);
        //备用字节
        replyList.add((byte) 0x00);
        //数据帧序
        ListUtils.append(replyList, frame);
        //保留
        ListUtils.append(replyList, new byte[]{0x00, 0x00});
        //数据类型
        ListUtils.append(replyList, type);
        if (data != null && data.length > 0) {
            //数据内容
            ListUtils.append(replyList, data);
        }
        byte[] len = NumberUtil.unsignedShortToByte2(replyList.size() + 4);
        //数据长度
        replyList.add(0, len[1]);
        replyList.add(0, len[0]);
        try {
            Byte[] tc = replyList.toArray(new Byte[replyList.size()]);
            byte[] cc = new byte[replyList.size()];
            for (int i = 0; i < tc.length; i++) {
                cc[i] = tc[i];
            }
          //  System.out.println("cc=="+ByteUtils.byte2hex(cc));
            byte[] crc = Crc16.crcCalculateByte(cc);
           // System.out.println("crc=="+ByteUtils.byte2hex(crc));
            ListUtils.append(replyList, crc);//尾部追加crc16数值
            //最后添加包头，确保包头不参与CRC计算
            replyList.add(0, ConstantUtil.START_);//包头
            Byte[] tb = replyList.toArray(new Byte[replyList.size()]);
            byte reply[] = new byte[replyList.size()];
            for (int i = 0; i < tb.length; i++) {
                reply[i] = tb[i];
            }
            return reply;
        } catch (CRCException e) {
            e.printStackTrace();
        }
        return null;
    }
}
