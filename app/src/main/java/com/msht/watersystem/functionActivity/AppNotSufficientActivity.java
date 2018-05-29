package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
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
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.InstructUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.MyImgScroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AppNotSufficientActivity extends BaseActivity implements Observer {

    private String   mAccount="0.0";
    private String   afterAmount="0.0";
    private String   afterWater="0.0";
    private boolean  buyStatus=false;
    private double   volume=0.00;
    private TextView tv_time;
    private TextView tv_balance;
    private TextView tv_customerNo;
    private boolean  bindStatus=false;
    private MyImgScroll myPager;
    private List<View> listViews;
    private ImageView textView;
    private Context mContext;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private PortService portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_not_sufficient);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        initView();
        initWaterQuality();
        OpenService();
    }
    private void OpenService() {
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
                   // MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    initCom204Data();
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    initCom205Data();
                }else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                  //  MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                  //  MyLogUtil.d("服务端业务指令107：",CreateOrderType.getPacketString(packet2));
                    ResponseSever(packet2.getFrame());
                    initCom107Data(packet2.getData());
                }
            }
        }
    }

    private void initCom204Data() {
        if (buyStatus){
            buyStatus=false;
            if (FormatToken.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWater.class);    //进入刷卡购水页面
                startActivityForResult(intent,1);
                finish();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppOutWaterActivity.class);       //进入app购水页面
                startActivityForResult(intent,1);
                finish();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);  //进入配送取水页面
                startActivityForResult(intent,1);
                finish();
            }
        }
    }
    private void ResponseSever(byte[] frame) {
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
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            if (FormatToken.BusinessType==3){
                FormatToken.Balance=FormatToken.Balance+FormatToken.rechargeAmount;
                Intent intent=new Intent(AppNotSufficientActivity.this,PaySuccessActivity.class);
                intent.putExtra("afterAmount",afterAmount) ;
                intent.putExtra("afetrWater",afterWater);
                intent.putExtra("mAccount",mAccount);
                intent.putExtra("sign","2");
                startActivity(intent);
                finish();
            }else {
                VariableUtil.byteArray.clear();
                VariableUtil.byteArray=data;
                CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);//数据更变
                buyStatus=true;
                if (FormatToken.BusinessType==1){
                    if (FormatToken.AppBalance<20){
                        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                        tv_balance.setText(String.valueOf(balance));
                        tv_customerNo.setText(FormatToken.StringCardNo);
                    }else {
                        setBusiness(1);
                    }
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
            if (FormatToken.ShowTDS==0){
                layout_TDS.setVisibility(View.GONE);
            }else {
                layout_TDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
        try {
            if(InstructUtil.ControlInstruct(data)){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                if (!DataCalculateUtils.isEvent(stringWork,3)){
                    if (FormatToken.Balance<=1){
                        if (FormatToken.ConsumptionType==1){
                            Intent intent=new Intent(mContext,NotSufficientActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else {
                            double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                            tv_balance.setText(String.valueOf(balance));
                        }
                    }else {
                        if (FormatToken.ConsumptionType==1){
                            Intent intent=new Intent(mContext,IcCardoutWater.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else if (FormatToken.ConsumptionType==3){
                            Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else if (FormatToken.ConsumptionType==5){
                            Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }
                    }
                }else {
                    if (FormatToken.ConsumptionType==1){
                        //刷卡结账
                        CalculateData();    //没联网计算取缓存数据
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
                        myCountDownTimer.cancel();
                        finish();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void CalculateData() {
        String waterVolume= CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
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
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (InstructUtil.StatusInstruct(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,1);
                    CloseService();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initCom205Data() {

    }
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,1);
            finish();
        }
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
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            tv_time.setText(millisUntilFinished/1000+"");
        }
        @Override
        public void onFinish() {
            finish();
        }
    }
    private void initView() {
        tv_time=(TextView)findViewById(R.id.id_time) ;
        tv_balance=(TextView)findViewById(R.id.id_balance_amount);
        tv_customerNo=(TextView)findViewById(R.id.id_tv_customerNo);
        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.AppBalance/100.0);
        tv_balance.setText(String.valueOf(balance));
        tv_customerNo.setText(FormatToken.StringCardNo);
        myCountDownTimer.start();
    }
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
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
        CloseService();
        endTimeCount();

    }
}
