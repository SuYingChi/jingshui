package com.msht.watersystem.functionActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
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
import com.msht.watersystem.Interface.ResultListener;
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
 * @date 2017/11/2  
 */
public class AppOutWaterActivity extends BaseActivity implements Observer{
    private int      second=0;
    private boolean  bindStatus=false;
    private double   volume=0.00;
    private double   priceNum=0.00;
    private String   mAccount;
    private View     layoutNotice;
    private TextView tvTime;
    private LEDView leWater, leAmount;
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
    /**暂停取水计时 */
    private MyCountDownTimer myCountDownTimer;
    private CountDownTimer mTimer;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private int ensureState =0;
    private int receiveState =0;
    private final UpDataHandler handlerStop = new UpDataHandler(this);
    private static class UpDataHandler extends Handler{
        private WeakReference<AppOutWaterActivity> mWeakReference;
        public UpDataHandler(AppOutWaterActivity appOutWater) {
            mWeakReference = new WeakReference<AppOutWaterActivity>(appOutWater);
        }
        @Override
        public void handleMessage(Message msg){
            AppOutWaterActivity activity =mWeakReference.get();
            // the referenced object has been cleared
            if (activity == null||activity.isFinishing()) {
                return;
            }
            switch (msg.what){
                case 1:
                    activity.handler.removeCallbacks(activity.runnable);
                    break;
                case 2:
                    activity.handler.removeCallbacks(activity.runnable);
                    double waterVolume= FormatInformationBean.WaterYield*activity.volume;
                    String water=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
                    activity.leWater.setLedView(activity.getString(R.string.default_bg_digital),String.valueOf(water));
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            second++;
            double volumeValue=second*volume;
            double amount=second*priceNum;
            double mVolume=DataCalculateUtils.getTwoDecimal(volumeValue);
            double mAmount=DataCalculateUtils.getTwoDecimal(amount);
            leWater.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            leAmount.setLedView(getString(R.string.default_bg_digital),String.valueOf(mAmount));
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appout_water);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        initView();
       // initBannerView();
        initWaterQuality();
        bindPortService();
        startCountDownTime(35);
    }
    private void initBannerView() {
        BannerM mBanner =findViewById(R.id.id_banner);
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
    private void initView() {
        layoutNotice =findViewById(R.id.id_layout_dialog);
        TextView tvBalance =(TextView)findViewById(R.id.id_amount);
        TextView tvCardNo =(TextView)findViewById(R.id.id_tv_cardno);
        tvTime =(TextView)findViewById(R.id.id_time) ;
        leAmount =(LEDView)findViewById(R.id.id_pay_amount);
        leWater =(LEDView)findViewById(R.id.id_waster_yield);
        double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.AppBalance/100.0);
        String balanceText=String.valueOf(balance)+"元";
        tvBalance.setText(balanceText);
        mAccount=String.valueOf(FormatInformationBean.StringCardNo);
        tvCardNo.setText(mAccount);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bindPortService();
    }
    private void bindPortService() {
        serviceConnection = new ComServiceConnection(AppOutWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
                    onCom1Received104DataFromControlBoard(packet1.getData(),packet1.getFrame());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControlBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    if (mTimer !=null){
                        mTimer.cancel();    //接收到207计时停止
                    }
                    onCom2Received207DataFromServer();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    onCom2Received104Data(packet2.getData());
                }
            }
        }
    }
    private void onCom2Received104Data(ArrayList<Byte> data) {
        try{
            if (data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
                int switchStatus=ByteUtils.byteToInt(data.get(31));
                if (switchStatus==2&&DataCalculateUtils.isEvent(stringWork,0)){
                    Intent intent=new Intent(mContext, CloseSystemActivity.class);
                    startActivityForResult(intent,1);
                    finish();
                }
            }
        }catch (Exception e){
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
            if (data!=null&&data.size()>= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE){
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
                tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    if (receiveState !=0){
                        //扫码打水过程，水量不足，自动结账
                        ensureState =2;
                        receiveState =3;
                        onSettleAccountEndOutWater();
                    }else {
                        Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                        startActivityForResult(intent,1);
                        finish();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onCom1Received104DataFromControlBoard(ArrayList<Byte> data, byte[] frame) {
            if(data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                int businessType=ByteUtils.byteToInt(data.get(15));
                //扫码结账
                if (businessType==3){
                    if (ensureState ==2){
                        Message msg=new Message();
                        msg.what=2;
                        handlerStop.sendMessage(msg);
                        int amountAfter= FormatInformationBean.AfterAmount;
                        int consumption= FormatInformationBean.ConsumptionAmount;
                        int chargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGE_MODE,0);
                        if (chargeMode!=1){
                            FormatInformationBean.AppBalance= FormatInformationBean.AppBalance-consumption;
                        }
                        if (mTimer !=null){
                            mTimer.cancel();
                        }
                        settleServer(amountAfter,consumption,frame);
                    }else {
                        Message msg=new Message();
                        msg.what=2;
                        handlerStop.sendMessage(msg);
                        int amountAfter= FormatInformationBean.AfterAmount;
                        int consumption= FormatInformationBean.ConsumptionAmount;
                        int chargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGE_MODE,0);
                        if (chargeMode!=1){
                            FormatInformationBean.AppBalance= FormatInformationBean.AppBalance-consumption;
                        }
                        if (mTimer !=null){
                            mTimer.cancel();
                        }
                        settleServer(amountAfter,consumption,frame);
                    }
                }else if (businessType==1){
                    String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)){
                        if (FormatInformationBean.Balance<=1){
                            Intent intent=new Intent(mContext,NotSufficientActivity.class);
                            startActivity(intent);
                            if (myCountDownTimer != null) {
                                myCountDownTimer.cancel();
                            }
                            finish();
                        }else {
                            Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }else {
                        calculateData();    //没联网计算取缓存数据
                        double consumption= FormatInformationBean.ConsumptionAmount/100.0;
                        double waterVolume=consumption/0.3;
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
    private void onCom2Received207DataFromServer() {
        double consumption= FormatInformationBean.ConsumptionAmount/100.0;
        double waterVolume= FormatInformationBean.WaterYield*volume;
        String afterAmount=String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
        if (waterVolume==0){
            afterAmount="0.0";
        }
        if (CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGE_MODE,0)==1){
            afterAmount="0.00";
        }
        String afterWater=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
        Intent intent=new Intent(mContext,PaySuccessActivity.class);
        intent.putExtra("afterAmount",afterAmount);
        intent.putExtra("afterWater",afterWater);
        intent.putExtra("mAccount",mAccount);
        intent.putExtra("sign","1");
        startActivity(intent);
        finish();
    }
    private void onCom1Received204DataFromControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mStartFrame)){
            if (mTimer !=null){
                mTimer.cancel();    //接收到启动灌装204计时停止，
            }
            volume=DataCalculateUtils.getWaterVolume(FormatInformationBean.WaterNum, FormatInformationBean.OutWaterTime);
            priceNum=DataCalculateUtils.getWaterPrice(FormatInformationBean.PriceNum);
            handler.post(runnable);
        }else if (Arrays.equals(frame,mStopFrame)){
            Message msg=new Message();
            msg.what=1;
            handlerStop.sendMessage(msg);
        }else if (Arrays.equals(frame,mEndFrame)){
            ensureState =0;
            Message msg=new Message();
            msg.what=2;
            handlerStop.sendMessage(msg);
            if (myCountDownTimer != null) {
                myCountDownTimer.cancel();
            }
            layoutNotice.setVisibility(View.GONE);
        }
    }
    private void calculateData() {
        int mVolume=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_NUM,5);
        int mTime=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);;
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void settleServer(int afterAmount,int amount,byte[] frame) {
        if (mTimer!=null){
            mTimer.start();//开始计时
        }
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x01, 0x07};
                if (VariableUtil.byteArray!=null&&VariableUtil.byteArray.size()!=0){
                    byte[] data= DataCalculateUtils.onArrayToByte(VariableUtil.byteArray);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(afterAmount);
                    byte[] water=ByteUtils.intToByte2(FormatInformationBean.WaterYield);
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
                    //没联网数据保存
                    if (!portService.isConnection()){
                        VariableUtil.requestLongTimeSaveData(packet);
                    }
                    portService.sendToServer(packet);
                }else {
                    byte[] data= ConsumeInformationUtils.settleData(FormatInformationBean.phoneType, FormatInformationBean.orderType);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(afterAmount);
                    byte[] water=ByteUtils.intToByte2(FormatInformationBean.WaterYield);
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
                    //没联网数据保存
                    if (!portService.isConnection()){
                        VariableUtil.requestLongTimeSaveData(packet);
                    }
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

    private void onSaveData(byte[] packet) {
        VariableUtil.requestLongTimeSaveData(packet);
    }
    private void onSettleAccountEndOutWater() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                mEndFrame=frame;
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
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            String millisUntilFinishedText=millisUntilFinished/1000+"s";
            tvTime.setText(millisUntilFinishedText);
        }
        @Override
        public void onFinish() {
            layoutNotice.setVisibility(View.GONE);
            if (receiveState ==2){
                ensureState =2;
                receiveState =3;
                if (mTimer!=null){
                    mTimer.start();
                }
               // onSettleAccountEndOutWater();
            }
        }
    }

    private void startCountDownTime(final long time) {
        mTimer =new CountDownTimer(time*1000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String millisUntilFinishedText=millisUntilFinished/1000+"秒";
                tvTime.setText(millisUntilFinishedText);
            }
            @Override
            public void onFinish() {
                if (handler!=null){
                    handler.removeCallbacks(runnable);
                }
                onCom2Received207DataFromServer();
            }
        };
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (ensureState ==0){
                if (portService != null) {
                    //正在打水
                    ensureState =1;
                    receiveState =1;
                    onStartOutWater();
                    if (myCountDownTimer!=null){
                        myCountDownTimer.onFinish();//计时停止
                    }
                }
            }else if (ensureState ==1){
                if (portService != null) {
                    //停止打水
                    ensureState =0;
                    receiveState =2;
                    onStopOutWater();
                    layoutNotice.setVisibility(View.VISIBLE);
                    if (myCountDownTimer!=null){
                        myCountDownTimer.start();
                    }

                }
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            //结账打水
            ensureState =2;
            receiveState =3;
            if (myCountDownTimer!=null){
                myCountDownTimer.onFinish();
            }
            onSettleAccountEndOutWater();
        }
        return super.onKeyDown(keyCode, event);
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
    private void showTips() {
        finish();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        removeBack();
        endTimeCount();
    }
}
