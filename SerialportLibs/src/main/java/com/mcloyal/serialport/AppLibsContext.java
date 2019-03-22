package com.mcloyal.serialport;

import android.app.Application;

import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.ServicesUtils;
import com.mcloyal.serialport.utils.logs.LogUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Observable;
import java.util.Observer;

import android_serialport_api.SerialPort;


/**
 * Created by huangzhong
 * @author hong
 */
public class AppLibsContext extends Application implements Observer {
    private final static String TAG = AppLibsContext.class.getSimpleName();
    private static AppLibsContext appLibsContext;
    private PortService portService;

    @Override
    public void onCreate() {
        super.onCreate();
        appLibsContext = this;
        //启动服务
        ServicesUtils.startPortServices(this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                portService = service;
              /*  if (portService != null) {
                    LogUtils.d(TAG, "AppLibsContext onServiceConnected() 断电重启");
                    portService.sendNetRestartCmd();
                }*/
            }
        });
        LogUtils.d(TAG, "onCreate()");
    }

    public static AppLibsContext instance() {
        return appLibsContext;
    }

    //***************COM1操作（开始）*************
    //*************COM1 串口配置
    private final static String COM1_PATH = "/dev/ttyS0";
    private final static int COM1_BAUDRATE = 9600;
    private SerialPort mSerialPort1 = null;

    /**
     * 创建并打开串口1
     * @return
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    public android_serialport_api.SerialPort getSerialPort1() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort1 == null) {
            mSerialPort1 = new SerialPort(new File(COM1_PATH), COM1_BAUDRATE, 0);
        }
        return mSerialPort1;
    }

    /**
     * 关闭串口1
     */
    public void closeSerialPort1() {
        if (mSerialPort1 != null) {
            mSerialPort1.close();
            mSerialPort1 = null;
        }
    }
    //*****COM1操作（结束）*************

    //###########################################################################

    //*****COM2操作（开始）*************

    //****COM2 串口配置
    private final static String COM2_PATH = "/dev/ttyS7";
    private final static int COM2_BAUDRATE = 115200;//9600   115200
    private SerialPort mSerialPort2 = null;

    /**
     * 创建并打开串口2
     *
     * @return
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    public android_serialport_api.SerialPort getSerialPort2() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort2 == null) {
            mSerialPort2 = new SerialPort(new File(COM2_PATH), COM2_BAUDRATE, 0);
        }
        return mSerialPort2;
    }

    /**
     * 关闭串口2
     */
    public void closeSerialPort2() {
        if (mSerialPort2 != null) {
            mSerialPort2.close();
            mSerialPort2 = null;
        }
    }

    @Override
    public void update(Observable observable, Object data) {

    }
    //*****COM2操作（结束）*************
}
