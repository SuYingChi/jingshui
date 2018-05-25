package com.msht.watersystem.functionView;

import android.content.Context;
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

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.ControlInstructUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.MyLogUtil;
import com.msht.watersystem.Utils.StatusInstructUtil;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class PaySuccess extends BaseActivity implements Observer, Handler.Callback {

    private boolean     bindStatus=false;
    private Context     mContext;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private MyImgScroll myPager;
    private List<View>  listViews;
    private ImageView   textView;
    private TextView    tv_time;
    private TextView    tv_customerNo;
    private TextView    tv_water;
    private TextView    tv_amount;
    private TextView    tv_balance;
    private TextView    tv_success;
    private String      mAccount;
    private String      sign;
    private String afterAmount,afetrWater;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_success);
        mContext=this;
        afetrWater=getIntent().getStringExtra("afetrWater");
        afterAmount=getIntent().getStringExtra("afterAmount");
        mAccount=getIntent().getStringExtra("mAccount");
        sign=getIntent().getStringExtra("sign");
        initViewImages();
        initView();
        iniWaterQualiy();
        OpenService();
        myCountDownTimer=new MyCountDownTimer(60000,1000);
        myCountDownTimer.start();
    }
    private void OpenService() {
        serviceConnection = new ComServiceConnection(PaySuccess.this, new ComServiceConnection.ConnectionCallBack() {
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
    private void initView() {
        tv_success=(TextView)findViewById(R.id.id_success) ;
        tv_balance=(TextView)findViewById(R.id.id_amount) ;
        tv_amount=(TextView)findViewById(R.id.id_consumption) ;
        tv_water =(TextView)findViewById(R.id.id_water_num);
        tv_customerNo=(TextView)findViewById(R.id.id_tv_customerNo);
        tv_time=(TextView)findViewById(R.id.id_time);
        tv_customerNo.setText(mAccount);
        if (sign.equals("0")){
            tv_success.setText("付款成功");
            tv_amount.setVisibility(View.VISIBLE);
            tv_amount.setText("成功消费了"+afterAmount+"元");
            tv_water.setText("共购买了"+afetrWater+"升的水");
            double afterconsumpte=FormatToken.AfterAmount/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }else if (sign.equals("1")){
            tv_success.setText("付款成功");
            tv_amount.setVisibility(View.VISIBLE);
            tv_amount.setText("成功消费了"+afterAmount+"元");
            tv_water.setText("共购买了"+afetrWater+"升的水");
            double afterconsumpte=FormatToken.AppBalance/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }else if (sign.equals("2")){
            tv_amount.setVisibility(View.INVISIBLE);
            tv_success.setText("充值成功");
            double rechargeAmount=FormatToken.rechargeAmount/100.0;
            tv_water.setText("成功充值了"+String.valueOf(DataCalculateUtils.TwoDecinmal2(rechargeAmount))+"元");
            double afterconsumpte=FormatToken.Balance/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }
    }
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void update(Observable observable, Object arg) {

        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSkeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("主板回复指令104",CreateOrderType.getPacketString(packet1));
                    intAnalist(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板状态指令105：",CreateOrderType.getPacketString(packet1));
                    initCom105Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    MyLogUtil.d("主板回复指令204",CreateOrderType.getPacketString(packet1));
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("主板回复指令205",CreateOrderType.getPacketString(packet1));
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("服务端控制指令205：", CreateOrderType.getPacketString(packet2));
                    initCom205Data();
                }else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：", CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer204(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    MyLogUtil.d("服务端控制指令107：", CreateOrderType.getPacketString(packet2));
                    responseServer(packet2.getFrame());
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    MyLogUtil.d("服务端控制指令102：", CreateOrderType.getPacketString(packet2));
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }

            }
        }

    }
    private void responseServer204(byte[] frame) {
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
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateRecharge(data)){
            if (FormatToken.BusinessType==3){
                if (sign.equals("0")){
                    FormatToken.AfterAmount=FormatToken.AfterAmount+FormatToken.rechargeAmount;
                    double afterconsumpte=FormatToken.AfterAmount/100.0;
                    tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
                }
            }
        }
    }
    private void responseServer(byte[] frame) {
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
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,2);
        }
    }
    private void initCom205Data() {}
    private void initCom105Data(ArrayList<Byte> data) {
        if (StatusInstructUtil.StatusInstruct(data)){
            tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
            tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
            String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
            if (!DataCalculateUtils.isEvent(stringWork,6)){
                Intent intent=new Intent(mContext, CannotBuywater.class);
                startActivityForResult(intent,1);
                finish();
            }
        }
    }
    private void intAnalist(ArrayList<Byte> data) {
        try {
            if(ControlInstructUtil.ControlInstruct(data)){
                    String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                    if (DataCalculateUtils.isEvent(stringWork,3)){
                        if (FormatToken.ConsumptionType==1){
                            double afterconsumpte=FormatToken.AfterAmount/100.0;
                            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
                        }
                    }else {
                        if (FormatToken.Balance<=20){
                            Intent intent=new Intent(mContext,NotSufficient.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else {
                            if (FormatToken.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardoutWater.class);
                                startActivityForResult(intent,1);
                                finish();
                            }else if (FormatToken.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppoutWater.class);
                                startActivityForResult(intent,1);
                                finish();
                            }else if (FormatToken.ConsumptionType==5){
                                Intent intent=new Intent(mContext,OutWater.class);
                                startActivityForResult(intent,1);
                                finish();
                            }
                        }
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
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
            tv_time.setText("120");
            finish();
        }
    }
    private void endTime() {
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
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
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/watersystem/images/");
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
           // return false;
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
      //  return super.onKeyDown(keyCode, event);
    }
    private void showTips() {
        finish();
    }
    private void CloseService(){
        if (serviceConnection != null && portService != null) {
            if (bindStatus){
                portService.removeObserver(this);
                unbindService(serviceConnection);
                bindStatus=false;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        endTime();
    }
}
