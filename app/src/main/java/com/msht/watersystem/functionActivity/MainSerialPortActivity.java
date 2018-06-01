package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Environment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;


import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.ConsumeInformationUtils;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.FileUtil;
import com.msht.watersystem.Utils.FormatInformationBean;
import com.msht.watersystem.Utils.FormatInformationUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.MyImgScrollViewPager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.widget.CenterLayout;

public class MainSerialPortActivity extends BaseActivity implements Observer, io.vov.vitamio.MediaPlayer.OnCompletionListener, io.vov.vitamio.MediaPlayer.OnPreparedListener, io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback {
    private static final int CLOSE_MECHINE = 2;
    private static final String TAG = "MainSerialPortActivity";
    private MyImgScrollViewPager myPager;
    private List<View> imageViewList;
    private ImageView textView;
    private PortService portService;
    private double volume = 0.00;
    private boolean bindStatus = false;
    private boolean buyStatus = false;
    private boolean pageStatus = false;
    private ComServiceConnection serviceConnection;
    private CenterLayout videoCenterLayout;
    private SurfaceView mPreview;
    private SurfaceHolder holder;
    private String videoFilePath;
    private io.vov.vitamio.MediaPlayer mMediaPlayer;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private long currentPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_serial_port);
        textView = findViewById(R.id.textView);
        initViewImages();
        bindAndAddObserverToPortService();

        videoCenterLayout = (CenterLayout) findViewById(R.id.cideo_center_layout);
        videoFilePath = FileUtil.getVideoFilePath();
        if (!LibsChecker.checkVitamioLibs(this)) {
            return;
        } else if (TextUtils.isEmpty(videoFilePath)) {
            videoCenterLayout.setVisibility(View.INVISIBLE);
        } else {
            videoCenterLayout.setVisibility(View.VISIBLE);
        }
        mPreview = (SurfaceView) findViewById(R.id.surface);
        holder = mPreview.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);
    }


    private void initViewImages() {
        myPager = findViewById(R.id.myvp);
        textView = findViewById(R.id.textView);
        initImageViewList();
        if (!imageViewList.isEmpty() && imageViewList.size() > 0) {
            pageStatus = true;
            myPager.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            myPager.start(this, imageViewList, 10000);
        } else {
            pageStatus = false;
            myPager.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void initImageViewList() {
        imageViewList = new ArrayList<View>();
        List<String> fileImagelist = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/watersystem/images/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory() && scanner5Directory.list().length > 0) {
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
                    fileImagelist.add(path);
                }
            }
            for (int i = 0; i < fileImagelist.size(); i++) {
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(BitmapUtil.decodeSampledBitmapFromRSavaSD(fileImagelist.get(i), 1633, 888));
                imageViewList.add(imageView);
            }
        } else if (!scanner5Directory.exists()) {
            scanner5Directory.mkdirs();
        }
    }

    private void bindAndAddObserverToPortService() {
        serviceConnection = new ComServiceConnection(this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                //此处给portService赋值有如下两种方式
                //portService=service;
                portService = serviceConnection.getService();
            }
        });
        bindService(new Intent(mContext, PortService.class), serviceConnection,
                BIND_AUTO_CREATE);
        bindStatus = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bindAndAddObserverToPortService();
        if (pageStatus) {
            myPager.startTimer();
        }
    }

    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSKeyEnable();
            //主控板通过COM1发过来的数据
            Packet packet1 = myObservable.getCom1Packet();
            //主控板通过串口1发送数据过来Android前端
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(), new byte[]{0x01, 0x04})) {
                    // MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    //满5升，刷卡，刷卡结账时发过来的指令
                    onCom1Received104DataFromControllBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x01, 0x05})) {
                    // MyLogUtil.delFile();     //清除两天前历史日志
                    //重置倒计时 跳转到无法买水界面
                    onCom1Received105DataFromControllBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x02, 0x04})) {
                    //andorid端主动发送104之后，主控板回复204，跳转到IC卡买水或APP买水或现金出水界面
                    onCom1Received204DataFromControllBoard();
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x01, 0x06})) {
                    //
                }
            }
            //android后台通过串口2发送数据过来前端
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(), new byte[]{0x02, 0x05})) {
                    //主控板发105到前端，前端发到后端，后端回复205
                    onCom2Received205DataFromServer();
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x02, 0x03})) {
                    //前端主动发103给后端，后端回复203过来，保存出水时间和计费模式，出水量等信息的到SP里
                    onCom2Received203DataFromServer(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x01, 0x04})) {
                    String stringWork = DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    //充值
                    if (DataCalculateUtils.isRechargeData(stringWork, 5, 6)) {
                        response204ToServer(packet2.getFrame());
                    }
                    //后端主动发104,让关机
                    onCom2Received104DataFromServer(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x01, 0x02})) {
                    //后端发102，回复202给后端
                    response202ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x01, 0x07})) {
                    //扫码取水，后端主动发送107，回复207给后端,发送104给主控板取水
                    response207ToServer(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray = packet2.getData();
                    onCom2Received107DataFromServer(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x02, 0x06})) {
                    Log.d("com206", CreateOrderType.getPacketString(packet2));
                    //后端主动发送206，设置显示屏的系统时间
                    onCom2Received206DataFromServer(packet2.getData());
                }
            }
        }
    }

    private void onCom2Received206DataFromServer(ArrayList<Byte> data) {
        if (data != null && data.size() != 0) {
            FormatInformationUtil.saveTimeInformationToFormatInformation(data);
            if (FormatInformationBean.TimeType == 1) {
                if (!VariableUtil.setTimeStatus) {
                    try {
                        DateTimeUtils.setDateTime(mContext, FormatInformationBean.Year, FormatInformationBean.Month, FormatInformationBean.Day
                                , FormatInformationBean.Hour, FormatInformationBean.Minute, FormatInformationBean.Second);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    VariableUtil.setTimeStatus = true;
                }
            }
        }
    }

    private void onCom1Received204DataFromControllBoard() {
        if (buyStatus) {
            buyStatus = false;
            if (FormatInformationBean.ConsumptionType == 1) {
                Intent intent = new Intent(mContext, IcCardoutWaterActivity.class);
                startActivityForResult(intent, 1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            } else if (FormatInformationBean.ConsumptionType == 3) {
                Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                startActivityForResult(intent, 1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            } else if (FormatInformationBean.ConsumptionType == 5) {
                Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                startActivityForResult(intent, 1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            }
        }
    }

    //Scan code
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data != null && data.size() != 0) {
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this, CachePreferencesUtil.FIRST_OPEN, false);
            buyStatus = true;
            //民生宝来扫
            if (FormatInformationBean.BusinessType == 1) {
                //已分为单位
                if (FormatInformationBean.AppBalance < 20) {
                    //提示余额不足
                    Intent intent = new Intent(mContext, AppNotSufficientActivity.class);
                    startActivityForResult(intent, 1);
                    unbindPortServiceAndRemoveObserver();
                    myPager.stopTimer();
                } else {
                    //给主控板发指令，取水
                    sendBuyWaterCommand104ToControlBoard(1);
                }
            }//配送端APP来扫
            else if (FormatInformationBean.BusinessType == 2) {
                //给主控板发指令，取水
                sendBuyWaterCommand104ToControlBoard(2);
            }
        }
    }

    private void sendBuyWaterCommand104ToControlBoard(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business == 1) {
                    byte[] data = FormatInformationUtil.setConsumeType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                } else if (business == 2) {
                    byte[] data = FormatInformationUtil.setConsumeType02();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                }
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private void response207ToServer(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x07};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToServer(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private void onCom2Received203DataFromServer(ArrayList<Byte> data) {
        // setEquipmentData(data.get(4));
        try {
            if (data != null && data.size() != 0) {
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                String waterVolume = String.valueOf(FormatInformationBean.WaterNum);
                String time = String.valueOf(FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putStringData(this, CachePreferencesUtil.VOLUME, waterVolume);
                CachePreferencesUtil.putStringData(this, CachePreferencesUtil.OUT_WATER_TIME, time);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.CHARGEMODE, FormatInformationBean.ChargeMode);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.SHOWTDS, FormatInformationBean.ShowTDS);
                VariableUtil.setEquipmentStatus = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setEquipmentData(Byte aByte) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data = FormatInformationUtil.setEquipmentParameter(aByte);
                byte[] packet = PacketUtils.makePackage(frame, type, data);
                portService.sendToControlBoard(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }

    //回复202，免得一直发102
    private void response202ToServer(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x02};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToServer(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private void onCom2Received102DataFromServer(ArrayList<Byte> data) {
        if (ConsumeInformationUtils.controlModel(mContext, data)) {
            /*  .....*/
        }
    }

    private void response204ToServer(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToServer(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private void onCom2Received104DataFromServer(ArrayList<Byte> data) {
        String stringWork = DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int switch2 = ByteUtils.byteToInt(data.get(31));
        //判断是否为关机指令
        if (switch2 == CLOSE_MECHINE && DataCalculateUtils.isEvent(stringWork, 0)) {
            Intent intent = new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent, 1);
            unbindPortServiceAndRemoveObserver();
            myPager.stopTimer();

        }
    }

    private void onCom1Received104DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() > 0) {
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                if (FormatInformationBean.Balance <= 1) {
                    Intent intent = new Intent(mContext, NotSufficientActivity.class);
                    startActivityForResult(intent, 1);
                    unbindPortServiceAndRemoveObserver();
                    myPager.stopTimer();
                } else {
                    String stringWork = DataCalculateUtils.IntToBinary(FormatInformationBean.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork, 3)) {
                        if (FormatInformationBean.ConsumptionType == 1) {
                            Intent intent = new Intent(mContext, IcCardoutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        } else if (FormatInformationBean.ConsumptionType == 3) {
                            Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        } else if (FormatInformationBean.ConsumptionType == 5) {
                            Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        }
                    } else {
                        //刷卡结账
                        calculateData();    //没联网计算取缓存数据
                        double consumption = FormatInformationBean.ConsumptionAmount / 100.0;
                        double waterVolume = FormatInformationBean.WaterYield * volume;
                        String afterAmount = String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                        String afterWater = String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                        String mAccount = String.valueOf(FormatInformationBean.StringCardNo);
                        Intent intent = new Intent(mContext, PaySuccessActivity.class);
                        intent.putExtra("afterAmount", afterAmount);
                        intent.putExtra("afetrWater", afterWater);
                        intent.putExtra("mAccount", mAccount);
                        intent.putExtra("sign", "0");
                        startActivityForResult(intent, 1);
                        unbindPortServiceAndRemoveObserver();
                        myPager.stopTimer();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateData() {
        String waterVolume = CachePreferencesUtil.getStringData(this, CachePreferencesUtil.VOLUME, "5");
        String time = CachePreferencesUtil.getStringData(this, CachePreferencesUtil.OUT_WATER_TIME, "30");
        int mVolume = Integer.valueOf(waterVolume);
        int mTime = Integer.valueOf(time);
        volume = DataCalculateUtils.getWaterVolume(mVolume, mTime);
    }

    private void onCom2Received205DataFromServer() {

    }

    private void onCom1Received105DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() != 0) {
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                String stringWork = DataCalculateUtils.IntToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork, 6)) {
                    Intent intent = new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent, 1);
                    unbindPortServiceAndRemoveObserver();
                    myPager.stopTimer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHavaDataChange(boolean timeFlag) {
        super.onHavaDataChange(timeFlag);
        if (timeFlag) {
        }
    }

    /*    //存储数据重发
        private void sendData(List<OrderInfo> infos) {
            if (portService!=null){
                if (portService.isConnection()){
                    for (int i=0;i<infos.size();i++){
                        sendStatus=false;
                        byte[] data=infos.get(i).getOrderData();
                        portService.sendToServer(data);
                        OrderInfo singgleData=infos.get(i);
                        deleteData(singgleData);
                    }
                    sendStatus=true;
                }
            }
        }*/
/*    public void deleteData(OrderInfo info) {
        getOrderDao().delete(info);
    }*/

 /*   private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            exitApp();
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            startBuyWater();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            startBuyWater();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            startBuyWater();
        } else if (keyCode == KeyEvent.KEYCODE_F1) {
            startBuyWater();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startBuyWater() {
        Intent intent = new Intent(mContext, BuyWaterActivity.class);
        startActivityForResult(intent, 1);
        unbindPortServiceAndRemoveObserver();
        myPager.stopTimer();
    }

    private void exitApp() {   //退出app
        System.exit(0);
    }

    private void unbindPortServiceAndRemoveObserver() {
        if (serviceConnection != null && portService != null) {
            if (bindStatus) {
                bindStatus = false;
                portService.removeObserver(this);
                unbindService(serviceConnection);
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new ContextWrapper(newBase) {
            @Override
            public Object getSystemService(String name) {
                if (Context.AUDIO_SERVICE.equals(name)) {
                    return getApplicationContext().getSystemService(name);
                }
                return super.getSystemService(name);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        initMediaPlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        if(mMediaPlayer==null){
            initMediaPlayer();
            mIsVideoSizeKnown = true;
            mIsVideoReadyToBePlayed = true;
            startVideoPlayback();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }


    private void initMediaPlayer() {
        doCleanUp();
        try {
            mMediaPlayer = new io.vov.vitamio.MediaPlayer(this);
            mMediaPlayer.setDataSource(videoFilePath);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
    }

    private void doCleanUp() {
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    @Override
    public void onPrepared(io.vov.vitamio.MediaPlayer mp) {
        Log.d(TAG, "onPrepared called");
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    private void startVideoPlayback() {
        Log.v(TAG, "startVideoPlayback");
        if (currentPosition != 0) {
            mMediaPlayer.seekTo(currentPosition);
        }
        mMediaPlayer.start();
    }

    @Override
    public void onCompletion(io.vov.vitamio.MediaPlayer mp) {
        Log.d(TAG, "onCompletion called");
        mMediaPlayer.seekTo(0);
        mMediaPlayer.start();
    }

    @Override
    public void onVideoSizeChanged(io.vov.vitamio.MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsVideoSizeKnown = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
        Log.v(TAG, "onPause");

    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            currentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
