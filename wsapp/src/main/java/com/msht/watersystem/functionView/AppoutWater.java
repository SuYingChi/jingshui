package com.msht.watersystem.functionView;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.ControlInstructUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.MyLogUtil;
import com.msht.watersystem.Utils.StatusInstructUtil;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.LEDView;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AppoutWater extends BaseActivity implements Observer, Handler.Callback{
    private int      second=0;
    private boolean  bindStatus=false;
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
    private MyImgScroll myPager;
    private List<View>  listViews;
    private ImageView   textView;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private int EnsureState=0;
    private int ReceiveState=0;
    private boolean Currentstatus=false;
    final Handler handlerStop=new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:
                    handler.removeCallbacks(runnable);
                    break;
                case 2:
                    handler.removeCallbacks(runnable);
                    double waterVolume=FormatToken.WaterYield*volume;
                    String Water=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                    le_water.setLedView(getString(R.string.default_bg_digital),String.valueOf(Water));
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
        initViewImages();
        myCountDownTimer=new MyCountDownTimer(90000,1000);
        initView();
        iniWaterQualiy();
        OpenService();
    }
    private void initView() {
        layout_notice=findViewById(R.id.id_layout_dialog);
        tv_time=(TextView)findViewById(R.id.id_time) ;
        tv_Balalance=(TextView)findViewById(R.id.id_amount);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_cardno);
        le_amount=(LEDView)findViewById(R.id.id_pay_amount);
        le_water=(LEDView)findViewById(R.id.id_waster_yield);
        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
        tv_Balalance.setText(String.valueOf(balance)+"元");
        mAccount=String.valueOf(FormatToken.StringCardNo);
        tv_CardNo.setText(mAccount);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OpenService();
    }
    private void OpenService() {
        serviceConnection = new ComServiceConnection(AppoutWater.this, new ComServiceConnection.ConnectionCallBack() {
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
    public boolean handleMessage(Message msg) {
        return false;
    }
    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            VariableUtil.skeyEnable = myObservable.isSkeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    MyLogUtil.d("主板控制指令204：",CreateOrderType.getPacketString(packet1));
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    Toast.makeText(mContext, CreateOrderType.getPacketString(packet1),Toast.LENGTH_SHORT).show();
                    MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板控制指令105：",CreateOrderType.getPacketString(packet1));
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    MyLogUtil.d("服务端控制指令207：",CreateOrderType.getPacketString(packet2));
                    initCom207Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    MyLogUtil.d("服务端控制指令102：",CreateOrderType.getPacketString(packet2));
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                }
            }
        }
    }
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,1);
            finish();
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
            if (StatusInstructUtil.StatusInstruct(data)){
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
                        Intent intent=new Intent(mContext, CannotBuywater.class);
                        startActivityForResult(intent,1);
                        finish();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
            if(ControlInstructUtil.ControlInstruct(data)){
                int businessType=ByteUtils.byteToInt(data.get(15));
                if (businessType==3){   //扫码结账
                    if (EnsureState==2){
                        Message msg=new Message();
                        msg.what=2;
                        handlerStop.sendMessage(msg);
                        int amountAfter=FormatToken.AfterAmount;
                        int consumption=FormatToken.ConsumptionAmount;
                        double waterVolume=FormatToken.WaterYield*volume;
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
                        double waterVolume=FormatToken.WaterYield*volume;
                        int Charge= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ChargeMode,0);
                        if (Charge!=1){
                            FormatToken.AppBalance=FormatToken.AppBalance-consumption;
                        }
                        settleServer(amountAfter,consumption);
                    }
                }else if (businessType==1){
                    if (FormatToken.Balance<=1){
                        Intent intent=new Intent(mContext,NotSufficient.class);
                        startActivityForResult(intent,1);
                        myCountDownTimer.cancel();
                        showTips();
                    }else {
                        String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                        if (!DataCalculateUtils.isEvent(stringWork,3)){
                            Intent intent=new Intent(mContext,IcCardoutWater.class);
                            startActivityForResult(intent,1);
                            myCountDownTimer.cancel();
                            showTips();
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
                            myCountDownTimer.cancel();
                            showTips();
                        }
                    }
                }
            }
    }
    private void initCom207Data() {
        double consumption=FormatToken.ConsumptionAmount/100.0;
        double waterVolume=FormatToken.WaterYield*volume;
        afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
        if (waterVolume==0){
            afterAmount="0.0";
        }
        afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
        Intent intent=new Intent(mContext,PaySuccess.class);
        intent.putExtra("afterAmount",afterAmount);
        intent.putExtra("afetrWater",afterWater);
        intent.putExtra("mAccount",mAccount);
        intent.putExtra("sign","1");
        startActivity(intent);
        finish();
    }
    private void initCom204Data() {
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
            }
        }
    }
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
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
                    portService.sendToCom2(packet);
                }else {
                    byte[] data= BusinessInstruct.settleData(FormatToken.phoneType);
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
    }
    private void settleAccount() {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data= ControlInstructUtil.SETTLE();
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
                settleAccount();
            }
        }
    }
    private void initViewImages() {
        myPager = (MyImgScroll) findViewById(R.id.myvp);
        textView = (ImageView) findViewById(R.id.textView);
        InitViewPagers();
        if (!listViews.isEmpty()&&listViews.size()>0) {
            myPager.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            myPager.start(this, listViews, 3000);
        }else{
            myPager.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
    }
    private void InitViewPagers() {
        listViews = new ArrayList<View>();
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
                imageView.setImageBitmap(BitmapUtil.decodeSampledBitmapFromFile(fileImagelist.get(i), 500, 500));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                listViews.add(imageView);
            }
        }
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
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
                    portService.sendToCom1(Cmd.ComCmd._START_);
                    myCountDownTimer.onFinish();//停止
                }
            }else if (EnsureState==1){
                if (portService != null) {
                    Currentstatus=false;
                    EnsureState=0;
                    ReceiveState=2;
                    portService.sendToCom1(Cmd.ComCmd._END_);
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
            settleAccount();
        }
        return super.onKeyDown(keyCode, event);
    }
    private void showTips() {
        finish();
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
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        endTimeCount();
    }
}
