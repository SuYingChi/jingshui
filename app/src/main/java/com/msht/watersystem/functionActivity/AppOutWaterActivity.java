package com.msht.watersystem.functionActivity;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.msht.watersystem.Utils.FormatCommandUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.LEDView;
import com.msht.watersystem.widget.MyImgScroll;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AppOutWaterActivity extends BaseActivity implements Observer{
    private int      second=0;
    private boolean  bindStatus=false;
    private boolean  isStart=false;
    private double   volume=0.00;
    private double   priceNum=0.00;
    private String   mAccount;
    private String   afterAmount="0.0";
    private String   afterWater="0";
    private View     layout_notice;
    private TextView tv_Balalance;
    private TextView tv_CardNo;
    private TextView tv_time;
    private LEDView  le_water,le_amount;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private CountDownTimer Timer;// 倒计时对象
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private int EnsureState=0;
    private int ReceiveState=0;
    private boolean Currentstatus=false;
    private final UpdataHandler handlerStop = new UpdataHandler(this);
    private static class  UpdataHandler extends Handler{
        private WeakReference<AppOutWaterActivity> mWeakReference;
        public UpdataHandler(AppOutWaterActivity appoutWater) {
            mWeakReference = new WeakReference<AppOutWaterActivity>(appoutWater);
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
                    double waterVolume=FormatToken.WaterYield*activity.volume;
                    String Water=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                    activity.le_water.setLedView(activity.getString(R.string.default_bg_digital),String.valueOf(Water));
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
            double Volume=second*volume;
            double amount=second*priceNum;
            double mVolume=DataCalculateUtils.TwoDecinmal2(Volume);
            double mAmount=DataCalculateUtils.TwoDecinmal2(amount);
            le_water.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            le_amount.setLedView(getString(R.string.default_bg_digital),String.valueOf(mAmount));
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
        initWaterQuality();
        bindPortService();
    }
    private void initView() {
        layout_notice=findViewById(R.id.id_layout_dialog);
        tv_time=(TextView)findViewById(R.id.id_time) ;
        tv_Balalance=(TextView)findViewById(R.id.id_amount);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_cardno);
        le_amount=(LEDView)findViewById(R.id.id_pay_amount);
        le_water=(LEDView)findViewById(R.id.id_waster_yield);
        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.AppBalance/100.0);
        tv_Balalance.setText(String.valueOf(balance)+"元");
        mAccount=String.valueOf(FormatToken.StringCardNo);
        tv_CardNo.setText(mAccount);
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
            VariableUtil.skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    onCom1Received204DataFromControllBoard();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    onCom1Received104DataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControllBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    if (Timer!=null){
                        Timer.cancel();    //接收到207计时停止
                    }
                    onCom2Received207DataFromServer();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    onCom2Received104Data(packet2.getData());
                }
            }
        }
    }
    private void onCom2Received104Data(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,1);
            finish();
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
        if (BusinessInstruct.ControlModel(mContext,data)){
            if (FormatToken.ShowTDS==0){
                layout_TDS.setVisibility(View.GONE);
            }else {
                layout_TDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void onCom1Received105DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if (FormatCommandUtil.convertStatusCommandToFormatToken(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    if (ReceiveState!=0){
                        //扫码打水过程，水量不足，自动结账
                        Currentstatus=false;
                        EnsureState=2;
                        ReceiveState=3;
                        settleAccount();
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
    private void onCom1Received104DataFromControllBoard(ArrayList<Byte> data) {
            if(FormatCommandUtil.convertCom1ReceivedDataToFormatToken(data)){
                int businessType=ByteUtils.byteToInt(data.get(15));
                //扫码结账
                if (businessType==3){
                    if (EnsureState==2){
                        Message msg=new Message();
                        msg.what=2;
                        handlerStop.sendMessage(msg);
                        int amountAfter=FormatToken.AfterAmount;
                        int consumption=FormatToken.ConsumptionAmount;
                        int Charge= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ChargeMode,0);
                        if (Charge!=1){
                            FormatToken.AppBalance=FormatToken.AppBalance-consumption;
                        }
                        settleServer(amountAfter,consumption);
                    }else {
                        Message msg=new Message();
                        msg.what=2;
                        handlerStop.sendMessage(msg);
                        int amountAfter=FormatToken.AfterAmount;
                        int consumption=FormatToken.ConsumptionAmount;
                        int Charge= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ChargeMode,0);
                        if (Charge!=1){
                            FormatToken.AppBalance=FormatToken.AppBalance-consumption;
                        }
                        settleServer(amountAfter,consumption);
                    }
                }else if (businessType==1){
                    if (FormatToken.Balance<=1){
                        Intent intent=new Intent(mContext,NotSufficientActivity.class);
                        startActivityForResult(intent,1);
                        myCountDownTimer.cancel();
                        finish();
                    }else {
                        String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                        if (!DataCalculateUtils.isEvent(stringWork,3)){
                            Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else {
                            //刷卡结账
                            CalculateData();    //没联网计算取缓存数据
                            double consumption=FormatToken.ConsumptionAmount/100.0;
                            double waterVolume=consumption/0.3;
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
    private void onCom2Received207DataFromServer() {
        double consumption=FormatToken.ConsumptionAmount/100.0;
        double waterVolume=FormatToken.WaterYield*volume;
        afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
        if (waterVolume==0){
            afterAmount="0.0";
        }
        afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
        Intent intent=new Intent(mContext,PaySuccessActivity.class);
        intent.putExtra("afterAmount",afterAmount);
        intent.putExtra("afetrWater",afterWater);
        intent.putExtra("mAccount",mAccount);
        intent.putExtra("sign","1");
        startActivity(intent);
        finish();
    }
    private void onCom1Received204DataFromControllBoard() {
        if (Currentstatus){
            volume=DataCalculateUtils.getWaterVolume(FormatToken.WaterNum,FormatToken.OutWaterTime);
            priceNum=DataCalculateUtils.getWaterPrice(FormatToken.PriceNum);
            handler.post(runnable);
            Currentstatus=false;
        }else {
            if (ReceiveState==2){
                Message msg=new Message();
                msg.what=1;
                handlerStop.sendMessage(msg);
            }else if (ReceiveState==3){
                EnsureState=0;
                ReceiveState=0;
                Message msg=new Message();
                msg.what=2;
                handlerStop.sendMessage(msg);
                if (myCountDownTimer != null) {
                    myCountDownTimer.cancel();
                }
                layout_notice.setVisibility(View.GONE);
            }
        }
    }
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.parseInt(waterVolume);
        int mTime=Integer.parseInt(Time);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void settleServer(int Afteramount,int amount) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x07};
                if (VariableUtil.byteArray!=null&&VariableUtil.byteArray.size()!=0){
                    byte[] data= DataCalculateUtils.ArrayToByte(VariableUtil.byteArray);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(Afteramount);
                    byte[] water=ByteUtils.intToByte2(FormatToken.WaterYield);
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
                    byte[] data= BusinessInstruct.settleData(FormatToken.phoneType,FormatToken.orderType);
                    byte[] consumption= ByteUtils.intToByte4(amount);
                    byte[] afterConsumption=ByteUtils.intToByte4(Afteramount);
                    byte[] water=ByteUtils.intToByte2(FormatToken.WaterYield);
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
        if (!isStart){
            startCountDownTime(30);
        }
    }
    private void settleAccount() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= FormatCommandUtil.settle();
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
            layout_notice.setVisibility(View.GONE);
            if (ReceiveState==2){
                EnsureState=2;
                ReceiveState=3;
                //settleAccount();
            }
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
                onCom2Received207DataFromServer();
            }
        };
        Timer.start();
        isStart=true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (EnsureState==0){
                if (portService != null) {
                    Currentstatus=true;
                    EnsureState=1;
                    ReceiveState=1;
                    portService.sendToControlBoard(Cmd.ComCmd._START_);
                    myCountDownTimer.onFinish();//停止
                }
            }else if (EnsureState==1){
                if (portService != null) {
                    Currentstatus=false;
                    EnsureState=0;
                    ReceiveState=2;
                    portService.sendToControlBoard(Cmd.ComCmd._END_);
                    layout_notice.setVisibility(View.VISIBLE);
                    myCountDownTimer.start();
                }
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            Currentstatus=false;
            EnsureState=2;
            ReceiveState=3;
            myCountDownTimer.onFinish();
            settleAccount();
            startCountDownTime(30);
        }
        return super.onKeyDown(keyCode, event);
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
        if (Timer!=null){
            Timer.cancel();
        }
    }
    private void removeback() {
        if (handler!=null){
            handler.removeCallbacks(runnable);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        removeback();
        endTimeCount();
    }
}
