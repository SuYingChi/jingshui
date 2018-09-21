package com.msht.watersystem.Utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by hong on 2017/11/20.
 */

public class DataCalculateUtils {
    public static int MealsType(ArrayList<Byte> byteList){
        int mealType=ByteUtils.byteToInt(byteList.get(0));
        return mealType;
    }
    public static double Meal_ONE(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(1);
        mael[1]=byteList.get(2);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_GIVE1(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(3);
        mael[1]=byteList.get(4);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_TWO(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(5);
        mael[1]=byteList.get(6);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_GIVE2(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(7);
        mael[1]=byteList.get(8);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_THREE(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(9);
        mael[1]=byteList.get(10);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_GIVE3(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(11);
        mael[1]=byteList.get(12);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_FOUR(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(13);
        mael[1]=byteList.get(14);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_GIVE4(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(15);
        mael[1]=byteList.get(16);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_FIVE(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(17);
        mael[1]=byteList.get(18);
        int amount=ByteUtils.byte2ToInt(mael);
        double doubleamount=amount/100.0;
        return doubleamount;
    }
    public static double Meal_GIVE5(ArrayList<Byte> byteList){
        byte[] mael=new byte[2];
        mael[0]=byteList.get(19);
        mael[1]=byteList.get(20);
        int amount=ByteUtils.byte2ToInt(mael);
        return amount/100.0;
    }

    public static double getTwoDecimal(double amount) {   //保留两位小数
        BigDecimal bg=new BigDecimal(amount);
        return bg.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    public static double getWaterVolume(int volume,int time){
        double outWaterNum=volume*1.0;
        double outWaterTime=time*1.0;
        double perSecond=outWaterNum/outWaterTime;
        double perdouble=DataCalculateUtils.getTwoDecimal(perSecond);
        return perSecond;
    }
    public static double getWaterPrice(int price){
        double priceNum=price*1.0;
        return 1.0/priceNum;
    }
    public static boolean getBusinessData(ArrayList<Byte> byteArrayList){

        if (byteArrayList!=null&&byteArrayList.size()!=0){
            FormatInformationBean.BusinessType=ByteUtils.byteToInt(byteArrayList.get(0));
            byte[] account=new byte[8];
            account[0]=byteArrayList.get(1);
            account[1]=byteArrayList.get(2);
            account[2]=byteArrayList.get(3);
            account[3]=byteArrayList.get(4);
            account[4]=byteArrayList.get(5);
            account[5]=byteArrayList.get(6);
            account[6]=byteArrayList.get(7);
            account[7]=byteArrayList.get(8);
            try {
                FormatInformationBean.StringCardNo= getBigNumberData(account);
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] BalanceByte=new byte[2];
            BalanceByte[0]=byteArrayList.get(9);
            BalanceByte[1]=byteArrayList.get(10);
            FormatInformationBean.Balance=ByteUtils.byte2ToInt(BalanceByte);

            byte[] AmountByte=new byte[2];
            AmountByte[0]=byteArrayList.get(11);
            AmountByte[1]=byteArrayList.get(12);
            FormatInformationBean.ConsumptionAmount=ByteUtils.byte2ToInt(AmountByte);

            byte[] AfterByte=new byte[2];
            AfterByte[0]=byteArrayList.get(13);
            AfterByte[1]=byteArrayList.get(14);
            FormatInformationBean.AfterAmount=ByteUtils.byte2ToInt(AfterByte);

            byte[] mDeviceId=new byte[4];
            mDeviceId[0]=byteArrayList.get(15);
            mDeviceId[1]=byteArrayList.get(16);
            mDeviceId[2]=byteArrayList.get(17);
            mDeviceId[3]=byteArrayList.get(18);
            FormatInformationBean.DeviceId=ByteUtils.byte4ToInt(mDeviceId);
            FormatInformationBean.PriceNum=ByteUtils.byteToInt(byteArrayList.get(19));
            FormatInformationBean.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(20));
            FormatInformationBean.WaterNum=ByteUtils.byteToInt(byteArrayList.get(21));
            return true;
        }else {
            return false;
        }

    }

    public static String getBigNumberData(byte[] account)throws Exception{
        BigInteger bigInteger=new BigInteger(account);
        long intNum=bigInteger.longValue();
        return String.valueOf(intNum);
    }
    public static byte[] ArrayToByte(ArrayList<Byte> byteList){
        int size=byteList.size();
        byte[] byteArray=new byte[size];
        for(int i=0;i<size;i++){
            byteArray[i]=byteList.get(i);
        }
        return  byteArray;
    }
    //十进制转八位二进制
    public static String intToBinary(int input) {
        //11110000
        String binaryString = Integer.toBinaryString(input);
        int binaryInt = Integer.parseInt(binaryString);
        return String.format(Locale.CHINA,"%08d",binaryInt);
    }
    //判断Flag位
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
    //判断Flag位
    public static boolean isRechargeData(String s,int index1,int index2){
        if (s.length()>=8){
            char charIndex1=s.charAt(index1);
            char charIndex2=s.charAt(index2);
            String stringIndex1=String.valueOf(charIndex1);
            String stringIndex2=String.valueOf(charIndex2);
            if (stringIndex1.equals(ConstantUtil.ONE_VALUE)||stringIndex2.equals(ConstantUtil.ONE_VALUE)){
                return false;
            }else {
                return true;
            }
        }else {
            return true;
        }
    }
}
