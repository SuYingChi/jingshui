package com.mcloyal.serialport.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2019/3/26 
 */
public class NetWorkUtil {
    public static boolean isNetWorkEnable(Context context) {
        boolean netState=false;
        if (context!=null){
            try {
                ConnectivityManager connectivity = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity!=null){
                    NetworkInfo networkInfo=connectivity.getActiveNetworkInfo();
                    if (networkInfo != null) {
                        if(networkInfo.isConnected()){
                            netState=true;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return netState;
    }
}
