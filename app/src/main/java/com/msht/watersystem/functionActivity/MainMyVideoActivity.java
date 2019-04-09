package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.eventmanager.DateMassageEvent;
import com.msht.watersystem.eventmanager.MessageEvent;
import com.msht.watersystem.eventmanager.RestartAppEvent;
import com.msht.watersystem.R;
import com.msht.watersystem.service.NetBroadcastReceiver;
import com.msht.watersystem.utilpackage.BitmapViewListUtil;
import com.msht.watersystem.utilpackage.ByteUtils;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.ConsumeInformationUtils;
import com.msht.watersystem.utilpackage.DataCalculateUtils;
import com.msht.watersystem.utilpackage.DateTimeUtils;
import com.msht.watersystem.utilpackage.FileUtil;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.utilpackage.RestartAppUtil;
import com.msht.watersystem.utilpackage.ThreadPoolManager;
import com.msht.watersystem.utilpackage.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;
import com.msht.watersystem.service.ResendDataService;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.CustomVideoView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 *
 * @author hong
 * @date 2018/8/14  
 */
public class MainMyVideoActivity extends BaseActivity implements Observer/*SurfaceHolder.Callback,*/ /*MediaPlayer.OnBufferingUpdateListener,*/ /*MediaPlayer.OnCompletionListener,*/ /*MediaPlayer.OnPreparedListener,*/ /*MediaPlayer.OnVideoSizeChangedListener*/ {
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private View imageLayout;
    private RelativeLayout mVideoViewContainer;
    private MediaPlayer mMediaPlayer;
    private NetBroadcastReceiver receiver;
    //  private SurfaceHolder holder;
    private boolean mIsVideoSizeKnown = false;
    // private boolean mIsVideoReadyToBePlayed = false;
    private long currentPosition = 0;
    private boolean changeVideo = false;

    /**
     * 扫码发送104帧序
     */
    private byte[] mAppFrame;
    private double volume = 0.00;
    private boolean bindStatus = false;
    private boolean buyStatus = false;
    private boolean pageStatus = true;
    private int timeCount = 0;
    /**
     * 夜间标志
     */
    private boolean nightStatus = true;
    private int videoIndex = 0;
    private List<String> fileList;
    private Context mContext;
    private CustomVideoView mVideoView;
    private String path;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_my_video);
        mContext = this;
        bindAndAddObserverToPortService();
        EventBus.getDefault().register(mContext);
        initService();
        imageLayout = findViewById(R.id.id_layout_frame);
        mVideoViewContainer = findViewById(R.id.id_video_layout);
        // SurfaceView mPreview = (SurfaceView) findViewById(R.id.surface);
        mVideoView = (CustomVideoView) findViewById(R.id.surface_view);
        fileList = FileUtil.getVideoFilePath();
        if (fileList != null && fileList.size() >= 1) {
            imageLayout.setVisibility(View.GONE);
            mVideoViewContainer.setVisibility(View.VISIBLE);
           /* holder = mPreview.getHolder();
            holder.addCallback(this);
            holder.setFormat(PixelFormat.RGBX_8888);*/
           /* if (mVideoViewContainer != null) {
                mVideoViewContainer.removeAllViews();
            }
            mVideoView = new CustomVideoView(getApplicationContext());
            mVideoViewContainer.addView(mVideoView);*/

            path = fileList.get(0);
            index = 0;
            mVideoView.setVideoPath(path);
          //  mVideoView.setMediaController(new MediaController(this));
           // mVideoView.requestFocus();
       /*     mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setPlaybackSpeed(1.0f);
                }
            });*/
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (index == fileList.size() - 1) {
                        index = 0;
                    } else if (index < fileList.size() - 1) {
                        index++;
                    }
                    path = fileList.get(index);
                    mVideoView.setVideoPath(path);
                    mVideoView.start();
                }
            });
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                        @Override
                        public boolean onInfo(MediaPlayer mp, int what, int extra) {
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)
                                mVideoView.setBackgroundColor(Color.TRANSPARENT);
                            return true;
                        }
                    });
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    index=0;
                    path = fileList.get(index);
                    mVideoView.setVideoPath(path);
                    mVideoView.start();
                    return true;
                }
            });
        } else {
            initBannerView();
            imageLayout.setVisibility(View.VISIBLE);
            mVideoViewContainer.setVisibility(View.GONE);
        }
    }


    private void initVideoData(){
        if (fileList != null && fileList.size() >= 1) {
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (index == fileList.size() - 1) {
                        index = 0;
                    } else if (index < fileList.size() - 1) {
                        index++;
                    }
                    path = fileList.get(index);
                    mVideoView.setVideoPath(path);
                    mVideoView.start();
                }
            });
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                        @Override
                        public boolean onInfo(MediaPlayer mp, int what, int extra) {
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)
                                mVideoView.setBackgroundColor(Color.TRANSPARENT);
                            return true;
                        }
                    });
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    index=0;
                    path = fileList.get(index);
                    mVideoView.setVideoPath(path);
                    mVideoView.start();
                    return true;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //initVideoData();
        if (mVideoView!=null){
            mVideoView.start();
        }else {
            imageLayout.setVisibility(View.VISIBLE);
            mVideoViewContainer.setVisibility(View.GONE);
        }

    }

    public void initNetBroadcast() {
        if (receiver==null){
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            receiver = new NetBroadcastReceiver();
            if (!receiver.isOrderedBroadcast()){
                registerReceiver(receiver, filter);
            }
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
        Intent resendDataService = new Intent(mContext, ResendDataService.class);
        startService(resendDataService);
    }

    private void initBannerView() {
        ImageView advertImage = findViewById(R.id.textView);
        BannerM mBanner = findViewById(R.id.id_banner);
        List<Bitmap> imageViewList = BitmapViewListUtil.getBitmapListUtil(mContext);
        if (imageViewList != null && imageViewList.size() > 0) {
            mBanner.setBannerBeanList(VariableUtil.imageViewList)
                    .setDefaultImageResId(R.drawable.water_advertisement)
                    .setIndexPosition(BannerM.INDEX_POSITION_BOTTOM)
                    .setIndexColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                    .setIntervalTime(10)
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
                    //android端主动发送104之后，主控板回复204，跳转到IC卡买水或APP买水或现金出水界面
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
                   // response207ToServer(packet2.getFrame());
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
            if (data != null && data.size() >= ConstantUtil.CONTROL_MAX_SIZE) {
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                String stringWork = DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                if (!DataCalculateUtils.isEvent(stringWork, 3)) {
                    /*打开屏幕背光*/
                    if (nightStatus) {
                        onControlScreenBackground(1);
                        timeCount = 0;
                    }
                    if (FormatInformationBean.Balance <= 1) {
                        pageStatus = false;
                        Intent intent = new Intent(mContext, NotSufficientActivity.class);
                        unbindPortServiceAndRemoveObserver();
                        startActivity(intent);
                    } else {
                        if (FormatInformationBean.ConsumptionType == 1) {
                            pageStatus = false;
                            Intent intent = new Intent(mContext, IcCardOutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
                        } else if (FormatInformationBean.ConsumptionType == 3) {
                            pageStatus = false;
                            Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
                        } else if (FormatInformationBean.ConsumptionType == 5) {
                            pageStatus = false;
                            Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                            unbindPortServiceAndRemoveObserver();
                            startActivity(intent);
                        }
                    }
                } else {
                    //刷卡结账
                    calculateData();    //没联网计算取缓存数据
                    double consumption = FormatInformationBean.ConsumptionAmount / 100.0;
                    double waterVolume = FormatInformationBean.WaterYield * volume;
                    String afterAmount = String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
                    String afterWater = String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
                    String mAccount = String.valueOf(FormatInformationBean.StringCardNo);
                    Intent intent = new Intent(mContext, PaySuccessActivity.class);
                    intent.putExtra("afterAmount", afterAmount);
                    intent.putExtra("afterWater", afterWater);
                    intent.putExtra("mAccount", mAccount);
                    intent.putExtra("sign", "0");
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                    pageStatus = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onCom1Received105DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() >= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE) {
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                String stringWork = DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork, 6)) {
                    pageStatus = false;
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
        if (Arrays.equals(frame, mAppFrame)) {
            if (buyStatus) {
                buyStatus = false;
                if (FormatInformationBean.ConsumptionType == 1) {
                    pageStatus = false;
                    Intent intent = new Intent(mContext, IcCardOutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else if (FormatInformationBean.ConsumptionType == 3) {
                    pageStatus = false;
                    Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else if (FormatInformationBean.ConsumptionType == 5) {
                    pageStatus = false;
                    Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                }
            }
        }
    }
    private void onCom2Received203DataFromServer(ArrayList<Byte> data) {
        try {
            if (data != null && data.size() >= ConstantUtil.REQUEST_MAX_SIZE) {
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                CachePreferencesUtil.getIntData(mContext, CachePreferencesUtil.PRICE, FormatInformationBean.PriceNum);
                CachePreferencesUtil.putIntData(this, CachePreferencesUtil.WATER_OUT_TIME, FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putIntData(this, CachePreferencesUtil.WATER_NUM, FormatInformationBean.WaterNum);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.SHOW_TDS, FormatInformationBean.ShowTDS);
                CachePreferencesUtil.getIntData(this, CachePreferencesUtil.DEDUCT_AMOUNT, FormatInformationBean.DeductAmount);
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
        try {
            if (data != null && data.size() >= ConstantUtil.CONTROL_MAX_SIZE) {
                String stringWork = DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
                int switch2 = ByteUtils.byteToInt(data.get(31));
                //判断是否为关机指令
                if (switch2 == ConstantUtil.CLOSE_MACHINE && DataCalculateUtils.isEvent(stringWork, 0)) {
                    pageStatus = false;
                    Intent intent = new Intent(mContext, CloseSystemActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            /*  .....
            Log.d("FormatInformationBean=","waterVolume="+String.valueOf(FormatInformationBean.WaterNum)+",time="+String.valueOf(FormatInformationBean.OutWaterTime));
            */
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
     *
     * @param data 107指令
     */
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data != null && data.size()>= ConstantUtil.BUSINESS_MAX_SIZE) {
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this, CachePreferencesUtil.FIRST_OPEN, false);
            buyStatus = true;
            /*打开屏幕背光*/
            if (nightStatus) {
                onControlScreenBackground(1);
                timeCount = 0;
            }
            //民生宝来扫
            if (FormatInformationBean.BusinessType == 1) {
                //以分为单位
                if (FormatInformationBean.AppBalance < 20) {
                    //提示余额不足
                    pageStatus = false;
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
                mAppFrame = frame;
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
        int mVolume = CachePreferencesUtil.getIntData(this, CachePreferencesUtil.WATER_NUM, 5);
        int mTime = CachePreferencesUtil.getIntData(this, CachePreferencesUtil.WATER_OUT_TIME, 30);
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

    private void onControlScreenBackground(int status) {
        if (portService != null) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageData(MessageEvent messageEvent) {
        List<OrderInfo> orderData = messageEvent.getMessage();
        if (portService != null) {
            if (portService.isConnection()) {
                for (int i = 0; i < orderData.size(); i++) {
                    VariableUtil.sendStatus = false;
                    byte[] data = orderData.get(i).getOrderData();
                    portService.sendToServer(data);
                    OrderInfo singleData = orderData.get(i);
                    deleteData(singleData);
                }
                VariableUtil.sendStatus = true;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageDate(DateMassageEvent event) {
        int status = event.getMessage();
        if (status == 1) {
            nightStatus = false;
            onControlScreenBackground(status);
        } else if (status == 2 && pageStatus) {
            nightStatus = true;
            timeCount++;
            /*10分钟后关闭**/
            if (timeCount >= 2) {
                onControlScreenBackground(status);
                timeCount = 0;

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRestartApp(RestartAppEvent event) {
        if (event.getMessage()) {
            releaseVideoData();
            RestartAppUtil.restartApp();
           // restartWaterApp();
        }
    }
    private OrderInfoDao getOrderDao() {
        return AppContext.getInstance().getDaoSession().getOrderInfoDao();
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
            timeCount = 0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            onControlScreenBackground(1);
            timeCount = 0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            onControlScreenBackground(1);
            timeCount = 0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F1) {
            onControlScreenBackground(1);
            timeCount = 0;
            startBuyWater();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    private void startBuyWater() {
        pageStatus = false;
        Intent intent = new Intent(mContext, BuyWaterActivity.class);
        unbindPortServiceAndRemoveObserver();
        startActivity(intent);
    }
    private void exitApp() {
        System.exit(0);
    }
    private void restartWaterApp() {
        releaseVideoData();
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent!=null){
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        }
        //**杀死整个进程**//*
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    /*    @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(fileList!=null&&fileList.size()>=1){
                imageLayout.setVisibility(View.GONE);
                videoLayout .setVisibility(View.VISIBLE);
                initMediaPlayer();
            }else {
                imageLayout.setVisibility(View.VISIBLE);
                videoLayout .setVisibility(View.GONE);
            }

        }*/
 /*   @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mMediaPlayer==null){
            if (fileList!=null&&fileList.size()>0){
                if (!TextUtils.isEmpty(fileList.get(videoIndex))){
                    onSetStartPlay();
                }else {
                    videoIndex++;
                    if (videoIndex>=fileList.size()){
                        videoIndex=0;
                    }
                    if (!TextUtils.isEmpty(fileList.get(videoIndex))){
                        onSetStartPlay();
                    }else {
                        imageLayout.setVisibility(View.VISIBLE);
                        videoLayout .setVisibility(View.GONE);
                    }
                }
            }else {
                imageLayout.setVisibility(View.VISIBLE);
                videoLayout .setVisibility(View.GONE);
            }

        }
    }*/
  /*  private void onSetStartPlay(){
       if(initMediaPlayer()) {
           mIsVideoSizeKnown = true;
           startVideoPlayback();
       }
    }*/
  /*  @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}*/
   /* private boolean initMediaPlayer() {
        doCleanUp();
        try {
            String videoPath=fileList.get(videoIndex);
            if (!TextUtils.isEmpty(videoPath)&&mMediaPlayer==null) {
                mMediaPlayer = new MediaPlayer(this);
                setDataPath(videoPath);
                mMediaPlayer.setDisplay(holder);
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnBufferingUpdateListener(this);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnVideoSizeChangedListener(this);
                *//*高质*//*
                mMediaPlayer.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
                mMediaPlayer.setVolume(0.6f,0.6f);
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                return  true;
            }else if(!TextUtils.isEmpty(videoPath)&&mMediaPlayer!=null){
                setDataPath(videoPath);
                return true;
            } else {
                Log.d("initMediaPlayer",Log.getStackTraceString(new Throwable()));
                imageLayout.setVisibility(View.VISIBLE);
                videoLayout .setVisibility(View.GONE);
                return  false;
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
        return false;
    }*/
    private void setDataPath(String videoPath) {
        try {
            mMediaPlayer.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doCleanUp() {
        //mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

  /*  private void startVideoPlayback() {
        if (changeVideo) {
            changeVideo = false;
        } else if (currentPosition != 0) {
            mMediaPlayer.seekTo(currentPosition);
        }
        mMediaPlayer.start();
    }
*/
    /*   private void onStopVideo(){
           mMediaPlayer.stop();
           releaseMediaPlayer();
           videoIndex=0;
           mIsVideoSizeKnown = true;
           changeVideo = true;
       }*/
/*    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) { }
    @Override
    public void onCompletion(MediaPlayer mp) {
        releaseMediaPlayer();
        videoIndex++;
        if (videoIndex>=fileList.size()){
            videoIndex=0;
        }
        if (initMediaPlayer()) {
            mIsVideoSizeKnown = true;
           // mIsVideoReadyToBePlayed = true;
            changeVideo = true;
        }
    }*/
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            currentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /*    @Override
        public void onPrepared(MediaPlayer mp) {
           // mIsVideoReadyToBePlayed = true;
            if (mIsVideoSizeKnown) {
                startVideoPlayback();
            }
        }
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            //关键
            if (width == 0 || height == 0) {
                imageLayout.setVisibility(View.VISIBLE);
                videoLayout .setVisibility(View.INVISIBLE);
               // return;
            }else {
                imageLayout.setVisibility(View.GONE);
                videoLayout .setVisibility(View.VISIBLE);
                mIsVideoSizeKnown = true;
            }
        }*/
    @Override
    protected void onStart() {
        super.onStart();
        initNetBroadcast();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        bindAndAddObserverToPortService();
        pageStatus = true;
    }

    /*@Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new ContextWrapper(newBase){
            @Override
            public Object getSystemService(String name) {
                if (Context.AUDIO_SERVICE.equals(name)){
                    return getApplicationContext().getSystemService(name);
                }
                return super.getSystemService(name);
            }
        });
    }*/
    private void  releaseVideoData(){
        if (mVideoView!=null){
            mVideoView.stopPlayback();
            mVideoView.suspend();
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnPreparedListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView=null;
            mVideoViewContainer.removeAllViews();
        }
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
        ThreadPoolManager.getInstance(mContext).onShutDown();
        EventBus.getDefault().unregister(mContext);
    }
}
