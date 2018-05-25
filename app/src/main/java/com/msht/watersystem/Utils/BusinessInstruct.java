package com.msht.watersystem.Utils;

import android.content.Context;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by hong on 2017/12/10.
 */

public class BusinessInstruct {
    public static boolean CalaculateBusiness(ArrayList<Byte> byteArrayList){
        if (byteArrayList.size()!=0&&byteArrayList!=null){
            FormatToken.BusinessType=ByteUtils.byteToInt(byteArrayList.get(0));
            if (FormatToken.BusinessType==1){
                FormatToken.ConsumptionType=3;
            }else if (FormatToken.BusinessType==2){
                FormatToken.ConsumptionType=5;  //消费类型
            }
            byte[] account=new  byte[8];
            account[0]=byteArrayList.get(1);
            account[1]=byteArrayList.get(2);
            account[2]=byteArrayList.get(3);
            account[3]=byteArrayList.get(4);
            account[4]=byteArrayList.get(5);
            account[5]=byteArrayList.get(6);
            account[6]=byteArrayList.get(7);
            account[7]=byteArrayList.get(8);
            FormatToken.phoneType=account;
            try {
                FormatToken.StringCardNo=getbigNumber(account);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] Balance=new byte[4];
            Balance[0]=byteArrayList.get(9);
            Balance[1]=byteArrayList.get(10);
            Balance[2]=byteArrayList.get(11);
            Balance[3]=byteArrayList.get(12);
           // FormatToken.Balance=ByteUtils.byte4ToInt(Balance);
            FormatToken.AppBalance=ByteUtils.byte4ToInt(Balance);

            byte[] recharge=new byte[4];
            recharge[0]=byteArrayList.get(13);
            recharge[1]=byteArrayList.get(14);
            recharge[2]=byteArrayList.get(15);
            recharge[3]=byteArrayList.get(16);
            FormatToken.rechargeAmount=ByteUtils.byte4ToInt(recharge);

            byte[] DeviceId=new byte[4];
            DeviceId[0]=byteArrayList.get(21);
            DeviceId[1]=byteArrayList.get(22);
            DeviceId[2]=byteArrayList.get(23);
            DeviceId[3]=byteArrayList.get(24);
            FormatToken.DeviceId=ByteUtils.byte4ToInt(DeviceId);
            FormatToken.PriceNum=ByteUtils.byteToInt(byteArrayList.get(25));
            FormatToken.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(26));
            FormatToken.WaterNum=ByteUtils.byteToInt(byteArrayList.get(27));
            byte[] water=new byte[2];
            water[0]=byteArrayList.get(28);
            water[1]=byteArrayList.get(29);
            FormatToken.Waterweight=ByteUtils.byte2ToInt(water);

            byte[] orderNo=new  byte[8];
            orderNo[0]=byteArrayList.get(30);
            orderNo[1]=byteArrayList.get(31);
            orderNo[2]=byteArrayList.get(32);
            orderNo[3]=byteArrayList.get(33);
            orderNo[4]=byteArrayList.get(34);
            orderNo[5]=byteArrayList.get(35);
            orderNo[6]=byteArrayList.get(36);
            orderNo[7]=byteArrayList.get(37);
            FormatToken.orderType=orderNo;
            try {
                FormatToken.OrderNoString=getbigNumber(orderNo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }else {
            return false;
        }
    }
    public static boolean CalaculateRecharge(ArrayList<Byte> byteArrayList){
        if (byteArrayList.size()!=0&&byteArrayList!=null){
            FormatToken.BusinessType=ByteUtils.byteToInt(byteArrayList.get(0));
            byte[] recharge=new byte[4];
            recharge[0]=byteArrayList.get(13);
            recharge[1]=byteArrayList.get(14);
            recharge[2]=byteArrayList.get(15);
            recharge[3]=byteArrayList.get(16);
            FormatToken.rechargeAmount=ByteUtils.byte4ToInt(recharge);
            return true;
        }else {
            return false;
        }
    }
    public static String getbigNumber(byte[] numbyte)throws Exception{
        BigInteger bigInteger=new BigInteger(numbyte);
        long intNum=bigInteger.longValue();
        String Account=String.valueOf(intNum);
        return Account;
    }
    public static  byte[] settleData(byte[] settledata,byte[] settleorder){
        byte[] data=new byte[38];
        for (int i=0;i<38;i++){
            if (i==0){
                data[i]=(byte)0x01;
            }else if (i==1){
                data[i]=settledata[0];
            }else if (i==2){
                data[i]=settledata[1];
            }else if (i==3){
                data[i]=settledata[2];
            }else if (i==4){
                data[i]=settledata[3];
            }else if (i==5){
                data[i]=settledata[4];
            }else if (i==6){
                data[i]=settledata[5];
            }else if (i==7){
                data[i]=settledata[6];
            }else if (i==8){
                data[i]=settledata[7];
            }else if (i==30){
                data[i]=settleorder[0];
            }else if (i==31){
                data[i]=settleorder[1];
            }else if (i==32){
                data[i]=settleorder[2];
            }else if (i==33){
                data[i]=settleorder[3];
            }else if (i==34){
                data[i]=settleorder[4];
            }else if (i==35){
                data[i]=settleorder[5];
            }else if (i==36){
                data[i]=settleorder[6];
            }else if (i==37){
                data[i]=settleorder[7];
            }else {
                data[i]=(byte)0x00;
            }
        }
        return data;
    }
    public static boolean ControlModel(Context context,ArrayList<Byte> byteArrayList){

        if (byteArrayList.size()!=0&&byteArrayList!=null){
            byte[] DeviceId=new byte[4];
            DeviceId[0]=byteArrayList.get(0);
            DeviceId[1]=byteArrayList.get(1);
            DeviceId[2]=byteArrayList.get(2);
            DeviceId[3]=byteArrayList.get(3);
            FormatToken.FreeDeviceNo=ByteUtils.byte4ToInt(DeviceId);
            FormatToken.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatToken.ShowTDS=ByteUtils.byteToInt(byteArrayList.get(5));
            CachePreferencesUtil.putChargeMode(context,CachePreferencesUtil.ChargeMode,FormatToken.ChargeMode);
            CachePreferencesUtil.putChargeMode(context,CachePreferencesUtil.ShowTds,FormatToken.ShowTDS);
            return true;
        }else {
            return false;
        }
    }
}
