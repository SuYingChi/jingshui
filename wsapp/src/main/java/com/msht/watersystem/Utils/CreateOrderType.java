package com.msht.watersystem.Utils;

import com.mcloyal.serialport.entity.Packet;

import java.util.ArrayList;

/**
 * Created by hong on 2018/1/26.
 */

public class CreateOrderType {
    public static byte[] OrderByteData(Packet packet1){
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
        String PacketString=ByteUtils.Byte2Hex(head)+" ";
        PacketString=PacketString+ByteUtils.ByteArrToHex(len)+" ";
        PacketString=PacketString+ByteUtils.Byte2Hex(version)+" ";
        PacketString=PacketString+ByteUtils.Byte2Hex(back1)+" ";
        PacketString=PacketString+ByteUtils.ByteArrToHex(frame)+" ";
        PacketString=PacketString+ByteUtils.ByteArrToHex(back2)+" ";
        PacketString=PacketString+ByteUtils.ByteArrToHex(cmd)+" ";
        PacketString=PacketString+ByteDataString(packet1.getData());
        PacketString=PacketString+ByteUtils.ByteArrToHex(cre);
        return  PacketString;
    }
    private static String ByteDataString(ArrayList<Byte> data){
        String DataString="";
        if (data!=null&&data.size()!=0){
            byte [] byteData=new byte[data.size()];
            for (int i=0;i<data.size();i++){
                byteData[i]=data.get(i);
            }
            DataString=ByteUtils.ByteArrToHex(byteData);
        }
        return DataString;
    }

}
