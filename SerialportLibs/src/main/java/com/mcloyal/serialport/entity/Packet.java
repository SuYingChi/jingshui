package com.mcloyal.serialport.entity;

import com.mcloyal.serialport.utils.ByteUtils;

import java.util.ArrayList;

/**
 * 数据包
 */
public class Packet extends Base {
    private byte start;//开始位
    private byte[] len = new byte[2];//数据包长度，除去包头以外的全部字节长度
    private byte version;//协议版本
    private byte back1;//备用字节
    private byte[] frame = new byte[4];//数据帧序
    private byte[] back2 = new byte[2];//保留
    private byte[] cmd = new byte[2];//CMD数据类型
    private ArrayList<Byte> data = new ArrayList();
    private byte[] crc = new byte[2];

    public byte getStart() {
        return start;
    }

    public void setStart(byte start) {
        this.start = start;
    }

    public byte[] getLen() {
        return len;
    }

    public void setLen(byte[] len) {
        this.len = len;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getBack1() {
        return back1;
    }

    public void setBack1(byte back1) {
        this.back1 = back1;
    }

    public byte[] getFrame() {
        return frame;
    }

    public void setFrame(byte[] frame) {
        this.frame = frame;
    }

    public byte[] getBack2() {
        return back2;
    }

    public void setBack2(byte[] back2) {
        this.back2 = back2;
    }

    public ArrayList<Byte> getData() {
        return data;
    }

    public void setData(ArrayList<Byte> data) {
        this.data = data;
    }

    public byte[] getCmd() {
        return cmd;
    }

    public void setCmd(byte[] cmd) {
        this.cmd = cmd;
    }

    public byte[] getCrc() {
        return crc;
    }

    public void setCrc(byte[] crc) {
        this.crc = crc;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "start=" + ByteUtils.toHex(start) +
                ", len=" + ByteUtils.byte2hex(len) +
                ", version=" + ByteUtils.toHex(version) +
                ", back1=" + ByteUtils.toHex(back1) +
                ", frame=" + ByteUtils.byte2hex(frame) +
                ", back2=" + ByteUtils.byte2hex(back2) +
                ", cmd=" + ByteUtils.byte2hex(cmd) +
                ", data=" + data +
                ", crc=" + ByteUtils.byte2hex(crc) +
                '}';
    }
}
