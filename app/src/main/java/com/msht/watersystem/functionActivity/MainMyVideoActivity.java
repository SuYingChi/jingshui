package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapViewListUtil;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.ConstantUtil;
import com.msht.watersystem.Utils.ConsumeInformationUtils;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.FileUtil;
import com.msht.watersystem.Utils.FormatInformationBean;
import com.msht.watersystem.Utils.FormatInformationUtil;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;
import com.msht.watersystem.service.ResendDataService;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.ToastUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/14  
 */
public class MyVideoActivity extends BaseActivity implements Observer,SurfaceHolder.Callback, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {

    private static final String TAG = "MediaPlayerDemo";
    private PortService portService;
    /**扫码发送104帧序*/
    private byte[]  mAppFrame;
    private double  volume = 0.00;
    private boolean bindStatus = false;
    private boolean buyStatus = false;
    private boolean pageStatus = false;
    private int timeCount=0;
    private ComServiceConnection serviceConnection;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder holder;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
    private long currentPosition = 0;
    private boolean changeVideo = false;
    /*夜间标志*/
    private boolean nightStatus=false;
    private int videoIndex=0;
    private List<String> fileList;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LibsChecker.checkVitamioLibs(this)) {
            return;
        }
        setContentView(R.layout.activity_my_video);
        context=this;
        bindAndAddObserverToPortService();
        initService();
        View imageLayout=findViewById(R.id.id_layout_frame);
        View videoLayout =  findViewById(R.id.id_video_layout);
        SurfaceView mPreview = (SurfaceView) findViewById(R.id.surface);
        initBannerView();
        fileList= FileUtil.getVideoFilePath();
        holder = mPreview.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBX_8888);
        if(fileList==null||fileList.size()<1){
            imageLayout.setVisibility(View.VISIBLE);
            videoLayout .setVisibility(View.GONE);
        }else {
            imageLayout.setVisibility(View.GONE);
            videoLayout .setVisibility(View.VISIBLE);
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

    private void initService() {
        /**开启一个新的服务，*/
        Intent resendDataService=new Intent(context,ResendDataService.class);
        context.startService(resendDataService);
    }
    private void initBannerView() {
        ImageView advertImage = findViewById(R.id.textView);
        BannerM mBanner=findViewById(R.id.id_banner);
        List<Bitmap> imageViewList= BitmapViewListUtil.getBitmapListUtil(context);
        if (imageViewList!=null&& imageViewList.size() > 0) {
            mBanner.setBannerBeanList(VariableUtil.imageViewList)
                    .setDefaultImageResId(R.drawable.ad_water)
                    .setIndexPosition(BannerM.INDEX_POSITION_BOTTOM)
                    .setIndexColor(getResources().getColor(R.color.colorPrimary))
                    .setIntervalTime(5)
                    .show();
            mBanner.setVisibility(View.VISIBLE);
            advertImage.setVisibility(View.GONE);
        } else {
            mBanner.setVisibility(View.GONE);
            advertImage.setVisibility(View.VISIBLE);
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
                    //满5升，刷卡，刷卡结账时发过来的指令
                    onCom1Received104DataFromControlBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x01, 0x05})) {
                    //重置倒计时 跳转到无法买水界面
                    onCom1Received105DataFromControlBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x02, 0x04})) {
                    //andorid端主动发送104之后，主控板回复204，跳转到IC卡买水或APP买水或现金出水界面
                    onCom1Received204DataFromControlBoard(packet1.getFrame());
                }
            }
            //android后台通过串口2发送数据过来前端
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(), new byte[]{0x02, 0x03})) {
                    //前端主动发103给后端，后端回复203过来，保存出水时间和计费模式，出水量等信息的到SP里
                    onCom2Received203DataFromServer(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(), new byte[]{0x01, 0x04})) {
                    String stringWork = DataCalculateUtils.intToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
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
                    //后端主动发送206，设置显示屏的系统时间
                    onCom2Received206DataFromServer(packet2.getData());
                }
            }
        }
    }
    private void onCom1Received104DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() > 0) {
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                if (FormatInformationBean.Balance <= 1) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, NotSufficientActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else {
                    String stringWork = DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork, 3)) {
                        /*打开屏幕背光*/
                        if (nightStatus){
                            onControlScreenBackground(1);
                            timeCount=0;
                        }
                        if (FormatInformationBean.ConsumptionType == 1) {
                            pageStatus=false;
                            Intent intent = new Intent(mContext, IcCardoutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
                        } else if (FormatInformationBean.ConsumptionType == 3) {
                            pageStatus=false;
                            Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
                        } else if (FormatInformationBean.ConsumptionType == 5) {
                            pageStatus=false;
                            Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
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
                        unbindPortServiceAndRemoveObserver();
                        startActivity(intent);
                        pageStatus=false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onCom1Received105DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() != 0) {
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                String stringWork = DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork, 6)) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, CannotBuyWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onCom1Received204DataFromControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mAppFrame)){
            if (buyStatus) {
                buyStatus = false;
                if (FormatInformationBean.ConsumptionType == 1) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, IcCardoutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else if (FormatInformationBean.ConsumptionType == 3) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else if (FormatInformationBean.ConsumptionType == 5) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                }
            }
        }
    }
    private void onCom2Received203DataFromServer(ArrayList<Byte> data) {
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
        String stringWork = DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
        int switch2 = ByteUtils.byteToInt(data.get(31));
        //判断是否为关机指令
        if (switch2 == ConstantUtil.CLOSE_MACHINE && DataCalculateUtils.isEvent(stringWork, 0)) {
            pageStatus=false;
            Intent intent = new Intent(mContext, CloseSystemActivity.class);
            unbindPortServiceAndRemoveObserver();
            startActivity(intent);
        }
    }
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
    /**
     * 扫码业务指令
     * @param data
     */
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data != null && data.size() != 0) {
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this, CachePreferencesUtil.FIRST_OPEN, false);
            buyStatus = true;
            /*打开屏幕背光*/
            if (nightStatus){
                onControlScreenBackground(1);
                timeCount=0;
            }
            //民生宝来扫
            if (FormatInformationBean.BusinessType == 1) {
                //以分为单位
                if (FormatInformationBean.AppBalance < 20) {
                    //提示余额不足
                    pageStatus=false;
                    Intent intent = new Intent(mContext, AppNotSufficientActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
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
    private void sendBuyWaterCommand104ToControlBoard(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                mAppFrame=frame;
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
    private void calculateData() {
        String waterVolume = CachePreferencesUtil.getStringData(this, CachePreferencesUtil.VOLUME, "5");
        String time = CachePreferencesUtil.getStringData(this, CachePreferencesUtil.OUT_WATER_TIME, "30");
        int mVolume = Integer.valueOf(waterVolume);
        int mTime = Integer.valueOf(time);
        volume = DataCalculateUtils.getWaterVolume(mVolume, mTime);
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
    public void onHaveDataChange(List<OrderInfo> orderData) {
        super.onHaveDataChange(orderData);
        if (portService!=null){
            if (portService.isConnection()){
                for (int i=0;i<orderData.size();i++){
                    VariableUtil.sendStatus=false;
                    byte[] data=orderData.get(i).getOrderData();
                    portService.sendToServer(data);
                    OrderInfo singleData=orderData.get(i);
                    deleteData(singleData);
                }
                VariableUtil.sendStatus=true;
            }
        }
    }
    @Override
    public void onControlScreen(int status) {
        super.onControlScreen(status);
        if (status==1){
            nightStatus=false;
            onControlScreenBackground(status);
        }else if (status==2&&pageStatus){
            nightStatus=true;
            timeCount++;
            /*10分钟后关闭**/
            if (timeCount>=2){
                onControlScreenBackground(status);
                timeCount=0;
            }
        }
    }
    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
    private void deleteData(OrderInfo info) {
        getOrderDao().delete(info);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            exitApp();
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            onControlScreenBackground(1);
            timeCount=0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            onControlScreenBackground(2);
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            onControlScreenBackground(1);
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F1) {
            onControlScreenBackground(1);
            timeCount=0;
            startBuyWater();
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }
    private void onControlScreenBackground(int status) {
        if (portService!=null){
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data = FormatInformationUtil.setCloseScreenData(status);
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
    private void startBuyWater() {
        pageStatus=false;
        Intent intent = new Intent(mContext, BuyWaterActivity.class);
        unbindPortServiceAndRemoveObserver();
        startActivity(intent);
    }
    private void exitApp() {
        System.exit(0);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initMediaPlayer();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mMediaPlayer==null){
            initMediaPlayer();
            mIsVideoSizeKnown = true;
            mIsVideoReadyToBePlayed = true;
            startVideoPlayback();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}
    private void initMediaPlayer() {
        doCleanUp();
        try {
            String videoPath=fileList.get(videoIndex);
            if (TextUtils.isEmpty(videoPath)) {
                Toast.makeText(MyVideoActivity.this, "Please edit MediaPlayerDemo_Video Activity, " + "and set the path variable to your media file path." + " Your media file must be stored on sdcard.", Toast.LENGTH_LONG).show();
                return;
            }
            mMediaPlayer = new MediaPlayer(this);
            setDataPath(videoPath);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            /*高质*/
            mMediaPlayer.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
            mMediaPlayer.setVolume(0.6f,0.6f);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
           e.printStackTrace();
        }
    }
    private void setDataPath(String videoPath) {
        try {
            mMediaPlayer.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void doCleanUp() {
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }
    private void startVideoPlayback() {
        if(changeVideo){
            changeVideo =false;
        }else if(currentPosition != 0){
            mMediaPlayer.seekTo(currentPosition);
        }
        mMediaPlayer.start();
    }
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) { }
    @Override
    public void onCompletion(MediaPlayer mp) {
        releaseMediaPlayer();
        videoIndex++;
        if (videoIndex==fileList.size()){
            videoIndex=0;
        }
        initMediaPlayer();
        mIsVideoSizeKnown = true;
        mIsVideoReadyToBePlayed = true;
        changeVideo = true;
    }
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            currentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    @Override
    public void onPrepared(MediaPlayer mp) {
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoSizeKnown) {
            startVideoPlayback();
        }
    }
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        mIsVideoSizeKnown = true;
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        bindAndAddObserverToPortService();
        pageStatus=true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        releaseMediaPlayer();
        doCleanUp();
    }
}