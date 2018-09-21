package com.msht.watersystem.functionActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.Interface.ResultListener;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.ConstantUtil;
import com.msht.watersystem.Utils.ConsumeInformationUtils;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.FormatInformationBean;
import com.msht.watersystem.Utils.FormatInformationUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.LEDView;

import java.lang.ref.WeakReference;
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
 * @date 2018/7/2  
 */
public class IcCardOutWaterActivity extends BaseActivity implements Observer{
    private TextView tvBalance;
    private TextView tvCardNo;
    private LEDView  ledWater,ledAmount;
    private TextView tvBlackCard;
    private TextView tvTime;
    /**
     * @parame  mStartFrame 灌装暂停发送104帧序
     */
    private byte[]  mStopFrame;
    /**
     * @parame  mStartFrame 灌装发送104帧序
     */
    private byte[]  mStartFrame;
    private int      second=0;
    private boolean  bindStatus=false;
    private String   mAccount;
    private double   volume=0.00;
    private double   priceNum=0.00;
    private int      mTime=30;
    /**出水时间超过10秒*/
    private int      overTime=40;
    private boolean  startOut=false;
    private String   afterAmount="0.0";
    private String   afterWater="0.0";
    private static  final int DELAY_TIME=15;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;
    private static final int SUCCESS=1;
    private static final int FAILURE=0;
    private final Handler finishHandler = new FinishHandler(this);
    private static class FinishHandler extends Handler{
        private final WeakReference<IcCardOutWaterActivity> icCardoutWaterActivityWeakReference;
        FinishHandler(IcCardOutWaterActivity icCardoutWaterActivity){
            icCardoutWaterActivityWeakReference = new WeakReference<IcCardOutWaterActivity>(icCardoutWaterActivity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    icCardoutWaterActivityWeakReference.get().finishOutWaterActivity("0");
                    break;
                case FAILURE:
                    icCardoutWaterActivityWeakReference.get().finishOutWaterActivity("0");
                    break;
                default:
                    break;
            }
        }
    }
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            second++;
            double volumeValue=second*volume;
            double amount=second*priceNum;
            double mVolume=DataCalculateUtils.getTwoDecimal(volumeValue);
            double mAmount=DataCalculateUtils.getTwoDecimal(amount);
            ledWater.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            ledAmount.setLedView(getString(R.string.default_bg_digital),String.valueOf(mAmount));
            if (second>=overTime){
                afterAmount=String.valueOf(mAmount);
                afterWater=String.valueOf(mVolume);
                finishOutWaterActivity("3");
            }
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iccard_outwater);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(185000,1000);
        initView();
        initBannerView();
        initWaterQuality();
        bindPortService();
    }
    private void initView() {
        mAccount=String.valueOf(FormatInformationBean.StringCardNo);
        tvTime =(TextView)findViewById(R.id.id_time) ;
        tvBalance =(TextView)findViewById(R.id.id_amount);
        tvCardNo =(TextView)findViewById(R.id.id_tv_cardno);
        tvBlackCard=findViewById(R.id.id_blackCard_text);
        ledAmount =(LEDView)findViewById(R.id.id_pay_amount);
        ledWater =(LEDView)findViewById(R.id.id_waster_yield);
        double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.Balance/100.0);
        tvBalance.setText(String.valueOf(balance));
        tvCardNo.setText(String.valueOf(FormatInformationBean.StringCardNo));
        mAccount=String.valueOf(FormatInformationBean.StringCardNo);
        myCountDownTimer.start();
    }
    private void initBannerView() {
        BannerM mBanner = (BannerM) findViewById(R.id.id_banner);
        ImageView advertImage = findViewById(R.id.textView);
        List<Bitmap> imageList=VariableUtil.imageViewList;
        if (imageList!=null&& imageList.size() > 0) {
            mBanner.setBannerBeanList(VariableUtil.imageViewList)
                    .setDefaultImageResId(R.drawable.water_advertisement)
                    .setIndexPosition(BannerM.INDEX_POSITION_BOTTOM)
                    .setIndexColor(getResources().getColor(R.color.colorPrimary))
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bindPortService();
    }
    private void bindPortService() {
        serviceConnection = new ComServiceConnection(IcCardOutWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            VariableUtil.mKeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    onCom1Received204DataFromControlBoard(packet1.getFrame());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    onCom1Received104DataFromControlBoard(packet1.getData(),packet1);
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControllBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    onCom2Received203DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    response207ToSever(packet2.getFrame());
                    onCom2Received107DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    onCom2Received104DataFromServer(packet2.getData());
                }
            }
        }
    }
    private void onCom2Received104DataFromServer(ArrayList<Byte> data) {
        //卡号为黑卡时，提示不可购水
        if (ByteUtils.byteToInt(data.get(37))==2) {
            tvBlackCard.setVisibility(View.VISIBLE);
            onDelayExecute();
        }else {
            tvBlackCard.setVisibility(View.GONE);
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
    private void onCom2Received102DataFromServer(ArrayList<Byte> data) {
        if (ConsumeInformationUtils.controlModel(mContext,data)){
            if (FormatInformationBean.ShowTDS==0){
                layoutTDS.setVisibility(View.GONE);
            }else {
                layoutTDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void response102ToServer(byte[] frame) {
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
    private void onCom1Received105DataFromControllBoard(ArrayList<Byte> data) {
        if (data!=null&&data.size()!=0){
            FormatInformationUtil.saveStatusInformationToFormatInformation(data);
            tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
            tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
            String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
            if (!DataCalculateUtils.isEvent(stringWork,6)){
                if (!startOut){
                    //扫码打水过程，水量不足，自动结账
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

        }
    }
    private void response207ToSever(byte[] frame) {
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
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (ConsumeInformationUtils.calaculateRecharge(data)){
            if (FormatInformationBean.BusinessType==3){
                FormatInformationBean.Balance= FormatInformationBean.Balance+ FormatInformationBean.rechargeAmount;
                Intent intent=new Intent(IcCardOutWaterActivity.this,PaySuccessActivity.class);
                intent.putExtra("afterAmount",afterAmount) ;
                intent.putExtra("afetrWater",afterWater);
                intent.putExtra("mAccount",mAccount);
                intent.putExtra("sign","2");
                startActivity(intent);
                finish();
            }
        }
    }
    /**
     * 控制板发送104指令过来COM1，里边包含着消费了多少元和出水量等一系列数据信息
     * @param data
     * @param packet1
     */
    private void onCom1Received104DataFromControlBoard(ArrayList<Byte> data, Packet packet1) {
        if (  data!=null&&data.size()>0){
            FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
            if (ByteUtils.byteToInt(data.get(15))==1){
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                if (DataCalculateUtils.isEvent(stringWork,3)){
                    myCountDownTimer.cancel();
                    handler.removeCallbacks(runnable);
                    double consumption= FormatInformationBean.ConsumptionAmount/100.0;
                    double waterVolume=consumption/0.3;
                    afterAmount=String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
                    afterWater=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
                    ledWater.setLedView(getString(R.string.default_bg_digital),afterWater);
                    //重置
                    second=0;
                    if (portService!=null){
                        if (!portService.isConnection()){
                            onSaveData(packet1);
                        }else {
                            finishOutWaterActivity("0");
                        }
                    }else {
                        onSaveData(packet1);
                    }
                   // finishOutWaterActivity("0");
                }else {
                    double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.Balance/100.0);
                    tvBalance.setText(String.valueOf(balance));
                    tvCardNo.setText(String.valueOf(FormatInformationBean.StringCardNo));
                    mAccount=String.valueOf(FormatInformationBean.StringCardNo);
                }
            }
        }
    }
    private void finishOutWaterActivity(String sign) {
        Intent intent=new Intent(IcCardOutWaterActivity.this,PaySuccessActivity.class);
        //消费了多少元
        intent.putExtra("afterAmount",afterAmount) ;
        //取了多少水
        intent.putExtra("afetrWater",afterWater);
        intent.putExtra("mAccount",mAccount);
        intent.putExtra("sign",sign);
        startActivity(intent);
        finish();
    }
    private void onSaveData(Packet packet1) {   //数据库操作保存数据
        VariableUtil.LongTimeSavaData(packet1, new ResultListener() {
            @Override
            public void onResultSuccess(String success) {
                Message msg = new Message();
                msg.obj=success;
                msg.what = SUCCESS;
                finishHandler.sendMessage(msg);
            }
            @Override
            public void onResultFail(String fail) {
                Message msg = new Message();
                msg.obj = fail;
                msg.what = FAILURE;
                finishHandler.sendMessage(msg);
            }
        });
    }
    private void onCom2Received203DataFromServer(ArrayList<Byte> data) {
        try {
            if(data!=null&&data.size()!=0){
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                mTime= FormatInformationBean.OutWaterTime;
                overTime=mTime+10;
                String waterVolume=String.valueOf(FormatInformationBean.WaterNum);
                String time=String.valueOf(FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.VOLUME,waterVolume);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.OUT_WATER_TIME,time);
                VariableUtil.setEquipmentStatus=false;
                volume=DataCalculateUtils.getWaterVolume(FormatInformationBean.WaterNum, FormatInformationBean.OutWaterTime);
                priceNum=DataCalculateUtils.getWaterPrice(FormatInformationBean.PriceNum);
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
                byte[] data= FormatInformationUtil.setEquipmentParameter(aByte);
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
    private void onCom1Received204DataFromControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mStartFrame)){
            if (startOut){
                myCountDownTimer.cancel();
                if (portService != null) {
                    calculateData();    //没联网计算取缓存数据
                    try {
                        byte[] mFrame = FrameUtils.getFrame(mContext);
                        byte[] type = new byte[]{0x01, 0x03};
                        byte[] packet = PacketUtils.makePackage(mFrame, type, null);
                        portService.sendToServer(packet);
                    } catch (CRCException e) {
                        e.printStackTrace();
                    } catch (FrameException e) {
                        e.printStackTrace();
                    } catch (CmdTypeException e) {
                        e.printStackTrace();
                    }
                    startOut=false;
                    handler.post(runnable);
                }
            }
        }
    }
    private void calculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.VOLUME,"5");
        String time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.OUT_WATER_TIME,"30");
        int mVolume=Integer.valueOf(waterVolume);
        mTime=Integer.valueOf(time);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
        overTime=mTime+10;
    }
    private void onDelayExecute() {
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                finish();
            }}, DELAY_TIME);
    }
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            String mFinishedText=millisUntilFinished/1000+"";
            tvTime.setText(mFinishedText);
        }
        @Override
        public void onFinish() {
            finishOutWaterActivity("0");
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            /*启动灌装 **/
            onStartOutWater();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
        }else if (keyCode==KeyEvent.KEYCODE_F1){
        }
        return super.onKeyDown(keyCode, event);
    }
    private void onStartOutWater() {
        if (portService != null) {
            startOut=true;
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                mStartFrame=frame;
                byte[] type = new byte[]{0x01, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, ConstantUtil.START_BYTE);
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

    private void endTimeCount() {
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        removeback();
        endTimeCount();
    }
    private void removeback() {
        if (handler!=null){
            handler.removeCallbacks(runnable);
        }
    }
}
