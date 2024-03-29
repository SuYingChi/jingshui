package com.msht.watersystem.utilpackage;

import android.graphics.Bitmap;

import com.mcloyal.serialport.entity.Packet;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.Interface.ResultListener;
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
    public static int cardStatus=0;
    public static boolean isRecharge=false;
    public static boolean mKeyEnable;
    public static Bitmap qrCodeBitmap =null;
    public static boolean setTimeStatus=false;
    public static boolean setEquipmentStatus=true;
    public static boolean sendStatus=true;
    public static boolean isOpenBackLight=true;
    public static boolean mFirstOpen=false;
    public static boolean mFirstClear=false;
    public static String mNoticeText="此卡已挂失，如需取水请重新换卡!";
    public static ArrayList<Byte> byteArray=new ArrayList<Byte>();
    private static final String SAVE_DATA_FAIL ="订单保存失败";
    private static final String SAVE_DATA_SUCCESS ="订单保存成功";

    ArrayList<HashMap<String, String>> List = new ArrayList<HashMap<String, String>>();
    public static List<Bitmap> imageViewList= new ArrayList<Bitmap>();
    public static void requestLongTimeSaveData(final Packet packet1, final ResultListener resultListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] orderData= CreatePacketTypeUtil.byteOrderByteDataToString(packet1);
                    OrderInfo insertData = new OrderInfo(null, orderData);
                    getOrderDao().insert(insertData);
                    resultListener.onResultSuccess(SAVE_DATA_SUCCESS);
                }catch (Exception e){
                    e.printStackTrace();
                    resultListener.onResultFail(SAVE_DATA_FAIL);
                }
            }
        }).start();
    }
    public static void requestLongTimeSaveData(final byte[] packet1) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OrderInfo insertData = new OrderInfo(null, packet1);
                    getOrderDao().insert(insertData);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /*private static OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }*/
    private static OrderInfoDao getOrderDao() {
        return AppContext.getInstance().getDaoSession().getOrderInfoDao();
    }
}
