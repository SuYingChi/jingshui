package com.msht.watersystem;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
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
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.InstructUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.functionView.AppNotSufficient;
import com.msht.watersystem.functionView.AppoutWater;
import com.msht.watersystem.functionView.BuyWater;
import com.msht.watersystem.functionView.CannotBuywater;
import com.msht.watersystem.functionView.CloseSystem;
import com.msht.watersystem.functionView.NotSufficient;
import com.msht.watersystem.functionView.DeliveryOutWater;
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
import java.util.List;
import java.util.Observable;
import java.util.Observer;
public class MainSerialPort extends BaseActivity  implements Observer, Handler.Callback{
    private static final int CLOSE_MECHINE = 2;
    private CustomVideoView mVideoView;
    private MyImgScroll myPager;
    private List<View> imageViewList;
    private ImageView       textView;
    private PortService     portService;
    private Uri      uri=null;
    private double   volume=0.00;
    private boolean  bindStatus=false;
    private boolean  buyStatus=false;
    private String   videoPath="";
    private boolean  sendStatus=true;
    private boolean  pageStatus=false;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_serial_port);
        textView = (ImageView) findViewById(R.id.textView);
        initViewImages();
       // initVideoView();
        OpenService();
    }
    private void initViewImages() {
        myPager = (MyImgScroll) findViewById(R.id.myvp);
        textView = (ImageView) findViewById(R.id.textView);
        initImageViewList();
        if (!imageViewList.isEmpty()&& imageViewList.size()>0) {
            pageStatus=true;
            myPager.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            myPager.start(this, imageViewList, 10000);
        }else{
            pageStatus=false;
            myPager.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
    }
    private void initImageViewList() {
        imageViewList = new ArrayList<View>();
        List<String> fileImagelist = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/watersystem/images/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory()&&scanner5Directory.list().length > 0) {
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg")|| path.endsWith(".png")) {
                    fileImagelist.add(path);
                }
            }
            for (int i = 0; i <fileImagelist.size(); i++) {
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(BitmapUtil.decodeSampledBitmapFromRSavaSD(fileImagelist.get(i), 1633, 888));
               // imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageViewList.add(imageView);
            }
        }else if(!scanner5Directory.exists()){
             scanner5Directory.mkdirs();
        }
       /* File file = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        if (!file.exists()){
            file.mkdirs();
        }*/
/*        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/");
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
    /* private void initBraodCast() {   //动态注册广播
         IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
         receiver = new TimeBroadcastReceiver();
         registerReceiver(receiver, filter);
     }*/
    private void initVideoView() {
       // mVideoView=(CustomVideoView) findViewById(R.id.id_mp4_vedio) ;
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
        if (pageStatus){
            myPager.startTimer();
        }
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
            //主控板通过串口1发送数据过来Android前端
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    //满5升，刷卡，刷卡结账时发过来的指令
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                   // MyLogUtil.delFile();     //清除两天前历史日志
                    //发送保持与后台建立长连接的心跳包
                    initCom105Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    //andorid端主动发送104之后，主控板回复204
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x06})){
                    //
                }
            }
            //android后台通过串口2发送数据过来前端
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    //主控板发105到前端，前端发到后端，后端回复205
                    initCom205Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    //前端主动发103给后端，后端回复203过来，保存出水和计费的状态到SP里
                    initCom203Data(packet2.getData());
                } else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());
                    }
                    //后端主动发104,让关机
                    initCom104Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    //后端主动发送102
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    //扫码取水，后端主动发送107
                    responseSever207(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x06})){
                    Log.d("com206", CreateOrderType.getPacketString(packet2));
                    //后端主动发送206，设置显示屏的系统时间
                    initComData206(packet2.getData());
                }
            }
        }
    }
    /*
      com206时间指令，设置系统时间
     */
    private void initComData206(ArrayList<Byte> data) {
        if (InstructUtil.TimeInstruct(data)){
            if (FormatToken.TimeType==1){
                if (!VariableUtil.setTimeStatus){
                    try {
                        DateTimeUtils.setDateTime(mContext,FormatToken.Year,FormatToken.Month,FormatToken.Day
                                ,FormatToken.Hour,FormatToken.Minute,FormatToken.Second);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    VariableUtil.setTimeStatus=true;
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
                myPager.stopTimer();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppoutWater.class);
                startActivityForResult(intent,1);
                CloseService();
                myPager.stopTimer();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliveryOutWater.class);
                startActivityForResult(intent,1);
                CloseService();
                myPager.stopTimer();
            }
        }
    }
    //Scan code
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            buyStatus=true;
            //民生宝来扫
            if (FormatToken.BusinessType==1){
                //已分为单位
                if (FormatToken.AppBalance<20){
                    //提示余额不足
                    Intent intent=new Intent(mContext,AppNotSufficient.class);
                    startActivityForResult(intent,1);
                    CloseService();
                    myPager.stopTimer();
                }else {
                    //给主控板发指令，取水
                    setBusiness(1);
                }
            }//配送端APP来扫
            else if (FormatToken.BusinessType==2){
                //给主控板发指令，取水
                setBusiness(2);
            }
        }
    }
    private void setBusiness(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business==1){
                    byte[] data= InstructUtil.setBusinessType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
                }else if (business==2){
                    byte[] data= InstructUtil.setBusinessType02();
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
        setEquipmentData(data.get(4));
        try {
            if(InstructUtil.EquipmentData(data)){
                String waterVolume=String.valueOf(FormatToken.WaterNum);
                String Time=String.valueOf(FormatToken.OutWaterTime);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.Volume,waterVolume);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.outWaterTime,Time);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ChargeMode,FormatToken.ChargeMode);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ShowTds,FormatToken.ShowTDS);
                VariableUtil.setEquipmentStatus=false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
     *设置出水时间
     *parame aByte  单价
     *
     */
    private void setEquipmentData(Byte aByte) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= InstructUtil.setEquipmentParameter(aByte);
                byte[] packet = PacketUtils.makePackage(frame, type, data);
                portService.sendToCom1(packet);
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
           /*  .....*/
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
        if (Switch==CLOSE_MECHINE&&DataCalculateUtils.isEvent(stringWork,0)){    //判断是否为关机指令
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,1);
            CloseService();
            myPager.stopTimer();

        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
        try {
            if(InstructUtil.ControlInstruct(data)){
                if (FormatToken.Balance<=1){
                    Intent intent=new Intent(mContext,NotSufficient.class);
                    startActivityForResult(intent,1);
                    CloseService();
                    myPager.stopTimer();
                }else {
                    String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)) {
                        if (FormatToken.ConsumptionType == 1) {
                            Intent intent = new Intent(mContext, IcCardoutWater.class);
                            startActivityForResult(intent, 1);
                            CloseService();
                            myPager.stopTimer();
                        } else if (FormatToken.ConsumptionType == 3) {
                            Intent intent = new Intent(mContext, AppoutWater.class);
                            startActivityForResult(intent, 1);
                            CloseService();
                            myPager.stopTimer();
                        } else if (FormatToken.ConsumptionType == 5) {
                            Intent intent = new Intent(mContext, DeliveryOutWater.class);
                            startActivityForResult(intent, 1);
                            CloseService();
                            myPager.stopTimer();
                        }
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
                        myPager.stopTimer();
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
            if (InstructUtil.StatusInstruct(data)){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuywater.class);
                    startActivityForResult(intent,1);
                    CloseService();
                    myPager.stopTimer();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void onHavaDataChange(boolean timeFlag) {
        super.onHavaDataChange(timeFlag);
        if (timeFlag){
            /*List<OrderInfo>  =getOrderDao().loadAll();
            if (infos.size()>=1&&infos!=null){
                if (sendStatus){
                    SendData(infos);
                }
            }*/
        }
    }
    //存储数据重发
    private void SendData(List<OrderInfo> infos) {
        if (portService!=null){
            if (portService.isConnection()){
                for (int i=0;i<infos.size();i++){
                    sendStatus=false;
                    byte[] data=infos.get(i).getOrderData();
                    portService.sendToCom2(data);
                    OrderInfo singgleData=infos.get(i);//单条订单记录
                    deleteData(singgleData);
                }
                sendStatus=true;
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
            Exitapp();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            StartBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            StartBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            StartBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            StartBuyWater();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void StartBuyWater() {
        Intent intent=new Intent(mContext,BuyWater.class);
        startActivityForResult(intent,1);
        CloseService();
        myPager.stopTimer();
    }
    private void Exitapp() {   //退出app
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
        //unregisterReceiver(receiver);
    }
}
