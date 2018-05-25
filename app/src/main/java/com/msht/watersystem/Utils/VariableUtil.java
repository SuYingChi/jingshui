package com.msht.watersystem.Utils;

import android.graphics.Bitmap;

import com.mcloyal.serialport.entity.Packet;
import com.msht.watersystem.Interface.ResultListener;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hong on 2017/1/11.
 */
public class VariableUtil {
    public static long dataId;
    public static int mPos;
    public static boolean skeyEnable;
    public static Bitmap QrCodebitmap=null;
    public static boolean setTimeStatus=false;
    public static boolean setEquipmentStatus=true;
    public static ArrayList<Byte> byteArray=new ArrayList<Byte>();
    private static final String SavaData_FAIL="订单保存失败";
    private static final String SavaData_SUCCESS="订单保存成功";
    public static void LongTimeSavaData(final Packet packet1, final ResultListener resultListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                DataInputStream dis = null;
                try {
                    byte[] orderData= CreateOrderType.OrderByteData(packet1);
                    OrderInfo insertData = new OrderInfo(null, orderData);
                    getOrderDao().insert(insertData);
                    resultListener.onResultSuccess(SavaData_SUCCESS);
                }catch (Exception e){
                    e.printStackTrace();
                    resultListener.onResultFail(SavaData_FAIL);
                }finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }
    private static OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
}
