package com.msht.watersystem.Utils;

import java.util.ArrayList;

/**
 * Created by hong on 2017/10/28.
 */
public class FormatCommandUtil {
    /*
   *@param  byteList  控制指令
    */
    public static boolean convertCom1ReceivedDataToFormatToken(ArrayList<Byte> byteArrayList){

        if (byteArrayList!=null&&byteArrayList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteArrayList.get(0);
            deviceId[1]=byteArrayList.get(1);
            deviceId[2]=byteArrayList.get(2);
            deviceId[3]=byteArrayList.get(3);
            FormatToken.DeviceId=ByteUtils.byte4ToInt(deviceId);

            FormatToken.StringCardNo= convertIcCardCodeDataToFormatToken(byteArrayList);
            byte[] timeByte=new byte[6];
            timeByte[0]=byteArrayList.get(9);
            timeByte[1]=byteArrayList.get(10);
            timeByte[2]=byteArrayList.get(11);
            timeByte[3]=byteArrayList.get(12);
            timeByte[4]=byteArrayList.get(13);
            timeByte[5]=byteArrayList.get(14);
            FormatToken.TriggerTime=ByteUtils.byte6ToInt(timeByte);
            FormatToken.ConsumptionType=ByteUtils.byteToInt(byteArrayList.get(15));

            byte[] balanceByte=new byte[2];
            balanceByte[0]=byteArrayList.get(16);
            balanceByte[1]=byteArrayList.get(17);
            FormatToken.Balance=ByteUtils.byte2ToInt(balanceByte);

            FormatToken.AmountType=ByteUtils.byteToInt(byteArrayList.get(18));//金额类型

            byte[] amountByte=new byte[2];
            amountByte[0]=byteArrayList.get(19);
            amountByte[1]=byteArrayList.get(20);
            FormatToken.ConsumptionAmount=ByteUtils.byte2ToInt(amountByte);

            byte[] afterByte=new byte[2];
            afterByte[0]=byteArrayList.get(21);
            afterByte[1]=byteArrayList.get(22);
            FormatToken.AfterAmount=ByteUtils.byte2ToInt(afterByte);

            byte[] waterByte=new byte[2];
            waterByte[0]=byteArrayList.get(23);
            waterByte[1]=byteArrayList.get(24);
            FormatToken.WaterYield=ByteUtils.byte2ToInt(waterByte);

            byte[] rechargeByte=new byte[2];
            rechargeByte[0]=byteArrayList.get(25);
            rechargeByte[1]=byteArrayList.get(26);
            FormatToken.RechargeValue=ByteUtils.byte2ToInt(rechargeByte);

            byte[] installIdByte=new byte[2];
            installIdByte[0]=byteArrayList.get(27);
            installIdByte[1]=byteArrayList.get(28);
            FormatToken.MotifyInstall=ByteUtils.byte2ToInt(installIdByte);

            FormatToken.MotifyPrice=ByteUtils.byteToInt(byteArrayList.get(29));  //修改单价
            FormatToken.MotifyOzoneTime=ByteUtils.byteToInt(byteArrayList.get(30));
            FormatToken.Switch=ByteUtils.byteToInt(byteArrayList.get(31));
            FormatToken.EnableGet=ByteUtils.byteToInt(byteArrayList.get(32));
            FormatToken.SetFilter=ByteUtils.byteToInt(byteArrayList.get(33));
            FormatToken.SetFilterLevel=ByteUtils.byteToInt(byteArrayList.get(34));

            byte[] deductByte=new byte[2];
            deductByte[0]=byteArrayList.get(35);
            deductByte[1]=byteArrayList.get(36);
            FormatToken.SetDeductAmount=ByteUtils.byte2ToInt(deductByte);
            FormatToken.Blacklist=ByteUtils.byteToInt(byteArrayList.get(37));
            FormatToken.KeyCode=ByteUtils.byteToInt(byteArrayList.get(38));
            FormatToken.Updateflag1=ByteUtils.byteToInt(byteArrayList.get(42));
            FormatToken.Updateflag2=ByteUtils.byteToInt(byteArrayList.get(43));
            FormatToken.Updateflag3=ByteUtils.byteToInt(byteArrayList.get(44));
            FormatToken.Updateflag4=ByteUtils.byteToInt(byteArrayList.get(45));
            FormatToken.Updateflag5=ByteUtils.byteToInt(byteArrayList.get(46));
            FormatToken.Updateflag6=ByteUtils.byteToInt(byteArrayList.get(47));
            return true;
        }else {
            return false;
        }
    }
    /*
    *@param  byteList  状态指令
     */
    public static boolean convertStatusCommandToFormatToken(ArrayList<Byte> byteList){
        if (byteList!=null&&byteList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteList.get(0);
            deviceId[1]=byteList.get(1);
            deviceId[2]=byteList.get(2);
            deviceId[3]=byteList.get(3);
            FormatToken.DeviceId=ByteUtils.byte4ToInt(deviceId);
            byte[] installId=new byte[2];
            installId[0]=byteList.get(4);
            installId[1]=byteList.get(5);
            FormatToken.InstallId=ByteUtils.byte2ToInt(installId);
            FormatToken.Ozonetime=ByteUtils.byteToInt(byteList.get(6));
            FormatToken.PriceNum=ByteUtils.byteToInt(byteList.get(7));
            FormatToken.humidity=ByteUtils.byteToInt(byteList.get(8));
            FormatToken.temperature=ByteUtils.byteToInt(byteList.get(9));
            byte[] origin=new byte[2];
            origin[0]=byteList.get(10);
            origin[1]=byteList.get(11);
            FormatToken.OriginTDS=ByteUtils.byte2ToInt(origin);
            byte[] purification=new byte[2];
            purification[0]=byteList.get(12);
            purification[1]=byteList.get(13);
            FormatToken.PurificationTDS=ByteUtils.byte2ToInt(purification);
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
        }else {
            return false;
        }
    }
    /*
    *Ic卡编码数据转换
     */
    private static String convertIcCardCodeDataToFormatToken(ArrayList<Byte> byteArrayList) {
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
        FormatToken.CardNo=ByteUtils.byte4ToInt(cardByte);
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
    public static boolean equipmentData(ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()!=0){
            byte[] deviceId=new byte[4];
            deviceId[0]=byteArrayList.get(0);
            deviceId[1]=byteArrayList.get(1);
            deviceId[2]=byteArrayList.get(2);
            deviceId[3]=byteArrayList.get(3);
            FormatToken.DeviceId=ByteUtils.byte4ToInt(deviceId);
            FormatToken.PriceNum=ByteUtils.byteToInt(byteArrayList.get(4));
            FormatToken.OutWaterTime=ByteUtils.byteToInt(byteArrayList.get(5));
            FormatToken.WaterNum=ByteUtils.byteToInt(byteArrayList.get(6));
            FormatToken.ChargeMode=ByteUtils.byteToInt(byteArrayList.get(7));
            FormatToken.ShowTDS=ByteUtils.byteToInt(byteArrayList.get(8));
            return true;
        }else {
            return false;
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

    public static byte[] setTransactionType01(){
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
                control[i]=VariableUtil.byteArray.get(21);  //设备号
            }else if (i==1){
                control[i]=VariableUtil.byteArray.get(22);
            }else if (i==2){
                control[i]=VariableUtil.byteArray.get(23);
            }else if (i==3){
                control[i]=VariableUtil.byteArray.get(24);
            }else if (i==15){
                control[i]=(byte)0x03;   //
            }else if (i==16){
                control[i]=twobyte[0];
            }else if (i==17){
                control[i]=twobyte[1];
            }else if (i==18){
                control[i]=(byte)0x01;
            }else if (i==29){
                control[i]=VariableUtil.byteArray.get(25);  //单价
            }else if (i==42){         //flag,设备号变化
                control[i]=(byte)0x0f;
            } else if (i==43){
                control[i]=(byte)0x80;      //消费类型
            }else if (i==44){
                control[i]=(byte)0x03;
            }else if (i==45){
                control[i]=(byte)0x20;      //单价，
            }else {
                control[i]=(byte)0x00;
            }
        }
        return control;
    }
    public static byte[] setTransactionType02(){
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
                control[i]=VariableUtil.byteArray.get(21);  //设备号
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
                control[i]=VariableUtil.byteArray.get(25);  //单价
            }else if (i==42){         //flag,设备号变化
                control[i]=(byte)0x0f;
            } else if (i==43){
                control[i]=(byte)0x80;
            }else if (i==44){
                control[i]=(byte)0x03;
               // Control[i]=(byte)0x1c;      //本次消费金额，
            } else if (i==45){
                control[i]=(byte)0x20;      //单价，
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
    /*
    *@param  byteArrayList 时间指令
     */
    public static boolean timeCommand(ArrayList<Byte> byteArrayList){
        if (byteArrayList!=null&&byteArrayList.size()!=0){
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