package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

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
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.FormatCommandUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.DateTimeUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.functionActivity.AppNotSufficientActivity;
import com.msht.watersystem.functionActivity.AppOutWaterActivity;
import com.msht.watersystem.functionActivity.BuyWaterActivity;
import com.msht.watersystem.functionActivity.CannotBuyWaterActivity;
import com.msht.watersystem.functionActivity.CloseSystemActivity;
import com.msht.watersystem.functionActivity.IcCardoutWaterActivity;
import com.msht.watersystem.functionActivity.NotSufficientActivity;
import com.msht.watersystem.functionActivity.DeliverOutWaterActivity;
import com.msht.watersystem.functionActivity.PaySuccessActivity;
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
public class MainSerialPortActivity extends BaseActivity  implements Observer{
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
        textView =findViewById(R.id.textView);
        initViewImages();
        bindPortService();
    }
    private void initViewImages() {
        myPager = findViewById(R.id.myvp);
        textView = findViewById(R.id.textView);
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
    private void bindPortService(){
        serviceConnection = new ComServiceConnection(MainSerialPortActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
        bindPortService();
        if (pageStatus){
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
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    //满5升，刷卡，刷卡结账时发过来的指令
                    onCom1Received104DataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                   // MyLogUtil.delFile();     //清除两天前历史日志
                    //重置倒计时 跳转到无法买水界面
                    onCom1Received105DataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    //andorid端主动发送104之后，主控板回复204，跳转到IC卡买水或APP买水或现金出水界面
                    onCom1Received204DataFromControllBoard();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x06})){
                    //
                }
            }
            //android后台通过串口2发送数据过来前端
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    //主控板发105到前端，前端发到后端，后端回复205
                    onCom2Received205DataFromServer();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    //前端主动发103给后端，后端回复203过来，保存出水和计费的状态到SP里
                    onCom2Received203DataFromServer(packet2.getData());
                } else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    //充值
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    //后端主动发104,让关机
                    onCom2Received104DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    //后端发102，回复202给后端
                    response202ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    //扫码取水，后端主动发送107，回复207给后端,发送104给主控板取水
                    response207ToServer(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    onCom2Received107DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x06})){
                    Log.d("com206", CreateOrderType.getPacketString(packet2));
                    //后端主动发送206，设置显示屏的系统时间
                    onCom2Received206DataFromServer(packet2.getData());
                }
            }
        }
    }
    private void onCom2Received206DataFromServer(ArrayList<Byte> data) {
        if (FormatCommandUtil.timeCommand(data)){
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
    private void onCom1Received204DataFromControllBoard() {
        if (buyStatus){
            buyStatus=false;
            if (FormatToken.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myPager.stopTimer();
            }
        }
    }
    //Scan code
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            buyStatus=true;
            //民生宝来扫
            if (FormatToken.BusinessType==1){
                //已分为单位
                if (FormatToken.AppBalance<20){
                    //提示余额不足
                    Intent intent=new Intent(mContext,AppNotSufficientActivity.class);
                    startActivityForResult(intent,1);
                    unbindPortServiceAndRemoveObserver();
                    myPager.stopTimer();
                }else {
                    //给主控板发指令，取水
                    sendBuyWaterCommandToControlBoard(1);
                }
            }//配送端APP来扫
            else if (FormatToken.BusinessType==2){
                //给主控板发指令，取水
                sendBuyWaterCommandToControlBoard(2);
            }
        }
    }
    private void sendBuyWaterCommandToControlBoard(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business==1){
                    byte[] data= FormatCommandUtil.setTransactionType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                }else if (business==2){
                    byte[] data= FormatCommandUtil.setTransactionType02();
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
        setEquipmentData(data.get(4));
        try {
            if(FormatCommandUtil.equipmentData(data)){
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
    private void setEquipmentData(Byte aByte) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= FormatCommandUtil.setEquipmentParameter(aByte);
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
        if (BusinessInstruct.ControlModel(mContext,data)){
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
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int switch2=ByteUtils.byteToInt(data.get(31));
        //判断是否为关机指令
        if (switch2==CLOSE_MECHINE&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,1);
            unbindPortServiceAndRemoveObserver();
            myPager.stopTimer();

        }
    }
    private void onCom1Received104DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if(FormatCommandUtil.convertCom1ReceivedDataToFormatToken(data)){
                if (FormatToken.Balance<=1){
                    Intent intent=new Intent(mContext,NotSufficientActivity.class);
                    startActivityForResult(intent,1);
                    unbindPortServiceAndRemoveObserver();
                    myPager.stopTimer();
                }else {
                    String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)) {
                        if (FormatToken.ConsumptionType == 1) {
                            Intent intent = new Intent(mContext, IcCardoutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        } else if (FormatToken.ConsumptionType == 3) {
                            Intent intent = new Intent(mContext, AppOutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        } else if (FormatToken.ConsumptionType == 5) {
                            Intent intent = new Intent(mContext, DeliverOutWaterActivity.class);
                            startActivityForResult(intent, 1);
                            unbindPortServiceAndRemoveObserver();
                            myPager.stopTimer();
                        }
                    }else {
                        //刷卡结账
                        calculateData();    //没联网计算取缓存数据
                        double consumption=FormatToken.ConsumptionAmount/100.0;
                        double waterVolume=FormatToken.WaterYield*volume;
                        String afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                        String afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                        String mAccount=String.valueOf(FormatToken.StringCardNo);
                        Intent intent=new Intent(mContext,PaySuccessActivity.class);
                        intent.putExtra("afterAmount",afterAmount) ;
                        intent.putExtra("afetrWater",afterWater);
                        intent.putExtra("mAccount",mAccount);
                        intent.putExtra("sign","0");
                        startActivityForResult(intent,1);
                        unbindPortServiceAndRemoveObserver();
                        myPager.stopTimer();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void calculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume);
        int mTime=Integer.valueOf(time);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void onCom2Received205DataFromServer() {

    }
    private void onCom1Received105DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if (FormatCommandUtil.convertStatusCommandToFormatToken(data)){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,1);
                    unbindPortServiceAndRemoveObserver();
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
                    sendData(infos);
                }
            }*/
        }
    }
    //存储数据重发
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
            exitapp();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            startBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            startBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            startBuyWater();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            startBuyWater();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startBuyWater() {
        Intent intent=new Intent(mContext,BuyWaterActivity.class);
        startActivityForResult(intent,1);
        unbindPortServiceAndRemoveObserver();
        myPager.stopTimer();
    }
    private void exitapp() {   //退出app
        System.exit(0);
    }
    private void unbindPortServiceAndRemoveObserver(){
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
                if (Context.AUDIO_SERVICE.equals(name)){
                    return getApplicationContext().getSystemService(name);
                }
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
        unbindPortServiceAndRemoveObserver();
    }
}
