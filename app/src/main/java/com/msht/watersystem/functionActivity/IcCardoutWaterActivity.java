package com.msht.watersystem.functionActivity;

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
import com.msht.watersystem.Interface.ResultListener;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.FormatCommandUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.LEDView;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class IcCardoutWaterActivity extends BaseActivity implements Observer{
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
    private int mTime=30;
    private int overTime=40;
    private boolean     startOut=false;
    private String      afterAmount="0.0";
    private String      afterWater="0.0";
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;
    private static final int SUCCESS=1;
    private static final int FAILURE=0;
    private final static String TAG = IcCardoutWaterActivity.class.getSimpleName();
    Handler finishHandler = new FinishHandler(this);

    private static class FinishHandler extends Handler{

        private final WeakReference<IcCardoutWaterActivity> icCardoutWaterActivityWeakReference;

        FinishHandler(IcCardoutWaterActivity icCardoutWaterActivity){
            icCardoutWaterActivityWeakReference = new WeakReference<IcCardoutWaterActivity>(icCardoutWaterActivity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    icCardoutWaterActivityWeakReference.get().finishOutwaterActivity("0");
                    break;
                case FAILURE:
                    icCardoutWaterActivityWeakReference.get().finishOutwaterActivity("0");
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
            double Volume=second*volume;
            double amount=second*priceNum;
            double mVolume=DataCalculateUtils.TwoDecinmal2(Volume);
            double mAmount=DataCalculateUtils.TwoDecinmal2(amount);
            le_water.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            le_amount.setLedView(getString(R.string.default_bg_digital),String.valueOf(mAmount));
            if (second>=overTime){
                afterAmount=String.valueOf(mAmount);
                afterWater=String.valueOf(mVolume);
                finishOutwaterActivity("3");
            }
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iccard_outwater);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(180000,1000);
        initView();
        initWaterQuality();
        bindPortService();
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
        bindPortService();
    }
    private void bindPortService() {
        serviceConnection = new ComServiceConnection(IcCardoutWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
                    onCom1Received104DataFromControllBoard(packet1.getData(),packet1);
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
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                }
            }
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
        if (BusinessInstruct.ControlModel(mContext,data)){
            if (FormatToken.ShowTDS==0){
                layout_TDS.setVisibility(View.GONE);
            }else {
                layout_TDS.setVisibility(View.VISIBLE);
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
        if (FormatCommandUtil.convertStatusCommandToFormatToken(data)){
            tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
            tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
            String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
            if (!DataCalculateUtils.isEvent(stringWork,6)){
                if (!startOut){
                    //扫码打水过程，水量不足，自动结账
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,1);
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
        if (BusinessInstruct.CalaculateRecharge(data)){
            if (FormatToken.BusinessType==3){
                FormatToken.Balance=FormatToken.Balance+FormatToken.rechargeAmount;
                Intent intent=new Intent(IcCardoutWaterActivity.this,PaySuccessActivity.class);
                intent.putExtra("afterAmount",afterAmount) ;
                intent.putExtra("afetrWater",afterWater);
                intent.putExtra("mAccount",mAccount);
                intent.putExtra("sign","2");
                startActivity(intent);
                finish();
            }
        }
    }
    //控制板发送104指令过来COM1，里边包含着消费了多少元和出水量等一系列数据信息
    private void onCom1Received104DataFromControllBoard(ArrayList<Byte> data, Packet packet1) {
        if (FormatCommandUtil.convertCom1ReceivedDataToFormatToken(data)){
            if (ByteUtils.byteToInt(data.get(15))==1){
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                if (DataCalculateUtils.isEvent(stringWork,3)){
                    myCountDownTimer.onFinish();
                    handler.removeCallbacks(runnable);
                    double consumption=FormatToken.ConsumptionAmount/100.0;
                    double waterVolume=consumption/0.3;
                    afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                    afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                    le_water.setLedView(getString(R.string.default_bg_digital),afterWater);
                    second=0;    //重置
                    /*if (portService!=null){
                        if (!portService.isConnection()){
                            SavaData(packet1);
                        }else {
                            finishOutwaterActivity();
                        }
                    }else {
                        SavaData(packet1);
                    }*/
                    finishOutwaterActivity("0");
                }else {
                    double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                    tv_Balalance.setText(String.valueOf(balance));
                    tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
                    mAccount=String.valueOf(FormatToken.StringCardNo);
                }
            }
        }
    }
    private void finishOutwaterActivity(String sign) {
        Intent intent=new Intent(IcCardoutWaterActivity.this,PaySuccessActivity.class);
        //消费了多少元
        intent.putExtra("afterAmount",afterAmount) ;
        //取了多少水
        intent.putExtra("afetrWater",afterWater);
        intent.putExtra("mAccount",mAccount);
        intent.putExtra("sign",sign);
        startActivity(intent);
        finish();
    }
    private void SavaData(Packet packet1) {   //数据库操作保存数据
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
        setEquipmentData(data.get(4));
        try {
            if(FormatCommandUtil.equipmentData(data)){
                mTime=FormatToken.OutWaterTime;
                overTime=mTime+10;
                String waterVolume=String.valueOf(FormatToken.WaterNum);
                String Time=String.valueOf(FormatToken.OutWaterTime);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.Volume,waterVolume);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.outWaterTime,Time);
                VariableUtil.setEquipmentStatus=false;
                volume=DataCalculateUtils.getWaterVolume(FormatToken.WaterNum,FormatToken.OutWaterTime);
                priceNum=DataCalculateUtils.getWaterPrice(FormatToken.PriceNum);
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
                byte[] data= FormatCommandUtil.setEquipmentParameter(aByte);
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
    private void onCom1Received204DataFromControllBoard() {
        if (startOut){
            if (portService != null) {
                CalculateData();    //没联网计算取缓存数据
                try {
                    byte[] frame = FrameUtils.getFrame(mContext);
                    byte[] type = new byte[]{0x01, 0x03};
                    byte[] packet = PacketUtils.makePackage(frame, type, null);
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
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
        overTime=mTime+10;
    }
    private void initViewImages() {
        myPager = (MyImgScroll) findViewById(R.id.myvp);
        textView = (ImageView) findViewById(R.id.textView);
        InitViewPagers();
        if (!listViews.isEmpty()&&listViews.size()>0) {
            myPager.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            myPager.start(this, listViews, 10000);
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
                imageView.setImageBitmap(BitmapUtil.decodeSampledBitmapFromFile(fileImagelist.get(i), 800, 800));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                listViews.add(imageView);
            }
        }else if(!scanner5Directory.exists()){
            scanner5Directory.mkdirs();
        }
      /*  File file = new File(Environment.getExternalStorageDirectory().getPath() + "/watersystem/images/");
        if (!file.exists()){
            file.mkdirs();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file.getPath() + "/");
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
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
        public void onFinish() { }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (portService!= null) {
                myCountDownTimer.cancel();
                startOut=true;
                portService.sendToControlBoard(Cmd.ComCmd._START_);
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
