package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mcloyal.serialport.constant.Cmd;
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
import com.msht.watersystem.widget.LEDView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class DeliverOutWaterActivity extends BaseActivity implements Observer{
    private View layout_finish;
    private ImageView   textView;
    private TextView    tv_CardNo;
    private TextView    tv_volume;
    private TextView    tv_orderNo;
    private TextView    tv_time;
    private TextView    tv_finishOrder;
    private TextView    tv_title;
    private TextView    tv_tip;
    private Button      btn_tip;
    private LEDView     le_water;
    private boolean     bindStatus=false;
    private int         second=0;
    private double      volume=0.00;
    private int         EnsureState=0;
    private int         ReceiveState=0;
    private boolean     Currentstatus=false;
    private boolean     finishStatus=false;
    private boolean     tipStatus=true;
    private Context     mContext;
    private boolean     isStart=false;
    private PortService portService;
    private CountDownTimer Timer;
    private MyCountDownTimer myCountDownTimer;
    private ComServiceConnection serviceConnection;
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            second++;
            double Volume=second*volume;
            double mVolume=DataCalculateUtils.TwoDecinmal2(Volume);
            le_water.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
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
        initWaterQuality();
        OpenService();
    }
    private void initView() {
        layout_finish=findViewById(R.id.id_re_code);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_customerNo);
        tv_volume=(TextView)findViewById(R.id.id_getwater_volume);
        tv_orderNo=(TextView)findViewById(R.id.id_tv_orderNo);
        le_water=(LEDView)findViewById(R.id.id_waster_yield);
        tv_title=findViewById(R.id.id_get_text);
        tv_tip=findViewById(R.id.id_tip_text);
        btn_tip=findViewById(R.id.id_tip_button);
        tv_finishOrder=(TextView)findViewById(R.id.id_finish_order);
        tv_time=(TextView)findViewById(R.id.id_time);
        double weight= DataCalculateUtils.TwoDecinmal2(FormatToken.Waterweight/100.0);
        tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
        tv_volume.setText(String.valueOf(weight)+"升");
        tv_orderNo.setText(FormatToken.OrderNoString);
    }
    private void OpenService(){
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
            VariableUtil.skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                   // MyLogUtil.d("主板控制指令204：", CreateOrderType.getPacketString(packet1));
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    if (Timer!=null){
                        Timer.cancel();
                    }
                    initCom207Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                }
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
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (InstructUtil.StatusInstruct(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    settleAccount();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom204Data() {
        if (Currentstatus){
            volume= DataCalculateUtils.getWaterVolume(FormatToken.WaterNum,FormatToken.OutWaterTime);
            handler.post(runnable);
            Currentstatus=false;
        }else {
            if (ReceiveState==2){
                removeback();
            }else if (ReceiveState==0){
                Toast.makeText(mContext,"无任何操作",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
        if (InstructUtil.ControlInstruct(data)){
            int businessType=ByteUtils.byteToInt(data.get(15));
            if (businessType==3){
                removeback();
                int amountAfter=FormatToken.AfterAmount;
                int consumption=FormatToken.ConsumptionAmount;
                int waterWeight=FormatToken.WaterYield;
                if (consumption<=0){
                    tv_title.setText("取水未完成");
                    tv_finishOrder.setText("当前订单"+FormatToken.OrderNoString+"未取水，请再次扫码取水");
                }else {
                    tv_title.setText("取水完成");
                    tv_finishOrder.setText("当前订单"+FormatToken.OrderNoString+"取水结束");
                }
                settleServer(amountAfter,consumption,waterWeight);
            }else if (businessType==1){
                if (FormatToken.Balance<30){
                    Intent intent=new Intent(mContext,NotSufficientActivity.class);
                    startActivityForResult(intent,1);
                    finish();
                }else {
                    String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)){
                        Intent intent=new Intent(mContext,IcCardoutWater.class);
                        startActivityForResult(intent,1);
                        finish();
                    }else {
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
                        finish();
                    }
                }
            }
        }
    }
    private void CalculateData() {
        String waterVolume= CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void initCom207Data() {
        tipStatus=true;
        //返回按键有效
        finishStatus=true;
        tv_tip.setText("1、返回首页请点击其他按键");
        btn_tip.setText("按键");
        myCountDownTimer.start();
        layout_finish.setVisibility(View.VISIBLE);
    }
    private void settleAccount() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= InstructUtil.Settle();
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
    private void settleServer(int Afteramount,int amount,int waterWeight) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x07};
                if (VariableUtil.byteArray!=null&&VariableUtil.byteArray.size()!=0){
                    byte[] data= DataCalculateUtils.ArrayToByte(VariableUtil.byteArray);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(Afteramount);
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
                    portService.sendToCom2(packet);
                }else {
                    byte[] data= BusinessInstruct.settleData(FormatToken.phoneType,FormatToken.orderType);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(Afteramount);
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
                    portService.sendToCom2(packet);
                }
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
        if (!isStart){
            startCountDownTime(30);
        }
    }
    private void startCountDownTime(final long time) {
        Timer=new CountDownTimer(time*1000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tv_time.setText(millisUntilFinished/1000+"秒");
            }
            @Override
            public void onFinish() {
                if (handler!=null){
                    handler.removeCallbacks(runnable);
                }
                initCom207Data();
            }
        };
        Timer.start();
        isStart=true;
    }
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            tv_time.setText(millisUntilFinished/1000+"s");
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
            if (EnsureState==0){
                if (portService != null) {
                    Currentstatus=true;
                    EnsureState=1;
                    ReceiveState=1;
                    portService.sendToCom1(Cmd.ComCmd._START_);
                    myCountDownTimer.cancel();
                    layout_finish.setVisibility(View.GONE);
                }
            }else if (EnsureState==1){
                if (portService != null) {
                    EnsureState=0;
                    Currentstatus=false;
                    ReceiveState=2;
                    portService.sendToCom1(Cmd.ComCmd._END_);
                    TipDialog();
                    removeback();
                }
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

    private void TipDialog() {
        tipStatus=false;
        tv_title.setText("暂停取水");
        tv_finishOrder.setText("请您在30s内重启灌装");
        tv_tip.setText("计时超时将终止取水");
        btn_tip.setText("提示");
        layout_finish.setVisibility(View.VISIBLE);
        myCountDownTimer.start();
    }
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
        if (Timer!=null){
            Timer.cancel();
        }
    }
    private void removeback() {
        if (handler!=null){
            handler.removeCallbacks(runnable);
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
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        removeback();
        endTimeCount();
    }
}
