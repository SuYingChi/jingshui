package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.ConsumeInformationUtils;
import com.msht.watersystem.utilpackage.ByteUtils;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.utilpackage.DataCalculateUtils;
import com.msht.watersystem.utilpackage.VariableUtil;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.LEDView;

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
 * @date 2017/11/21  
 */
public class DeliverOutWaterActivity extends BaseActivity implements Observer{
    private View      layoutFinish;
    private TextView  tvTime;
    private TextView  tvFinishOrder;
    private TextView  tvTitle;
    private TextView  tvTip;
    private Button    btnTip;
    private LEDView   leWater;
    private boolean   bindStatus=false;
    /**
     * @parame  mAppFrame 结账发送104帧序
     */
    private byte[]  mEndFrame;
    /**
     * @parame  mStartFrame 灌装暂停发送104帧序
     */
    private byte[]  mStopFrame;
    /**
     * @parame  mStartFrame 灌装发送104帧序
     */
    private byte[]  mStartFrame;
    private int      second=0;
    private double   volume=0.00;
    private int      ensureState =0;
    private boolean  finishStatus=false;
    private boolean  tipStatus=true;
    private Context  mContext;
    private PortService portService;
    private CountDownTimer mTimer;
    private MyCountDownTimer myCountDownTimer;
    private ComServiceConnection serviceConnection;
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            second++;
            double volumeValue=second*volume;
            double mVolume=DataCalculateUtils.getTwoDecimal(volumeValue);
            leWater.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliveryout_water);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        initView();
        initBannerView();
        initWaterQuality();
        bindPortService();
        startCountDownTime(35);
    }
    private void initView() {
        layoutFinish =findViewById(R.id.id_re_code);
        TextView tvCardNo =findViewById(R.id.id_tv_customerNo);
        TextView tvVolume =findViewById(R.id.id_getwater_volume);
        TextView tvOrderNo =findViewById(R.id.id_tv_orderNo);
        leWater =findViewById(R.id.id_waster_yield);
        tvTitle =findViewById(R.id.id_get_text);
        tvTip =findViewById(R.id.id_tip_text);
        btnTip =findViewById(R.id.id_tip_button);
        tvFinishOrder =findViewById(R.id.id_finish_order);
        tvTime =findViewById(R.id.id_time);
        double weight= DataCalculateUtils.getTwoDecimal(FormatInformationBean.Waterweight/100.0);
        tvCardNo.setText(String.valueOf(FormatInformationBean.StringCardNo));
        String weightText=String.valueOf(weight)+"升";
        tvVolume.setText(weightText);
        tvOrderNo.setText(FormatInformationBean.OrderNoString);
    }
    private void initBannerView() {
        BannerM mBanner = (BannerM) findViewById(R.id.id_banner);
        ImageView advertImage = findViewById(R.id.textView);
        List<Bitmap> imageList= VariableUtil.imageViewList;
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
    private void bindPortService(){
        serviceConnection = new ComServiceConnection(DeliverOutWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
                    onCom1Received104DataFromControlBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControlBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    if (mTimer !=null){
                        mTimer.cancel();
                    }
                    onCom2Received207DataFromServer();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }
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
    private void onCom2Received102DataFromServer(ArrayList<Byte> data) {
        if (ConsumeInformationUtils.controlModel(mContext,data)){
            if (FormatInformationBean.ShowTDS==0){
                layoutTDS.setVisibility(View.GONE);
            }else {
                layoutTDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void onCom1Received105DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data!=null&&data.size()!=0){
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
                tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    settleAccount();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onCom1Received204DataFromControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mStartFrame)){
            if (mTimer !=null){
                mTimer.cancel();    //接收到启动灌装204计时停止，
            }
            volume= DataCalculateUtils.getWaterVolume(FormatInformationBean.WaterNum, FormatInformationBean.OutWaterTime);
            handler.post(runnable);
        }else if (Arrays.equals(frame,mStopFrame)){
            removeBack();
        }else {
            Toast.makeText(mContext,"无任何操作",Toast.LENGTH_SHORT).show();
        }
    }
    private void onCom1Received104DataFromControlBoard(ArrayList<Byte> data) {
        if ( data!=null&&data.size()>0){
            FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
            int businessType=ByteUtils.byteToInt(data.get(15));
            if (businessType==3){
                removeBack();
                int amountAfter= FormatInformationBean.AfterAmount;
                int consumption= FormatInformationBean.ConsumptionAmount;
                int waterWeight= FormatInformationBean.WaterYield;
                if (consumption<=0){
                    tvTitle.setText("取水未完成");
                    String orderNoText="当前订单"+ FormatInformationBean.OrderNoString+"未取水，请再次扫码取水";
                    tvFinishOrder.setText(orderNoText);
                }else {
                    tvTitle.setText("取水完成");
                    String orderNoText="当前订单"+ FormatInformationBean.OrderNoString+"取水结束";
                    tvFinishOrder.setText(orderNoText);
                }
                settleServer(amountAfter,consumption,waterWeight);
            }else if (businessType==1){
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                if (!DataCalculateUtils.isEvent(stringWork,3)){
                    /*余额不足*/
                    if (FormatInformationBean.Balance<2){
                        Intent intent=new Intent(mContext,NotSufficientActivity.class);
                        startActivity(intent);
                        finish();
                    }else {
                        Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }else {
                    //刷卡结账
                    calculateData();    //没联网计算取缓存数据
                    double consumption= FormatInformationBean.ConsumptionAmount/100.0;
                    double waterVolume= FormatInformationBean.WaterYield*volume;
                    String afterAmount=String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
                    String afterWater=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
                    String mAccount=String.valueOf(FormatInformationBean.StringCardNo);
                    Intent intent=new Intent(mContext,PaySuccessActivity.class);
                    intent.putExtra("afterAmount",afterAmount) ;
                    intent.putExtra("afterWater",afterWater);
                    intent.putExtra("mAccount",mAccount);
                    intent.putExtra("sign","0");
                    startActivity(intent);
                    finish();
                }
            }
        }
    }
    private void calculateData() {
        int mVolume=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_NUM,5);
        int mTime=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void onCom2Received207DataFromServer() {
        tipStatus=true;
        //返回按键有效
        finishStatus=true;
        tvTip.setText("1、返回首页请点击其他按键");
        btnTip.setText("按键");
        myCountDownTimer.start();
        layoutFinish.setVisibility(View.VISIBLE);
    }
    private void settleAccount() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= FormatInformationUtil.settle();
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
    private void settleServer(int mAfterAmount,int amount,int waterWeight) {
        mTimer.start();//开始计时
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x07};
                if (VariableUtil.byteArray!=null&&VariableUtil.byteArray.size()!=0){
                    byte[] data= DataCalculateUtils.onArrayToByte(VariableUtil.byteArray);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(mAfterAmount);
                    byte[] water=ByteUtils.intToByte2(waterWeight);
                    data[13]=consumption[0];
                    data[14]=consumption[1];
                    data[15]=consumption[2];
                    data[16]=consumption[3];
                    data[17]=afterConsumption[0];
                    data[18]=afterConsumption[1];
                    data[19]=afterConsumption[2];
                    data[20]=afterConsumption[3];
                    data[28]=water[0];
                    data[29]=water[1];
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToServer(packet);
                }else {
                    byte[] data= ConsumeInformationUtils.settleData(FormatInformationBean.phoneType, FormatInformationBean.orderType);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(mAfterAmount);
                    byte[] water=ByteUtils.intToByte2(waterWeight);
                    data[13]=consumption[0];
                    data[14]=consumption[1];
                    data[15]=consumption[2];
                    data[16]=consumption[3];
                    data[17]=afterConsumption[0];
                    data[18]=afterConsumption[1];
                    data[19]=afterConsumption[2];
                    data[20]=afterConsumption[3];
                    data[28]=water[0];
                    data[29]=water[1];
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToServer(packet);
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
    private void startCountDownTime(final long time) {
        mTimer =new CountDownTimer(time*1000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String secondText=millisUntilFinished/1000+"秒";
                tvTime.setText(secondText);
            }
            @Override
            public void onFinish() {
                if (handler!=null){
                    handler.removeCallbacks(runnable);
                }
                onCom2Received207DataFromServer();
            }
        };
        mTimer.start();
    }
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            String mUntilFinishedText=millisUntilFinished/1000+"s";
            tvTime.setText(mUntilFinishedText);
        }
        @Override
        public void onFinish() {
            if (tipStatus){
                finish();
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (ensureState ==0){
                onStartOutWater();
                ensureState =1;
                myCountDownTimer.cancel();
                layoutFinish.setVisibility(View.GONE);
            }else if (ensureState ==1){
                ensureState =0;
                onStopOutWater();
                onTipDialog();
                removeBack();
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            if (finishStatus){
                finish();
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            if (finishStatus){
                finish();
            }
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            if (finishStatus){
                finish();
            }
        }
        return false;
    }

    private void onStartOutWater() {
        if (portService != null) {
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
    private void onStopOutWater() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                mStopFrame=frame;
                byte[] type = new byte[]{0x01, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, ConstantUtil.STOP_BYTE);
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
    private void onTipDialog() {
        tipStatus=false;
        tvTitle.setText("暂停取水");
        tvFinishOrder.setText("请您在30s内重启灌装");
        tvTip.setText("计时超时将终止取水");
        btnTip.setText("提示");
        layoutFinish.setVisibility(View.VISIBLE);
        myCountDownTimer.start();
    }
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
        if (mTimer !=null){
            mTimer.cancel();
        }
    }
    private void removeBack() {
        if (handler!=null){
            handler.removeCallbacks(runnable);
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
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        removeBack();
        endTimeCount();
    }
}
