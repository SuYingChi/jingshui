package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.service.RestartApp;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.AppContext;
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.eventmanager.DateMassageEvent;
import com.msht.watersystem.eventmanager.MessageEvent;
import com.msht.watersystem.R;
import com.msht.watersystem.eventmanager.RestartAppEvent;
import com.msht.watersystem.utilpackage.BitmapViewListUtil;
import com.msht.watersystem.utilpackage.ByteUtils;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.ConsumeInformationUtils;
import com.msht.watersystem.utilpackage.CreatePacketTypeUtil;
import com.msht.watersystem.utilpackage.DataCalculateUtils;
import com.msht.watersystem.utilpackage.DateTimeUtils;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.utilpackage.MyLogUtil;
import com.msht.watersystem.utilpackage.MyServiceUtil;
import com.msht.watersystem.utilpackage.RestartAppUtil;
import com.msht.watersystem.utilpackage.ThreadPoolManager;
import com.msht.watersystem.utilpackage.VariableUtil;
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;
import com.msht.watersystem.service.ResendDataService;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.ToastUtils;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/20  
 */
public class MainWaterImageActivity extends BaseActivity implements Observer{
    private PortService portService;
    /**扫码发送104帧序*/
    private byte[]  mAppFrame;
    private double  volume = 0.00;
    private boolean bindStatus = false;
    private boolean buyStatus = false;
    private boolean pageStatus = true;
    private int timeCount=0;
    private ComServiceConnection serviceConnection;
    private MyScanCodeDownTimer myScanCodeDownTimer;
    /**夜间标志*/
    private boolean nightStatus=false;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_water_image);
        context=this;
        bindAndAddObserverToPortService();
        EventBus.getDefault().register(context);
        myScanCodeDownTimer=new MyScanCodeDownTimer(3000,1000);
        initService();

        //initBannerView();
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
        startService(resendDataService);
    }
    private void initBannerView() {
        ImageView advertImage = findViewById(R.id.textView);
        BannerM mBanner=findViewById(R.id.id_banner);
        List<Bitmap> imageViewList= BitmapViewListUtil.getBitmapListUtil(context);
        if (imageViewList!=null&& imageViewList.size() > 0) {
            mBanner.setBannerBeanList(VariableUtil.imageViewList)
                    .setDefaultImageResId(R.drawable.water_advertisement)
                    .setIndexPosition(BannerM.INDEX_POSITION_BOTTOM)
                    .setIndexColor(ContextCompat.getColor(context,R.color.colorPrimary))
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
                   // MyLogUtil.d("receiveCom1_104:",CreatePacketTypeUtil.getPacketString(packet1));
                    onCom1Received104DataFromControlBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x01, 0x05})) {
                    //重置倒计时 跳转到无法买水界面
                    onCom1Received105DataFromControlBoard(packet1.getData());
                } else if (Arrays.equals(packet1.getCmd(), new byte[]{0x02, 0x04})) {
                    //android端主动发送104之后，主控板回复204，跳转到IC卡买水或APP买水或现金出水界面
                   // MyLogUtil.d("receiveCom1_204:",CreatePacketTypeUtil.getPacketString(packet1));
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
                    //MyLogUtil.d("receiveCom2_104:",CreatePacketTypeUtil.getPacketString(packet2));
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
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray = packet2.getData();
                    //MyLogUtil.d("receiveCom2_107:",CreatePacketTypeUtil.getPacketString(packet2));
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
            if (data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE) {
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                String stringWork = DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                if (!DataCalculateUtils.isEvent(stringWork, 3)) {
                    /*打开屏幕背光*/
                    if (nightStatus){
                        if (!VariableUtil.isOpenBackLight){
                            onControlScreenBackground(1);
                        }
                        timeCount=0;
                    }
                    if (FormatInformationBean.Balance <= 1){
                        pageStatus=false;
                        Intent intent = new Intent(mContext, NotSufficientActivity.class);
                        unbindPortServiceAndRemoveObserver();
                        startActivity(intent);
                    }else {
                        if (FormatInformationBean.ConsumptionType == 1) {
                            pageStatus=false;
                            Intent intent = new Intent(mContext, IcCardOutWaterActivity.class);
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
                }else {
                    if (FormatInformationBean.ConsumptionType == 1){
                        onFinishOrder("0",FormatInformationBean.StringCardNo);
                    }else if (FormatInformationBean.ConsumptionType == 3){
                        onFinishOrder("1",FormatInformationBean.StringCustomerNo);
                    }else if (FormatInformationBean.ConsumptionType == 5){
                        onFinishOrder("1",FormatInformationBean.StringCustomerNo);
                    }else {
                        onFinishOrder("0",FormatInformationBean.StringCardNo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void onFinishOrder(String sign,String account){
        //刷卡结账
        calculateData();    //没联网计算取缓存数据
        double consumption = FormatInformationBean.ConsumptionAmount / 100.0;
        double waterVolume = FormatInformationBean.WaterYield * volume;
        String afterAmount = String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
        String afterWater = String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
        Intent intent = new Intent(mContext, PaySuccessActivity.class);
        intent.putExtra("afterAmount", afterAmount);
        intent.putExtra("afterWater", afterWater);
        intent.putExtra("mAccount", account);
        intent.putExtra("sign", sign);
        unbindPortServiceAndRemoveObserver();
        startActivity(intent);
        pageStatus=false;
    }
    private void onCom1Received105DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data!=null&&data.size()>= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE) {
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
            cancelCountDownTimer();
            if (buyStatus) {
                buyStatus = false;
                if (FormatInformationBean.ConsumptionType == 1) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, IcCardOutWaterActivity.class);
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
            if (data != null && data.size() >= ConstantUtil.REQUEST_MAX_SIZE) {
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                CachePreferencesUtil.getIntData(context,CachePreferencesUtil.PRICE,FormatInformationBean.PriceNum);
                CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_OUT_TIME,FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_NUM,FormatInformationBean.WaterNum);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
                CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.SHOW_TDS, FormatInformationBean.ShowTDS);
                CachePreferencesUtil.getIntData(this,CachePreferencesUtil.DEDUCT_AMOUNT,FormatInformationBean.DeductAmount);
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
        try{
            if (data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                String stringWork = DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
                int switch2 = ByteUtils.byteToInt(data.get(31));
                //判断是否为关机指令
                if (switch2 == ConstantUtil.CLOSE_MACHINE && DataCalculateUtils.isEvent(stringWork, 0)) {
                    pageStatus=false;
                    Intent intent = new Intent(mContext, CloseSystemActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                }
                if (ByteUtils.byteToInt(data.get(37))==2) {
                    VariableUtil.mNoticeText="此卡已挂失，如需取水请重新换卡!!";
                    VariableUtil.cardStatus=1;
                }else {
                    String workStatus= DataCalculateUtils.intToBinary(data.get(46));
                    if (ByteUtils.byteToInt(data.get(32))==2&&DataCalculateUtils.isEvent(workStatus,7)){
                        VariableUtil.mNoticeText="卡号"+String.valueOf(FormatInformationBean.StringCardNo)+"的用户，您的张卡有异常已禁止购水，请再次刷卡返还扣款，如有疑问请致电963666转2！";
                        VariableUtil.cardStatus=2;
                    }else {
                        VariableUtil.cardStatus=0;
                    }
                }
            }
        }catch (Exception e){
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
        if (ConsumeInformationUtils.controlModel(getApplicationContext(), data)) {
            /*  .....
            Log.d("FormatInformationBean=","waterVolume="+String.valueOf(FormatInformationBean.WaterNum)+",time="+String.valueOf(FormatInformationBean.OutWaterTime));
            */
        }
    }
    /**
     * 扫码业务指令
     * @param data 107指令
     */
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data != null && data.size()>= ConstantUtil.BUSINESS_MAX_SIZE) {
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this, CachePreferencesUtil.FIRST_OPEN, false);
            CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
            buyStatus = true;
            /*打开屏幕背光*/
            if (nightStatus){
                if (!VariableUtil.isOpenBackLight){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onControlScreenBackground(1);
                        }
                    }, 2500);
                }
                timeCount=0;
            }
            //民生宝来扫
            if (FormatInformationBean.BusinessType == 1) {
                //以分为单位
                if (FormatInformationBean.outWaterAmount <= 1) {
                    //提示余额不足
                    pageStatus=false;
                    Intent intent = new Intent(mContext, AppNotSufficientActivity.class);
                    unbindPortServiceAndRemoveObserver();
                    startActivity(intent);
                } else {
                    //给主控板发指令，取水
                    sendBuyWaterCommand104ToControlBoard(1,data);
                }
            }//配送端APP来扫
            else if (FormatInformationBean.BusinessType == 2) {
                //给主控板发指令，取水
                sendBuyWaterCommand104ToControlBoard(2,data);
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
    private void sendBuyWaterCommand104ToControlBoard(int business,ArrayList<Byte> dataList) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(getApplicationContext());
                mAppFrame=frame;
                byte[] type = new byte[]{0x01, 0x04};
                if (business == 1) {
                    byte[] data = FormatInformationUtil.setBuyWaterCommand104ConsumeType1(dataList);
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                    if (myScanCodeDownTimer!=null){
                        myScanCodeDownTimer.start();
                    }
                    //MyLogUtil.d("sendCom1_104:",ByteUtils.byteArrayToHexString(packet));
                } else if (business == 2) {
                    byte[] data = FormatInformationUtil.setBuyWaterCommand104ConsumeType2(dataList);
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                   // MyLogUtil.d("sendCom1_104:",ByteUtils.byteArrayToHexString(packet));
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

    /**
     * 自动发结账
     */
    private void onSettleAccountEndOutWater() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, ConstantUtil.END_BYTE);
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
    private void calculateData() {
        int mVolume = CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_NUM,5);
        int mTime =CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);
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
        if (portService!=null){
            try {
                byte[] frame = FrameUtils.getFrame(getApplicationContext());
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data = FormatInformationUtil.setCloseScreenData(status);
                byte[] packet = PacketUtils.makePackage(frame, type, data);
                portService.sendToControlBoard(packet);
                VariableUtil.isOpenBackLight = status == 1;
               // MyLogUtil.d("sendCom1backLight_104:",ByteUtils.byteArrayToHexString(packet));
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    class MyScanCodeDownTimer extends CountDownTimer {
        private MyScanCodeDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
        }
        @Override
        public void onFinish() {
            onSettleAccountEndOutWater();
            ToastUtils.onCustomToastLong("本次扫码无效，请稍候10秒重新扫描二维码");
            ToastUtils.onCustomToastLong("本次扫码无效，请稍候10秒重新扫描二维码");
        }
    }
    private void cancelCountDownTimer(){
        if (myScanCodeDownTimer!=null){
            myScanCodeDownTimer.cancel();
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageData(MessageEvent messageEvent) {
        List<OrderInfo> orderData=messageEvent.getMessage();
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageDate(DateMassageEvent event) {
        int status=event.getMessage();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRestartApp(RestartAppEvent event){
        if (event.getMessage()){
            RestartAppUtil.restartApp();
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
            if (!VariableUtil.isOpenBackLight){
                onControlScreenBackground(1);
            }
            timeCount=0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (!VariableUtil.isOpenBackLight){
                onControlScreenBackground(1);
            }
            timeCount=0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!VariableUtil.isOpenBackLight){
                onControlScreenBackground(1);
            }
            timeCount=0;
            startBuyWater();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F1) {
            if (!VariableUtil.isOpenBackLight){
                onControlScreenBackground(1);
            }
            timeCount=0;
            startBuyWater();
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
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
    private  void restartWaterApp(){
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        startActivity(launchIntent);
        //**杀死整个进程**//*
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    @Override
    protected void onStart() {
        super.onStart();
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
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        cancelCountDownTimer();
        ThreadPoolManager.getInstance(getApplicationContext()).onShutDown();
        EventBus.getDefault().unregister(getApplicationContext());
    }
}
