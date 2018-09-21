package com.msht.watersystem.Utils;

import android.graphics.Bitmap;
import android.view.View;

import com.mcloyal.serialport.entity.Packet;
import com.msht.watersystem.Interface.ResultListener;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;

import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by hong on 2017/1/11.
 */
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/3/2  
 */
public class VariableUtil {
    public static long dataId;
    public static int mPos;
    public static boolean mKeyEnable;
    public static Bitmap qrCodeBitmap =null;
    public static boolean setTimeStatus=false;
    public static boolean setEquipmentStatus=true;
    public static boolean sendStatus=true;
    public static boolean mFirstOpen=false;
    public static ArrayList<Byte> byteArray=new ArrayList<Byte>();
    private static final String SAVE_DATA_FAIL ="订单保存失败";
    private static final String SAVE_DATA_SUCCESS ="订单保存成功";
    ArrayList<HashMap<String, String>> List = new ArrayList<HashMap<String, String>>();
    public static List<Bitmap> imageViewList= new ArrayList<Bitmap>();

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
                    resultListener.onResultSuccess(SAVE_DATA_SUCCESS);
                }catch (Exception e){
                    e.printStackTrace();
                    resultListener.onResultFail(SAVE_DATA_FAIL);
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
