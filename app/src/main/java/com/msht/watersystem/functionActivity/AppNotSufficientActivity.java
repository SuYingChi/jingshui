package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
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
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.ConsumeInformationUtils;
import com.msht.watersystem.utilpackage.ByteUtils;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.CreatePacketTypeUtil;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.utilpackage.DataCalculateUtils;
import com.msht.watersystem.utilpackage.MyLogUtil;
import com.msht.watersystem.utilpackage.VariableUtil;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.ToastUtils;

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
public class AppNotSufficientActivity extends BaseActivity implements Observer {
    private boolean  buyStatus=false;
    private double   volume=0.00;
    private TextView tvTime;
    private TextView tvBalance;
    private TextView tvCustomerNo;
    private boolean  bindStatus=false;
    /**
     * @parame  mAppFrame 扫码发送104帧序
     */
    private byte[]  mAppFrame;
    private Context mContext;
    private MyScanCodeDownTimer myScanCodeDownTimer;
    private MyCountDownTimer myCountDownTimer;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_not_sufficient);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        myScanCodeDownTimer=new MyScanCodeDownTimer(3000,1000);
        initView();
       // initBannerView();
        initWaterQuality();
        bindPortService();
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
    private void bindPortService() {
        serviceConnection = new ComServiceConnection(AppNotSufficientActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
            boolean skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("receiveCom1_104:",CreatePacketTypeUtil.getPacketString(packet1));
                    onCom1Received104DataFromControlBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControlBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                   // MyLogUtil.d("receiveCom1_204:",CreatePacketTypeUtil.getPacketString(packet1));
                    onCom1Received204DataFromControlBoard(packet1.getFrame());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    onCom2Received205DataFromServer();
                }else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("receiveCom2_104:",CreatePacketTypeUtil.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    onCom2Received104DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    //MyLogUtil.d("receiveCom2_107:",CreatePacketTypeUtil.getPacketString(packet2));
                    onCom2Received107DataFromServer(packet2.getData());
                }
            }
        }
    }
    private void onCom1Received204DataFromControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mAppFrame)){
            cancelCountDownTimer();
            if (buyStatus){
                buyStatus=false;
                if (FormatInformationBean.ConsumptionType==1){
                    //进入刷卡购水页面
                    Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                    startActivity(intent);
                    finish();
                }else if (FormatInformationBean.ConsumptionType==3){
                    //进入app购水页面
                    Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                    startActivity(intent);
                    finish();
                }else if (FormatInformationBean.ConsumptionType==5){
                    //进入配送取水页面
                    Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }
    }
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data!=null&&data.size()>=ConstantUtil.BUSINESS_MAX_SIZE){
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putChargeMode(this, CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
            if (FormatInformationBean.BusinessType==3){
                FormatInformationBean.Balance= FormatInformationBean.Balance+ FormatInformationBean.rechargeAmount;
                Intent intent=new Intent(AppNotSufficientActivity.this,PaySuccessActivity.class);
                intent.putExtra("afterAmount","0.0") ;
                intent.putExtra("afterWater","0");
                intent.putExtra("mAccount","0.0");
                intent.putExtra("sign","2");
                startActivity(intent);
                finish();
            }else {
                VariableUtil.byteArray.clear();
                VariableUtil.byteArray=data;
                CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
                buyStatus=true;
                if (FormatInformationBean.BusinessType==1){
                    if (FormatInformationBean.outWaterAmount<=1){
                        double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.Balance/100.0);
                        tvBalance.setText(String.valueOf(balance));
                        tvCustomerNo.setText(FormatInformationBean.StringCardNo);
                    }else {
                        sendBuyWaterCommand104ToControlBoard(1,data);
                    }
                }else if (FormatInformationBean.BusinessType==2){
                    sendBuyWaterCommand104ToControlBoard(2,data);
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
    private void onCom1Received104DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if(data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                if (!DataCalculateUtils.isEvent(stringWork,3)){
                    if (FormatInformationBean.Balance<=1){
                        if (FormatInformationBean.ConsumptionType==1){
                            Intent intent=new Intent(mContext,NotSufficientActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else {
                            double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.Balance/100.0);
                            tvBalance.setText(String.valueOf(balance));
                        }
                    }else {
                        if (FormatInformationBean.ConsumptionType==1){
                            Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else if (FormatInformationBean.ConsumptionType==3){
                            Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else if (FormatInformationBean.ConsumptionType==5){
                            Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
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
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onFinishOrder(String sign,String account){
        calculateData();    //没联网计算取缓存数据
        double consumption= FormatInformationBean.ConsumptionAmount/100.0;
        double waterVolume= FormatInformationBean.WaterYield*volume;
        String afterAmount=String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
        String afterWater=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
        Intent intent=new Intent(mContext,PaySuccessActivity.class);
        intent.putExtra("afterAmount",afterAmount) ;
        intent.putExtra("afterWater",afterWater);
        intent.putExtra("mAccount",account);
        intent.putExtra("sign",sign);
        startActivityForResult(intent,1);
        endTimeCount();
        finish();
    }
    private void calculateData() {
        int mVolume=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_NUM,5);
        int mTime=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
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
    private void onCom1Received105DataFromControlBoard(ArrayList<Byte> data) {
        try {
            if (data!=null&&data.size()>= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE){
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
                tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,1);
                    unbindPortServiceAndRemoveObserver();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onCom2Received205DataFromServer() {}
    private void onCom2Received104DataFromServer(ArrayList<Byte> data) {
        try{
            if (data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
                int switchStatus=ByteUtils.byteToInt(data.get(31));
                if (switchStatus==2&&DataCalculateUtils.isEvent(stringWork,0)){
                    Intent intent=new Intent(mContext, CloseSystemActivity.class);
                    startActivityForResult(intent,1);
                    finish();
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
    private void unbindPortServiceAndRemoveObserver(){
        if (serviceConnection != null && portService != null) {
            if (bindStatus){
                bindStatus=false;
                portService.removeObserver(this);
                unbindService(serviceConnection);
            }
        }
    }
    class MyCountDownTimer extends CountDownTimer {
         MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            tvTime.setText(millisUntilFinished/1000+"");
        }
        @Override
        public void onFinish() {
            finish();
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
            ToastUtils.onToastLong("本次扫码无效，请稍候10秒重新扫描二维码");
            ToastUtils.onToastLong("本次扫码无效，请稍候10秒重新扫描二维码");
        }
    }
    private void initView() {
        tvTime =(TextView)findViewById(R.id.id_time) ;
        tvBalance =(TextView)findViewById(R.id.id_balance_amount);
        tvCustomerNo =(TextView)findViewById(R.id.id_tv_customerNo);
        double balance= DataCalculateUtils.getTwoDecimal(FormatInformationBean.AppBalance/100.0);
        tvBalance.setText(String.valueOf(balance));
        tvCustomerNo.setText(FormatInformationBean.StringCustomerNo);
        if (myCountDownTimer!=null){
            myCountDownTimer.start();
        }
    }
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
    }
    private void cancelCountDownTimer(){
        if (myScanCodeDownTimer!=null){
            myScanCodeDownTimer.cancel();
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            showTips();
        }
        return true;
    }
    private void showTips() {
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        endTimeCount();
        cancelCountDownTimer();
    }
}
