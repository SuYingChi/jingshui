package com.msht.watersystem.utilpackage;

import com.mcloyal.serialport.constant.Cmd;
import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.AppContext;

import java.util.ArrayList;

/**
 *
 * @author hong
 * @date 2018/1/26
 */

public class CreatePacketTypeUtil {
    public static byte[] byteOrderByteDataToString(Packet packet1){
        byte   head=packet1.getStart();
        byte[] len=packet1.getLen();
        byte   version=packet1.getVersion();
        byte   back1=packet1.getBack1();
        byte[] frame=packet1.getFrame();
        byte[] back2=packet1.getBack2();
        byte[] cmd=packet1.getCmd();
        byte[] cre=packet1.getCrc();
        ArrayList<Byte> data=packet1.getData();
        int datalenght=ByteUtils.byte2ToInt(len)+1;
        byte[] orderData=new byte[datalenght];
        orderData[0]=head;
        orderData[1]=len[0];
        orderData[2]=len[1];
        orderData[3]=version;
        orderData[4]=back1;
        orderData[5]=frame[0];
        orderData[6]=frame[1];
        orderData[7]=frame[2];
        orderData[8]=frame[3];
        orderData[9]=back2[0];
        orderData[10]=back2[1];
        orderData[11]=cmd[0];
        orderData[12]=cmd[1];
        int n=0;
        for (int i=13;i<datalenght;i++){   //n=4
            if (n<data.size()){
                orderData[i]=data.get(n);
            }
            n++;
        }
        orderData[61]=cre[0];
        orderData[62]=cre[1];
        return orderData;
    }
    public static String getPacketString(Packet packet1){
        byte   head=packet1.getStart();
        byte[] len=packet1.getLen();
        byte   version=packet1.getVersion();
        byte   back1=packet1.getBack1();
        byte[] frame=packet1.getFrame();
        byte[] back2=packet1.getBack2();
        byte[] cmd=packet1.getCmd();
        byte[] cre=packet1.getCrc();
        String mPacketString=ByteUtils.Byte2Hex(head)+" ";
        mPacketString=mPacketString+ByteUtils.ByteArrToHex(len)+" ";
        mPacketString=mPacketString+ByteUtils.Byte2Hex(version)+" ";
        mPacketString=mPacketString+ByteUtils.Byte2Hex(back1)+" ";
        mPacketString=mPacketString+ByteUtils.ByteArrToHex(frame)+" ";
        mPacketString=mPacketString+ByteUtils.ByteArrToHex(back2)+" ";
        mPacketString=mPacketString+ByteUtils.ByteArrToHex(cmd)+" ";
        mPacketString=mPacketString+onArrayToByteDataString(packet1.getData());
        mPacketString=mPacketString+ByteUtils.ByteArrToHex(cre);
        return  mPacketString;
    }
    private static String onArrayToByteDataString(ArrayList<Byte> data){
        String mDataString="";
        if (data!=null&&data.size()!=0){
            byte [] byteData=new byte[data.size()];
            for (int i=0;i<data.size();i++){
                byteData[i]=data.get(i);
            }
            mDataString=ByteUtils.ByteArrToHex(byteData);
        }
        return mDataString;
    }


    public static byte[] getPacketData103(){
        try {
            byte[] frame = FrameUtils.getFrame(AppContext.getWaterApplicationContext());
            byte[] type = new byte[]{0x01, 0x03};
            byte[] packet = PacketUtils.makePackage(frame, type, null);
            byte[] version=ByteUtils.intTo2Byte(AppPackageUtil.getPackageVersionCode());
            packet[9]=version[0];
            packet[10]=version[1];
            return packet;
        } catch (CRCException e) {
            e.printStackTrace();
        } catch (FrameException e) {
            e.printStackTrace();
        } catch (CmdTypeException e) {
            e.printStackTrace();
        }
        return Cmd.ComCmd.EQUIPMENT_INFO;
    }

}
