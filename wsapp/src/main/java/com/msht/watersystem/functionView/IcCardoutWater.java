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
import com.msht.watersystem.Manager.GreenDaoManager;
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
import com.msht.watersystem.entity.OrderInfo;
import com.msht.watersystem.gen.OrderInfoDao;
import com.msht.watersystem.widget.LEDView;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class IcCardoutWater extends BaseActivity implements Observer, Handler.Callback{
    private TextView tv_Balalance;
    private TextView tv_CardNo;
    private LEDView  le_water,le_amount;
    private MyImgScroll myPager;
    private List<View>  listViews;
    private ImageView   textView;
    private TextView    tv_time;
    private int         second=0;
    private boolean     bindStatus=false;
    private String      mAccount;
    private double      volume=0.00;
    private double      priceNum=0.00;
    private boolean     startOut=false;
    private String      afterAmount="0.0";
    private String      afterWater="0.0";
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private ArrayList<Byte> byteList=new ArrayList<Byte>();
    private final static String TAG = IcCardoutWater.class.getSimpleName();
    final Handler handlerStop=new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:
                    handler.removeCallbacks(runnable);
                    double waterVolume=FormatToken.WaterYield*volume;
                    afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                    le_water.setLedView(getString(R.string.default_bg_digital),afterWater);
                    second=0;
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
        setContentView(R.layout.activity_pay_afterpage);
        initViewImages();
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(180000,1000);
        initView();
        iniWaterQualiy();
        OpenService();
    }
    private void initView() {
        tv_time=(TextView)findViewById(R.id.id_time) ;
        tv_Balalance=(TextView)findViewById(R.id.id_amount);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_cardno);
        le_amount=(LEDView)findViewById(R.id.id_pay_amount);
        le_water=(LEDView)findViewById(R.id.id_waster_yield);
        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
        tv_Balalance.setText(String.valueOf(balance));
        tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
        mAccount=String.valueOf(FormatToken.StringCardNo);
        myCountDownTimer.start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OpenService();
    }
    private void OpenService() {
        serviceConnection = new ComServiceConnection(IcCardoutWater.this, new ComServiceConnection.ConnectionCallBack() {
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
                    MyLogUtil.d("主板回复指令204",CreateOrderType.getPacketString(packet1));
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("主板控制指令104：",CreateOrderType.getPacketString(packet1));
                    initCom104Data(packet1.getData(),packet1);
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板回复指令105",CreateOrderType.getPacketString(packet1));
                    initCom105Data(packet1.getData());
                }else {
                    MyLogUtil.d("主板其他指令类型",CreateOrderType.getPacketString(packet1));
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    MyLogUtil.d("服务端回复指令203：",CreateOrderType.getPacketString(packet2));
                    initCom203Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    MyLogUtil.d("服务端业务指令107：",CreateOrderType.getPacketString(packet2));
                    ResponseSever(packet2.getFrame());
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    MyLogUtil.d("服务端设备设置指令102：",CreateOrderType.getPacketString(packet2));
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    Toast.makeText(mContext, CreateOrderType.getPacketString(packet2),Toast.LENGTH_SHORT).show();
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer204(packet2.getFrame());      //回复
                    }
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("服务端回复指令205：",CreateOrderType.getPacketString(packet2));
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x06})){
                    MyLogUtil.d("服务端时间指令206：",CreateOrderType.getPacketString(packet2));
                }else {
                    MyLogUtil.d("服务其他指令类型",CreateOrderType.getPacketString(packet2));
                }
            }
        }
    }

    private void responseServer204(byte[] frame) {

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
    private void initCom105Data(ArrayList<Byte> data) {
        if (StatusInstructUtil.StatusInstruct(data)){
            tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
            tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
            String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
            if (!DataCalculateUtils.isEvent(stringWork,6)){
                if (!startOut){
                    //扫码打水过程，水量不足，自动结账
                    Intent intent=new Intent(mContext, CannotBuywater.class);
                    startActivityForResult(intent,1);
                    finish();
                }
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
        if (BusinessInstruct.CalaculateRecharge(data)){
            if (FormatToken.BusinessType==3){
                FormatToken.Balance=FormatToken.Balance+FormatToken.rechargeAmount;
                Intent intent=new Intent(IcCardoutWater.this,PaySuccess.class);
                intent.putExtra("afterAmount",afterAmount) ;
                intent.putExtra("afetrWater",afterWater);
                intent.putExtra("mAccount",mAccount);
                intent.putExtra("sign","2");
                startActivity(intent);
                finish();
            }
        }
    }
    private void initCom104Data(ArrayList<Byte> data, Packet packet1) {
        if (ControlInstructUtil.ControlInstruct(data)){
            if (ByteUtils.byteToInt(data.get(15))==1){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                if (DataCalculateUtils.isEvent(stringWork,3)){   //判断第一次刷卡？
                    myCountDownTimer.onFinish();
                    Message msg=new Message();
                    msg.what=1;
                    handlerStop.sendMessage(msg);
                   /* if (portService!=null){
                        if (!portService.isConnection()){
                            byte[] orderData= CreateOrderType.OrderByteData(packet1);
                            insertdata(orderData);
                        }
                    }*/
                    double consumption=FormatToken.ConsumptionAmount/100.0;
                    double waterVolume=FormatToken.WaterYield*volume;
                    afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                    afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                    Intent intent=new Intent(IcCardoutWater.this,PaySuccess.class);
                    intent.putExtra("afterAmount",afterAmount) ;
                    intent.putExtra("afetrWater",afterWater);
                    intent.putExtra("mAccount",mAccount);
                    intent.putExtra("sign","0");
                    startActivity(intent);
                    finish();
                }else {
                    double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                    tv_Balalance.setText(String.valueOf(balance));
                    tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
                    mAccount=String.valueOf(FormatToken.StringCardNo);
                }
            }
        }
    }
    private void initCom203Data(ArrayList<Byte> data) {
        try {
            if(ControlInstructUtil.EquipmentData(data)){
                    String waterVolume=String.valueOf(FormatToken.WaterNum);
                    String Time=String.valueOf(FormatToken.OutWaterTime);
                    CachePreferencesUtil.putStringData(this,CachePreferencesUtil.Volume,waterVolume);
                    CachePreferencesUtil.putStringData(this,CachePreferencesUtil.outWaterTime,Time);
                    CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
                    volume=DataCalculateUtils.getWaterVolume(FormatToken.WaterNum,FormatToken.OutWaterTime);
                    priceNum=DataCalculateUtils.getWaterPrice(FormatToken.PriceNum);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom204Data() {
        if (startOut){
            if (portService != null) {
                CalculateData();    //没联网计算取缓存数据
                try {
                    byte[] frame = FrameUtils.getFrame(mContext);
                    byte[] type = new byte[]{0x01, 0x03};
                    byte[] packet = PacketUtils.makePackage(frame, type, null);
                    portService.sendToCom2(packet);
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
    private void insertdata(byte[] bytes) {
        //插入数据
        OrderInfo insertData = new OrderInfo(null, bytes);
        getOrderDao().insert(insertData);
    }
    private OrderInfoDao getOrderDao() {
        return GreenDaoManager.getInstance().getSession().getOrderInfoDao();
    }
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
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
//            e.printStackTrace();
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
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (portService!= null) {
                myCountDownTimer.cancel();
                startOut=true;
                portService.sendToCom1(Cmd.ComCmd._START_);
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
        }else if (keyCode==KeyEvent.KEYCODE_F1){
        }
        return super.onKeyDown(keyCode, event);
    }
    private void endTimeCount() {
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        endTimeCount();
    }
}
