package com.msht.watersystem.utilpackage;

import android.content.Context;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
/**
 * Demo class
 *
 * @author  hong
 * @date 2017/12/10
 */
public class ConsumeInformationUtils {
    public static void saveConsumptionInformationToFormatInformation(ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()!=0){
            FormatInformationBean.BusinessType=ByteUtils.byteToInt(byteArrayList.get(0));
            if (FormatInformationBean.BusinessType==1){
                FormatInformationBean.ConsumptionType=3;
            }else if (FormatInformationBean.BusinessType==2){
                //消费类型
                FormatInformationBean.ConsumptionType=5;
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
            FormatInformationBean.phoneType=account;
            try {
                FormatInformationBean.StringCardNo= getBigNumber(account);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] byteBalance=new byte[4];
            byteBalance[0]=byteArrayList.get(9);
            byteBalance[1]=byteArrayList.get(10);
            byteBalance[2]=byteArrayList.get(11);
            byteBalance[3]=byteArrayList.get(12);
           // FormatInformationBean.Balance=ByteUtils.byte4ToInt(Balance);
            FormatInformationBean.AppBalance=ByteUtils.byte4ToInt(byteBalance);

            byte[] recharge=new byte[4];
            recharge[0]=byteArrayList.get(13);
            recharge[1]=byteArrayList.get(14);
            recharge[2]=byteArrayList.get(15);
            recharge[3]=byteArrayList.get(16);
            FormatInformationBean.outWaterAmount=ByteUtils.byte4ToInt(recharge);
            FormatInformationBean.rechargeAmount=ByteUtils.byte4ToInt(recharge);

            byte[] byteDeviceId=new byte[4];
            byteDeviceId[0]=byteArrayList.get(21);
            byteDeviceId[1]=byteArrayList.get(22);
            byteDeviceId[2]=byteArrayList.get(23);
            byteDeviceId[3]=byteArrayList.get(24);
            FormatInformationBean.DeviceId=ByteUtils.byte4ToInt(byteDeviceId);
            FormatInformationBean.PriceNum=ByteUtils.byteToInt(byteArrayList.get(25));
            FormatInformationBean.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(26));
            FormatInformationBean.WaterNum=ByteUtils.byteToInt(byteArrayList.get(27));
            byte[] water=new byte[2];
            water[0]=byteArrayList.get(28);
            water[1]=byteArrayList.get(29);
            FormatInformationBean.Waterweight=ByteUtils.byte2ToInt(water);

            byte[] orderNo=new  byte[8];
            orderNo[0]=byteArrayList.get(30);
            orderNo[1]=byteArrayList.get(31);
            orderNo[2]=byteArrayList.get(32);
            orderNo[3]=byteArrayList.get(33);
            orderNo[4]=byteArrayList.get(34);
            orderNo[5]=byteArrayList.get(35);
            orderNo[6]=byteArrayList.get(36);
            orderNo[7]=byteArrayList.get(37);
            FormatInformationBean.orderType=orderNo;
            FormatInformationBean.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(38));
            try {
                FormatInformationBean.OrderNoString= getBigNumber(orderNo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean calculateRecharge(ArrayList<Byte> byteArrayList){
        try{
            if (byteArrayList!=null&&byteArrayList.size()>=ConstantUtil.BUSINESS_MAX_SIZE){
                FormatInformationBean.BusinessType=ByteUtils.byteToInt(byteArrayList.get(0));
                byte[] recharge=new byte[4];
                recharge[0]=byteArrayList.get(13);
                recharge[1]=byteArrayList.get(14);
                recharge[2]=byteArrayList.get(15);
                recharge[3]=byteArrayList.get(16);
                FormatInformationBean.rechargeAmount=ByteUtils.byte4ToInt(recharge);
                return true;
            }else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public static String getBigNumber(byte[] numByte)throws Exception{
        BigInteger bigInteger=new BigInteger(numByte);
        long intNum=bigInteger.longValue();
        return String.valueOf(intNum);
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
    public static boolean controlModel(Context context, ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()>=ConstantUtil.SET_MODE_MAX_SIZE){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteArrayList.get(0);
            deviceId[1]=byteArrayList.get(1);
            deviceId[2]=byteArrayList.get(2);
            deviceId[3]=byteArrayList.get(3);
            FormatInformationBean.FreeDeviceNo=ByteUtils.byte4ToInt(deviceId);
            FormatInformationBean.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatInformationBean.ShowTDS=ByteUtils.byteToInt(byteArrayList.get(5));
            FormatInformationBean.PriceNum=ByteUtils.byteToInt(byteArrayList.get(6));
            FormatInformationBean.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(7));
            FormatInformationBean.WaterNum=ByteUtils.byteToInt(byteArrayList.get(8));
            byte[] deductAmount=new byte[2];
            deductAmount[0]=byteArrayList.get(9);
            deductAmount[1]=byteArrayList.get(10);
            FormatInformationBean.DeductAmount=ByteUtils.byte2ToInt(deductAmount);
            CachePreferencesUtil.putIntData(context,CachePreferencesUtil.WATER_OUT_TIME,FormatInformationBean.OutWaterTime);
            CachePreferencesUtil.putIntData(context,CachePreferencesUtil.WATER_NUM,FormatInformationBean.WaterNum);
            CachePreferencesUtil.getIntData(context,CachePreferencesUtil.DEDUCT_AMOUNT,FormatInformationBean.DeductAmount);
            CachePreferencesUtil.getIntData(context,CachePreferencesUtil.PRICE,FormatInformationBean.PriceNum);
            CachePreferencesUtil.putChargeMode(context,CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
            CachePreferencesUtil.putChargeMode(context,CachePreferencesUtil.SHOW_TDS, FormatInformationBean.ShowTDS);
            return true;
        }else {
            return false;
        }
    }
}
