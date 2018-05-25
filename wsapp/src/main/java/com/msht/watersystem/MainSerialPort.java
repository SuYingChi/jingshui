package com.msht.watersystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;
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
import com.msht.watersystem.Manager.GreenDaoManager;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.ControlInstructUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.MyLogUtil;
import com.msht.watersystem.Utils.StatusInstructUtil;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.functionView.AppNotSufficient;
import com.msht.watersystem.functionView.AppoutWater;
import com.msht.watersystem.functionView.BuyWater;
import com.msht.watersystem.functionView.CannotBuywater;
import com.msht.watersystem.functionView.CloseSystem;
import com.msht.watersystem.functionView.NotSufficient;
import com.msht.watersystem.functionView.OutWater;
import com.msht.watersystem.functionView.IcCardoutWater;
import com.msht.watersystem.functionView.PaySuccess;
import com.msht.watersystem.gen.OrderInfoDao;
import com.msht.watersystem.widget.CustomVideoView;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;




public class MainSerialPort extends BaseActivity  implements Observer, Handler.Callback{

    private MyImgScroll     myPager;
    private CustomVideoView mVideoView;
    private List<View>      listViews;
    private ImageView       textView;
    private PortService     portService;
    private Uri      uri=null;
    private double   volume=0.00;
    private boolean  bindStatus=false;
    private boolean  buyStatus=false;
    private String   videoPath="";
    private boolean  setDateStatus=true;
    private boolean  sendStatus=true;
    private ComServiceConnection serviceConnection;
    private IntentFilter intentFilter;
    private final static String TAG = MainSerialPort.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_serial_port);
       // mContext=this;
       // initViewImages();
        textView = (ImageView) findViewById(R.id.textView);
        initVideoView();
        OpenService();
       // StartRunnable();
        /*intentFilter=new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(timeChangeReveiver,intentFilter);*/
    }
    private void initsetData() {
        if (portService != null) {
           if (portService.isConnection()){
               try {
                   byte[] frame = FrameUtils.getFrame(mContext);
                   byte[] type = new byte[]{0x01, 0x03};
                   byte[] packet = PacketUtils.makePackage(frame, type, null);
                   portService.sendToCom2(packet);
               } catch (CRCException e) {
                   e.printStackTrace();
               } catch (FrameException e) {
                   e.printStackTrace();
               } catch (CmdTypeException e) {
                   e.printStackTrace();
               }
           }
        }
    }
    private void initVideoView() {
        mVideoView=(CustomVideoView) findViewById(R.id.id_mp4_vedio) ;
        List<String> fileVediolist = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/video/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory()&&scanner5Directory.list().length > 0) {
            textView.setVisibility(View.GONE);
            mVideoView.setVisibility(View.VISIBLE);
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".mp4") ) {
                    fileVediolist.add(path);
                }
            }
            if (fileVediolist.size()>=1){
                uri=Uri.parse(fileVediolist.get(0));
                videoPath=fileVediolist.get(0);
            }
           // mVideoView.setVideoURI(uri);
           // mVideoView.setMediaController(new MediaController(this));
          //  mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH,1);
            mVideoView.setVideoPath(videoPath);
            mVideoView.start();
            mVideoView.requestFocus();
            mVideoView.setOnCompletionListener(new MyPlayerOnComletionListener());
            mVideoView.setOnPreparedListener(new MyPreparedListener());
        }else {
            mVideoView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/video/");
        if (!file.exists()){
            file.mkdirs();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/");
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    class MyPreparedListener implements MediaPlayer.OnPreparedListener{

        @Override
        public void onPrepared(MediaPlayer mp) {
           // mp.setPlaybackSpeed(1.0f);
            mp.start();
            mp.setLooping(true);
        }
    }
    class MyPlayerOnComletionListener implements MediaPlayer.OnCompletionListener{

        @Override
        public void onCompletion(MediaPlayer mp) {
           // mp.start();
            mp.seekTo(0);
            mVideoView.setVideoPath(videoPath);
          //  mVideoView.requestFocus();
            mVideoView.start();
        }
    }
    private void OpenService(){
        serviceConnection = new ComServiceConnection(MainSerialPort.this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                //此处给portService赋值有如下两种方式
                //portService=service;
                portService = serviceConnection.getService();
            }
        });
        bindService(new Intent(mContext, PortService.class), serviceConnection,
                BIND_AUTO_CREATE);
        bindStatus=true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OpenService();
        mVideoView.start();
    }
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSkeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    intAnalist(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板状态指令105：",CreateOrderType.getPacketString(packet1));
                    MyLogUtil.delFile();
                    initCom105Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    MyLogUtil.d("主板回复指令204",CreateOrderType.getPacketString(packet1));
                    initCom204Data();
                }else {
                    MyLogUtil.d("主板其他指令类型",CreateOrderType.getPacketString(packet1));
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("服务端回复指令205：",CreateOrderType.getPacketString(packet2));
                    initCom205Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    MyLogUtil.d("服务端回复指令203：",CreateOrderType.getPacketString(packet2));
                    initCom203Data(packet2.getData());
                } else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());
                    }
                    initCom104Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    MyLogUtil.d("服务端设备设置指令102：",CreateOrderType.getPacketString(packet2));
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    MyLogUtil.d("服务端业务指令107：",CreateOrderType.getPacketString(packet2));
                    responseSever207(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x06})){
                    MyLogUtil.d("服务端时间指令206：", CreateOrderType.getPacketString(packet2));
                    initComData206(packet2.getData());
                }
            }
        }
    }
    private void initComData206(ArrayList<Byte> data) {
        if (ControlInstructUtil.TimeInstruct(data)){
            if (FormatToken.TimeType==1){
                    try {
                        DateTimeUtils.setDateTime(mContext,FormatToken.Year,FormatToken.Month,FormatToken.Day
                                ,FormatToken.Hour,FormatToken.Minute,FormatToken.Second);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
    private void initCom204Data() {
        if (buyStatus){
            buyStatus=false;
            if (FormatToken.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWater.class);
                startActivityForResult(intent,1);
                CloseService();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppoutWater.class);
                startActivityForResult(intent,1);
                CloseService();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,OutWater.class);
                startActivityForResult(intent,1);
                CloseService();
            }
        }
    }
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);//数据更变
            buyStatus=true;
            if (FormatToken.Balance<20){
                Intent intent=new Intent(mContext,AppNotSufficient.class);
                startActivityForResult(intent,1);
                CloseService();
            }else {
                if (FormatToken.BusinessType==1){
                    setBusiness(1);
                }else if (FormatToken.BusinessType==2){
                    setBusiness(2);
                }
            }
        }
    }
    private void setBusiness(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business==1){
                    byte[] data= ControlInstructUtil.setBusinessType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
                }else if (business==2){
                    byte[] data= ControlInstructUtil.setBusinessType02();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
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
    private void responseSever207(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x07};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom203Data(ArrayList<Byte> data) {
        try {
            if(ControlInstructUtil.EquipmentData(data)){
                String waterVolume=String.valueOf(FormatToken.WaterNum);
                String Time=String.valueOf(FormatToken.OutWaterTime);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.Volume,waterVolume);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.outWaterTime,Time);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ChargeMode,FormatToken.ChargeMode);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ShowTds,FormatToken.ShowTDS);
                CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void response102(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x02};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom102Data2(ArrayList<Byte> data) {
        if (BusinessInstruct.ControlModel(mContext,data)){
        }
    }
    private void responseServer(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,1);
            CloseService();
        }
    }
    private void intAnalist(ArrayList<Byte> data) {
        try {
            if(ControlInstructUtil.ControlInstruct(data)){
                if (FormatToken.Balance<=1){
                    Intent intent=new Intent(mContext,NotSufficient.class);
                    startActivityForResult(intent,1);
                    CloseService();
                }else {
                    String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)) {
                        if (FormatToken.ConsumptionType == 1) {
                            Intent intent = new Intent(mContext, IcCardoutWater.class);
                            startActivityForResult(intent, 1);
                        } else if (FormatToken.ConsumptionType == 3) {
                            Intent intent = new Intent(mContext, AppoutWater.class);
                            startActivityForResult(intent, 1);
                        } else if (FormatToken.ConsumptionType == 5) {
                            Intent intent = new Intent(mContext, OutWater.class);
                            startActivityForResult(intent, 1);
                        }
                        CloseService();
                    }else {
                        //刷卡结账
                        CalculateData();    //没联网计算取缓存数据
                        double consumption=FormatToken.ConsumptionAmount/100.0;
                        double waterVolume=FormatToken.WaterYield*volume;
                        String afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                        String afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                        String mAccount=String.valueOf(FormatToken.StringCardNo);
                        Intent intent=new Intent(mContext,PaySuccess.class);
                        intent.putExtra("afterAmount",afterAmount) ;
                        intent.putExtra("afetrWater",afterWater);
                        intent.putExtra("mAccount",mAccount);
                        intent.putExtra("sign","0");
                        startActivityForResult(intent,1);
                        CloseService();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void initCom205Data() {

    }
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (StatusInstructUtil.StatusInstruct(data)){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuywater.class);
                    startActivityForResult(intent,1);
                    CloseService();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initViewImages() {
        myPager = (MyImgScroll) findViewById(R.id.myvp);
        InitViewPagers();
        if (!listViews.isEmpty()&&listViews.size()>0) {
            myPager.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            myPager.start(this, listViews, 3000);
        }else{
            myPager.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
    }
    private void InitViewPagers() {
        listViews = new ArrayList<View>();
        List<String> fileImagelist = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory()&&scanner5Directory.list().length > 0) {
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg")|| path.endsWith(".png")) {
                    fileImagelist.add(path);
                }
            }
            for (int i = 0; i <fileImagelist.size(); i++) {
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(BitmapUtil.decodeSampledBitmapFromFile(fileImagelist.get(i), 500, 500));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                listViews.add(imageView);
            }
        }
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        if (!file.exists()){
            file.mkdirs();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/");
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private BroadcastReceiver timeChangeReveiver =new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()==intent.ACTION_TIME_TICK){
                boolean isFirstOpen = CachePreferencesUtil.getBoolean(mContext, CachePreferencesUtil.FIRST_OPEN, true);
                if (isFirstOpen){
                    initsetData();
                }
               /* List<OrderInfo> infos =getOrderDao().loadAll();
                if (infos.size()>=1&&infos!=null){
                    if (sendStatus){
                        CheckTime(infos);
                    }
                }*/
            }
        }
    };
    private void CheckTime(List<OrderInfo> infos) {
        Calendar calendar=Calendar.getInstance();
        int hour=calendar.get(Calendar.HOUR_OF_DAY);
        int minute=calendar.get(Calendar.MINUTE);
        int minuteOfDay=hour*60+minute;
        final int start=0*60+10;
        final int end=4*60+40;
        if (minuteOfDay>=start&&minuteOfDay<=end){
            if (portService!=null){
                if (portService.isConnection()){
                    for (int i=0;i<infos.size();i++){
                        sendStatus=false;
                        byte[] data=infos.get(i).getOrderData();
                        portService.sendToCom2(data);
                        OrderInfo singgleData=infos.get(i);//单条
                        deleteData(singgleData);
                      //  Toast.makeText(mContext,"发送数据",Toast.LENGTH_SHORT).show();
                    }
                    sendStatus=true;
                }
            }
        }
    }
    public void deleteData(OrderInfo info){
        getOrderDao().delete(info);
    }
    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            Intent intent=new Intent(mContext,BuyWater.class);
            startActivityForResult(intent,1);
            CloseService();
            return true;
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            Intent intent=new Intent(mContext,BuyWater.class);
            startActivityForResult(intent,1);
            CloseService();
            return true;

        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            Intent intent=new Intent(mContext,BuyWater.class);
            startActivityForResult(intent,1);
            CloseService();
            return true;
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            Intent intent=new Intent(mContext,BuyWater.class);
            startActivityForResult(intent,1);
            CloseService();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    private void showTips() {
        System.exit(0);
    }
    private void CloseService(){
        if (serviceConnection != null && portService != null) {
            if (bindStatus){
                bindStatus=false;
                portService.removeObserver(this);
                unbindService(serviceConnection);
            }
        }
    }
    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(new ContextWrapper(newBase)
        {
            @Override
            public Object getSystemService(String name)
            {
                if (Context.AUDIO_SERVICE.equals(name))
                    return getApplicationContext().getSystemService(name);
                return super.getSystemService(name);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        //removeback();
      //  unregisterReceiver(timeChangeReveiver);
    }
}
