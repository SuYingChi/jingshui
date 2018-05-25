package com.msht.watersystem.Utils;

import android.content.Context;

import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;

import java.util.ArrayList;

/**
 * Created by hong on 2017/10/28.
 */
public class ControlInstructUtil {
    public static boolean ControlInstruct(ArrayList<Byte> byteArrayList){

        byte[] DeviceId=new byte[4];
        DeviceId[0]=byteArrayList.get(0);
        DeviceId[1]=byteArrayList.get(1);
        DeviceId[2]=byteArrayList.get(2);
        DeviceId[3]=byteArrayList.get(3);
        FormatToken.DeviceId=ByteUtils.byte4ToInt(DeviceId);

        FormatToken.StringCardNo=CardData(byteArrayList);
        byte[] TimeByte=new byte[6];
        TimeByte[0]=byteArrayList.get(9);
        TimeByte[1]=byteArrayList.get(10);
        TimeByte[2]=byteArrayList.get(11);
        TimeByte[3]=byteArrayList.get(12);
        TimeByte[4]=byteArrayList.get(13);
        TimeByte[5]=byteArrayList.get(14);
        FormatToken.TriggerTime=ByteUtils.byte6ToInt(TimeByte);
        FormatToken.ConsumptionType=ByteUtils.byteToInt(byteArrayList.get(15));

        byte[] BalanceByte=new byte[2];
        BalanceByte[0]=byteArrayList.get(16);
        BalanceByte[1]=byteArrayList.get(17);
        FormatToken.Balance=ByteUtils.byte2ToInt(BalanceByte);

        FormatToken.AmountType=ByteUtils.byteToInt(byteArrayList.get(18));//金额类型

        byte[] AmountByte=new byte[2];
        AmountByte[0]=byteArrayList.get(19);
        AmountByte[1]=byteArrayList.get(20);
        FormatToken.ConsumptionAmount=ByteUtils.byte2ToInt(AmountByte);

        byte[] AfterByte=new byte[2];
        AfterByte[0]=byteArrayList.get(21);
        AfterByte[1]=byteArrayList.get(22);
        FormatToken.AfterAmount=ByteUtils.byte2ToInt(AfterByte);

        byte[] WaterByte=new byte[2];
        WaterByte[0]=byteArrayList.get(23);
        WaterByte[1]=byteArrayList.get(24);
        FormatToken.WaterYield=ByteUtils.byte2ToInt(WaterByte);

        byte[] RechargeByte=new byte[2];
        RechargeByte[0]=byteArrayList.get(25);
        RechargeByte[1]=byteArrayList.get(26);
        FormatToken.RechargeValue=ByteUtils.byte2ToInt(RechargeByte);

        byte[] InstallIdByte=new byte[2];
        InstallIdByte[0]=byteArrayList.get(27);
        InstallIdByte[1]=byteArrayList.get(28);
        FormatToken.MotifyInstall=ByteUtils.byte2ToInt(InstallIdByte);

        FormatToken.MotifyPrice=ByteUtils.byteToInt(byteArrayList.get(29));  //修改单价
        FormatToken.MotifyOzoneTime=ByteUtils.byteToInt(byteArrayList.get(30));
        FormatToken.Switch=ByteUtils.byteToInt(byteArrayList.get(31));
        FormatToken.EnableGet=ByteUtils.byteToInt(byteArrayList.get(32));
        FormatToken.SetFilter=ByteUtils.byteToInt(byteArrayList.get(33));
        FormatToken.SetFilterLevel=ByteUtils.byteToInt(byteArrayList.get(34));

        byte[] DeductByte=new byte[2];
        DeductByte[0]=byteArrayList.get(35);
        DeductByte[1]=byteArrayList.get(36);
        FormatToken.SetDeductAmount=ByteUtils.byte2ToInt(DeductByte);
        FormatToken.Blacklist=ByteUtils.byteToInt(byteArrayList.get(37));
        FormatToken.KeyCode=ByteUtils.byteToInt(byteArrayList.get(38));
        FormatToken.Updateflag1=ByteUtils.byteToInt(byteArrayList.get(42));
        FormatToken.Updateflag2=ByteUtils.byteToInt(byteArrayList.get(43));
        FormatToken.Updateflag3=ByteUtils.byteToInt(byteArrayList.get(44));
        FormatToken.Updateflag4=ByteUtils.byteToInt(byteArrayList.get(45));
        FormatToken.Updateflag5=ByteUtils.byteToInt(byteArrayList.get(46));
        FormatToken.Updateflag6=ByteUtils.byteToInt(byteArrayList.get(47));
        return true;
    }
    private static String CardData(ArrayList<Byte> byteArrayList) {
        String stringNo="";
        int cardType=ByteUtils.byteToInt(byteArrayList.get(4));
        if (cardType<10){
            stringNo="0"+String.valueOf(cardType);
        }else {
            stringNo=String.valueOf(cardType);
        }
        byte[] CardByte=new byte[5];
        CardByte[0]=byteArrayList.get(5);
        CardByte[1]=byteArrayList.get(6);
        CardByte[2]=byteArrayList.get(7);
        CardByte[3]=byteArrayList.get(8);
        FormatToken.CardNo=ByteUtils.byte4ToInt(CardByte);
        String addzero=String.valueOf(ByteUtils.byte4ToInt(CardByte));
        return stringNo+AddZero(addzero);
    }
    private static String AddZero(String addzero) {
        String zeroString=addzero;
        if (addzero.length()<8){
            for (int i=0;i<8-addzero.length();i++){
                zeroString="0"+zeroString;
            }
        }
       return zeroString ;
    }
    public static boolean EquipmentData(ArrayList<Byte> byteArrayList){
        byte[] DeviceId=new byte[4];
        DeviceId[0]=byteArrayList.get(0);
        DeviceId[1]=byteArrayList.get(1);
        DeviceId[2]=byteArrayList.get(2);
        DeviceId[3]=byteArrayList.get(3);
        FormatToken.DeviceId=ByteUtils.byte4ToInt(DeviceId);
        FormatToken.PriceNum=ByteUtils.byteToInt(byteArrayList.get(4));
        FormatToken.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(5));
        FormatToken.WaterNum=ByteUtils.byteToInt(byteArrayList.get(6));
        FormatToken.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(7));
        FormatToken.ShowTDS=ByteUtils.byteToInt(byteArrayList.get(8));

        return true;
    }
    public static byte[] SETTLE(){
        byte[] Control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==38){
                Control[i]=(byte)0x03;
            }else if (i==46){
                Control[i]=(byte)0x40;
            }else {
                Control[i]=(byte)0x00;
            }
        }
        return Control;
    }
    public static byte[] setBusinessType01(){
        byte[] consumption=new byte[4];
        consumption[0]=VariableUtil.byteArray.get(13);
        consumption[1]=VariableUtil.byteArray.get(14);
        consumption[2]=VariableUtil.byteArray.get(15);
        consumption[3]=VariableUtil.byteArray.get(16);
        int Amount=ByteUtils.byte4ToInt(consumption);
        byte[] Twobyte=ByteUtils.intToByte2(Amount);
        byte[] Control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==0){
                Control[i]=VariableUtil.byteArray.get(21);  //设备号
            }else if (i==1){
                Control[i]=VariableUtil.byteArray.get(22);
            }else if (i==2){
                Control[i]=VariableUtil.byteArray.get(23);
            }else if (i==3){
                Control[i]=VariableUtil.byteArray.get(24);
            }else if (i==15){
                Control[i]=(byte)0x03;   //
            }else if (i==16){
                Control[i]=Twobyte[0];
            }else if (i==17){
                Control[i]=Twobyte[1];
            }else if (i==29){
                Control[i]=VariableUtil.byteArray.get(25);  //单价
            }else if (i==42){         //flag,设备号变化
                Control[i]=(byte)0x0f;
            } else if (i==43){
                Control[i]=(byte)0x80;      //消费类型
            }else if (i==44){
                Control[i]=(byte)0x03;
            }else if (i==45){
                Control[i]=(byte)0x20;      //单价，
            }else {
                Control[i]=(byte)0x00;
            }
        }
        return Control;
    }
    public static byte[] setBusinessType02(){
        byte[] consumption=new byte[4];
        consumption[0]=VariableUtil.byteArray.get(13);
        consumption[1]=VariableUtil.byteArray.get(14);
        consumption[2]=VariableUtil.byteArray.get(15);
        consumption[3]=VariableUtil.byteArray.get(16);
        int Amount=ByteUtils.byte4ToInt(consumption);
        byte[] Twobyte=ByteUtils.intToByte2(Amount);
        byte[] Control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==15){
                Control[i]=(byte)0x05;
            }else if (i==18){
                Control[i]=(byte)0x01;
            }else if (i==19){
                Control[i]=Twobyte[0];
            }else if (i==20){
                Control[i]=Twobyte[1];
            }else if (i==29){
                Control[i]=VariableUtil.byteArray.get(25);  //单价
            } else if (i==43){
                Control[i]=(byte)0x80;
            }else if (i==44){
                Control[i]=(byte)0x1c;      //本次消费金额，
            } else if (i==45){
                Control[i]=(byte)0x20;      //单价，
            } else {
                Control[i]=(byte)0x00;
            }
        }
        return Control;
    }
    public static byte[] setDataTimeByte(){
        byte[] date=new byte[16];
        for (int i=0;i<16;i++){
            if (i==0){
                date[i]=(byte)0x01;
            }else {
                date[i]=(byte)0x00;
            }
        }
        return date;
    }
    public static boolean TimeInstruct(ArrayList<Byte> byteArrayList){
        if (byteArrayList.size()!=0&&byteArrayList!=null){
            FormatToken.TimeType=ByteUtils.byteToInt(byteArrayList.get(0));
            FormatToken.Year=ByteUtils.byteToInt(byteArrayList.get(1))+2000;
            FormatToken.Month=ByteUtils.byteToInt(byteArrayList.get(2));
            FormatToken.Day=ByteUtils.byteToInt(byteArrayList.get(3));
            FormatToken.Hour=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatToken.Minute=ByteUtils.byteToInt(byteArrayList.get(5));
            FormatToken.Second=ByteUtils.byteToInt(byteArrayList.get(6));
            FormatToken.TimeWeek=ByteUtils.byteToInt(byteArrayList.get(7));
            FormatToken.TimeZone=ByteUtils.byteToInt(byteArrayList.get(8));
            return true;
        }else {
            return false;
        }
    }
}