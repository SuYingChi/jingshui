package com.msht.watersystem.utilpackage;

import android.util.Log;

import java.util.ArrayList;

/**
 *
 * @author hong
 * @date 2017/10/28
 */
public class FormatInformationUtil {
    private static final int CONTROL_INSTRUCT_LENGTH=48;
    /**
     * 解析控制指令
     * @param byteArrayList 控制指令
     */
    public static  void saveCom1ReceivedDataToFormatInformation(ArrayList<Byte> byteArrayList){

        if (byteArrayList!=null&&byteArrayList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteArrayList.get(0);
            deviceId[1]=byteArrayList.get(1);
            deviceId[2]=byteArrayList.get(2);
            deviceId[3]=byteArrayList.get(3);
            FormatInformationBean.DeviceId=ByteUtils.byte4ToInt(deviceId);

            FormatInformationBean.StringCardNo= saveIcCardCodeDataToFormatInformation(byteArrayList);
            byte[] timeByte=new byte[6];
            timeByte[0]=byteArrayList.get(9);
            timeByte[1]=byteArrayList.get(10);
            timeByte[2]=byteArrayList.get(11);
            timeByte[3]=byteArrayList.get(12);
            timeByte[4]=byteArrayList.get(13);
            timeByte[5]=byteArrayList.get(14);
            FormatInformationBean.TriggerTime=ByteUtils.byte6ToInt(timeByte);
            FormatInformationBean.ConsumptionType=ByteUtils.byteToInt(byteArrayList.get(15));

            byte[] balanceByte=new byte[2];
            balanceByte[0]=byteArrayList.get(16);
            balanceByte[1]=byteArrayList.get(17);
            FormatInformationBean.Balance=ByteUtils.byte2ToInt(balanceByte);
            //金额类型
            FormatInformationBean.AmountType=ByteUtils.byteToInt(byteArrayList.get(18));

            byte[] amountByte=new byte[2];
            amountByte[0]=byteArrayList.get(19);
            amountByte[1]=byteArrayList.get(20);
            FormatInformationBean.ConsumptionAmount=ByteUtils.byte2ToInt(amountByte);

            byte[] afterByte=new byte[2];
            afterByte[0]=byteArrayList.get(21);
            afterByte[1]=byteArrayList.get(22);
            FormatInformationBean.AfterAmount=ByteUtils.byte2ToInt(afterByte);

            byte[] waterByte=new byte[2];
            waterByte[0]=byteArrayList.get(23);
            waterByte[1]=byteArrayList.get(24);
            FormatInformationBean.WaterYield=ByteUtils.byte2ToInt(waterByte);

            byte[] rechargeByte=new byte[2];
            rechargeByte[0]=byteArrayList.get(25);
            rechargeByte[1]=byteArrayList.get(26);
            FormatInformationBean.RechargeValue=ByteUtils.byte2ToInt(rechargeByte);

            byte[] installIdByte=new byte[2];
            installIdByte[0]=byteArrayList.get(27);
            installIdByte[1]=byteArrayList.get(28);
            FormatInformationBean.MotifyInstall=ByteUtils.byte2ToInt(installIdByte);
            //修改单价
            FormatInformationBean.MotifyPrice=ByteUtils.byteToInt(byteArrayList.get(29));
            FormatInformationBean.MotifyOzoneTime=ByteUtils.byteToInt(byteArrayList.get(30));
            FormatInformationBean.Switch=ByteUtils.byteToInt(byteArrayList.get(31));
            FormatInformationBean.EnableGet=ByteUtils.byteToInt(byteArrayList.get(32));
            FormatInformationBean.SetFilter=ByteUtils.byteToInt(byteArrayList.get(33));
            FormatInformationBean.SetFilterLevel=ByteUtils.byteToInt(byteArrayList.get(34));

            byte[] deductByte=new byte[2];
            deductByte[0]=byteArrayList.get(35);
            deductByte[1]=byteArrayList.get(36);
            FormatInformationBean.SetDeductAmount=ByteUtils.byte2ToInt(deductByte);
            FormatInformationBean.blackCard=ByteUtils.byteToInt(byteArrayList.get(37));
            FormatInformationBean.KeyCode=ByteUtils.byteToInt(byteArrayList.get(38));
            FormatInformationBean.Updateflag1=ByteUtils.byteToInt(byteArrayList.get(42));
            FormatInformationBean.Updateflag2=ByteUtils.byteToInt(byteArrayList.get(43));
            FormatInformationBean.Updateflag3=ByteUtils.byteToInt(byteArrayList.get(44));
            FormatInformationBean.Updateflag4=ByteUtils.byteToInt(byteArrayList.get(45));
            FormatInformationBean.Updateflag5=ByteUtils.byteToInt(byteArrayList.get(46));
            FormatInformationBean.Updateflag6=ByteUtils.byteToInt(byteArrayList.get(47));
        }
    }
    /**
     * 〈状态指令解析
     * 〈功能详细描述〉
     * @param byteList  状态指令
     * @return  
     */
    public static void saveStatusInformationToFormatInformation(ArrayList<Byte> byteList){
        if (byteList!=null&&byteList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteList.get(0);
            deviceId[1]=byteList.get(1);
            deviceId[2]=byteList.get(2);
            deviceId[3]=byteList.get(3);
            FormatInformationBean.DeviceId=ByteUtils.byte4ToInt(deviceId);
            byte[] installId=new byte[2];
            installId[0]=byteList.get(4);
            installId[1]=byteList.get(5);
            FormatInformationBean.InstallId=ByteUtils.byte2ToInt(installId);
            FormatInformationBean.Ozonetime=ByteUtils.byteToInt(byteList.get(6));
            FormatInformationBean.PriceNum=ByteUtils.byteToInt(byteList.get(7));
            FormatInformationBean.humidity=ByteUtils.byteToInt(byteList.get(8));
            FormatInformationBean.temperature=ByteUtils.byteToInt(byteList.get(9));
            byte[] origin=new byte[2];
            origin[0]=byteList.get(10);
            origin[1]=byteList.get(11);
            FormatInformationBean.OriginTDS=ByteUtils.byte2ToInt(origin);
            byte[] purification=new byte[2];
            purification[0]=byteList.get(12);
            purification[1]=byteList.get(13);
            if (isMakeWater(ByteUtils.byteToInt(byteList.get(ConstantUtil.FOURTEEN)))){
                FormatInformationBean.PurificationTDS=ByteUtils.byte2ToInt(purification);
            }
           // FormatInformationBean.PurificationTDS=ByteUtils.byte2ToInt(purification);
            FormatInformationBean.OriginTDS0=ByteUtils.byteToInt(byteList.get(10));
            FormatInformationBean.OriginTDS1=ByteUtils.byteToInt(byteList.get(11));
            FormatInformationBean.PurificationTDS0=ByteUtils.byteToInt(byteList.get(12));
            FormatInformationBean.PurificationTDS1=ByteUtils.byteToInt(byteList.get(13));
            FormatInformationBean.WorkState=ByteUtils.byteToInt(byteList.get(14));
            byte[] makewater=new byte[2];
            makewater[0]=byteList.get(15);
            makewater[1]=byteList.get(16);
            FormatInformationBean.MakeWater=ByteUtils.byte2ToInt(makewater);
        }
    }

    /**
     * Ic卡编码数据转换
     * @param byteArrayList
     * @return
     */
    private static String saveIcCardCodeDataToFormatInformation(ArrayList<Byte> byteArrayList) {
        String stringNo="";
        int cardType=ByteUtils.byteToInt(byteArrayList.get(4));
        if (cardType<10){
            stringNo="0"+String.valueOf(cardType);
        }else {
            stringNo=String.valueOf(cardType);
        }
        byte[] cardByte=new byte[5];
        cardByte[0]=byteArrayList.get(5);
        cardByte[1]=byteArrayList.get(6);
        cardByte[2]=byteArrayList.get(7);
        cardByte[3]=byteArrayList.get(8);
        FormatInformationBean.CardNo=ByteUtils.byte4ToInt(cardByte);
        String addzero=String.valueOf(ByteUtils.byte4ToInt(cardByte));
        return stringNo+ addZeroPrefix(addzero);
    }
    private static String addZeroPrefix(String addZero) {
        String zeroString=addZero;
        if (addZero.length()<8){
            for (int i=0;i<8-addZero.length();i++){
                zeroString="0"+zeroString;
            }
        }
       return zeroString ;
    }
    public static void saveDeviceInformationToFormatInformation(ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteArrayList.get(0);
            deviceId[1]=byteArrayList.get(1);
            deviceId[2]=byteArrayList.get(2);
            deviceId[3]=byteArrayList.get(3);
            FormatInformationBean.DeviceId=ByteUtils.byte4ToInt(deviceId);
            FormatInformationBean.PriceNum=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatInformationBean.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(5));
            FormatInformationBean.WaterNum=ByteUtils.byteToInt(byteArrayList.get(6));
            FormatInformationBean.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(7));
            FormatInformationBean.ShowTDS=ByteUtils.byteToInt(byteArrayList.get(8));

        }
    }
    public static byte[] settle(){
        byte[] control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==38){
                control[i]=(byte)0x03;
            }else if (i==46){
                control[i]=(byte)0x40;
            }else {
                control[i]=(byte)0x00;
            }
        }
        return control;
    }
    public static byte[] setEquipmentParameter(byte abyte){
        byte[] parameter=new byte[48];
        for (int i=0;i<48;i++){
            if (i==29){
                parameter[i]=abyte;
            }else if (i==45){
                parameter[i]=(byte)0x20;
            }else {
                parameter[i]=(byte)0x00;
            }
        }
        return parameter;
    }

    public static byte[] setConsumeType01(){
        byte[] consumption=new byte[4];
        consumption[0]=VariableUtil.byteArray.get(13);
        consumption[1]=VariableUtil.byteArray.get(14);
        consumption[2]=VariableUtil.byteArray.get(15);
        consumption[3]=VariableUtil.byteArray.get(16);
        int amount=ByteUtils.byte4ToInt(consumption);
        byte[] twobyte=ByteUtils.intToByte2(amount);
        byte[] control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==0){
                //设备号
                control[i]=VariableUtil.byteArray.get(21);
            }else if (i==1){
                control[i]=VariableUtil.byteArray.get(22);
            }else if (i==2){
                control[i]=VariableUtil.byteArray.get(23);
            }else if (i==3){
                control[i]=VariableUtil.byteArray.get(24);
            }else if (i==15){
                control[i]=(byte)0x03;
            }else if (i==16){
                control[i]=twobyte[0];
            }else if (i==17){
                control[i]=twobyte[1];
            }else if (i==18){
                control[i]=(byte)0x01;
            }else if (i==29){
                //单价
                control[i]=VariableUtil.byteArray.get(25);
                //flag,设备号变化
            }else if (i==42){
                control[i]=(byte)0x0f;
            } else if (i==43){
                //消费类型
                control[i]=(byte)0x80;
            }else if (i==44){
                control[i]=(byte)0x03;
            }else if (i==45){
                //单价，
                control[i]=(byte)0x20;
            }else {
                control[i]=(byte)0x00;
            }
        }
        return control;
    }
    public static byte[] setConsumeType02(){
        byte[] consumption=new byte[4];
        consumption[0]=VariableUtil.byteArray.get(13);
        consumption[1]=VariableUtil.byteArray.get(14);
        consumption[2]=VariableUtil.byteArray.get(15);
        consumption[3]=VariableUtil.byteArray.get(16);
        int amount=ByteUtils.byte4ToInt(consumption);
        byte[] twobyte=ByteUtils.intToByte2(amount);
        byte[] control=new byte[48];
        for (int i=0;i<48;i++){
            if (i==0){
                //设备号
                control[i]=VariableUtil.byteArray.get(21);
            }else if (i==1){
                control[i]=VariableUtil.byteArray.get(22);
            }else if (i==2){
                control[i]=VariableUtil.byteArray.get(23);
            }else if (i==3){
                control[i]=VariableUtil.byteArray.get(24);
            }else if (i==15){
                control[i]=(byte)0x03;
            }else if (i==16){
                control[i]=twobyte[0];
            }else if (i==17){
                control[i]=twobyte[1];
            }else if (i==18){
                control[i]=(byte)0x01;
            }else if (i==19){
                control[i]=twobyte[0];
            }else if (i==20){
                control[i]=twobyte[1];
            }else if (i==29){
                //单价
                control[i]=VariableUtil.byteArray.get(25);
                //flag,设备号变化
            }else if (i==42){
                control[i]=(byte)0x0f;
            } else if (i==43){
                control[i]=(byte)0x80;
            }else if (i==44){
                control[i]=(byte)0x03;
               // Control[i]=(byte)0x1c;     ，
            } else if (i==45){
                //单价，
                control[i]=(byte)0x20;
            } else {
                control[i]=(byte)0x00;
            }
        }
        return control;
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

    /**
     * 屏幕背光控制
     * @param switchState
     * @return
     */
    public static byte[] setCloseScreenData(int switchState){
        byte[] data=new byte[CONTROL_INSTRUCT_LENGTH];
        for (int i=0;i<CONTROL_INSTRUCT_LENGTH;i++){
            if (i==39){
                if (switchState==1){
                    data[i]=(byte)0x01;
                }else {
                    data[i]=(byte)0x02;
                }
            }else if (i==46){
                data[i]=(byte)0x80;
            }else {
                data[i]=(byte)0x00;
            }
        }
        return data;
    }
    /**
     *
     * @param byteArrayList  时间指令
     */
    public static void saveTimeInformationToFormatInformation(ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()!=0){
            FormatInformationBean.TimeType=ByteUtils.byteToInt(byteArrayList.get(0));
            FormatInformationBean.Year=ByteUtils.byteToInt(byteArrayList.get(1))+2000;
            FormatInformationBean.Month=ByteUtils.byteToInt(byteArrayList.get(2));
            FormatInformationBean.Day=ByteUtils.byteToInt(byteArrayList.get(3));
            FormatInformationBean.Hour=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatInformationBean.Minute=ByteUtils.byteToInt(byteArrayList.get(5));
            FormatInformationBean.Second=ByteUtils.byteToInt(byteArrayList.get(6));
            FormatInformationBean.TimeWeek=ByteUtils.byteToInt(byteArrayList.get(7));
            FormatInformationBean.TimeZone=ByteUtils.byteToInt(byteArrayList.get(8));
        }
    }
    public static boolean isMakeWater(int i){
        String stringWork= DataCalculateUtils.intToBinary(i);
        return DataCalculateUtils.isEvent(stringWork,3);
    }
}