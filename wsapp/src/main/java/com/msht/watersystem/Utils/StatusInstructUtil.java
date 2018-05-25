package com.msht.watersystem.Utils;

import java.util.ArrayList;

/**
 * Created by hong on 2017/10/27.
 */

public class StatusInstructUtil {

    public static boolean StatusInstruct(ArrayList<Byte> byteList){
        byte[] DeviceId=new byte[4];
        DeviceId[0]=byteList.get(0);
        DeviceId[1]=byteList.get(1);
        DeviceId[2]=byteList.get(2);
        DeviceId[3]=byteList.get(3);
        FormatToken.DeviceId=ByteUtils.byte4ToInt(DeviceId);

        byte[] InstallId=new byte[2];
        InstallId[0]=byteList.get(4);
        InstallId[1]=byteList.get(5);
        FormatToken.InstallId=ByteUtils.byte2ToInt(InstallId);
        FormatToken.Ozonetime=ByteUtils.byteToInt(byteList.get(6));
        FormatToken.PriceNum=ByteUtils.byteToInt(byteList.get(7));
        FormatToken.humidity=ByteUtils.byteToInt(byteList.get(8));
        FormatToken.temperature=ByteUtils.byteToInt(byteList.get(9));

        byte[] Origin=new byte[2];
        Origin[0]=byteList.get(10);
        Origin[1]=byteList.get(11);
        FormatToken.OriginTDS=ByteUtils.byte2ToInt(Origin);

        byte[] Purification=new byte[2];
        Purification[0]=byteList.get(12);
        Purification[1]=byteList.get(13);
        FormatToken.PurificationTDS=ByteUtils.byte2ToInt(Purification);

        FormatToken.OriginTDS0=ByteUtils.byteToInt(byteList.get(10));
        FormatToken.OriginTDS1=ByteUtils.byteToInt(byteList.get(11));
        FormatToken.PurificationTDS0=ByteUtils.byteToInt(byteList.get(12));
        FormatToken.PurificationTDS1=ByteUtils.byteToInt(byteList.get(13));


        FormatToken.WorkState=ByteUtils.byteToInt(byteList.get(14));
        byte[] makewater=new byte[2];
        makewater[0]=byteList.get(15);
        makewater[1]=byteList.get(16);
        FormatToken.MakeWater=ByteUtils.byte2ToInt(makewater);
        return true;
    }

}
