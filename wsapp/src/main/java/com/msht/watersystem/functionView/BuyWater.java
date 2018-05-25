package com.msht.watersystem.functionView;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import com.msht.watersystem.Utils.CodeUtils;
import com.msht.watersystem.Utils.ControlInstructUtil;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.MyLogUtil;
import com.msht.watersystem.Utils.StatusInstructUtil;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.MyImgScroll;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class BuyWater extends BaseActivity implements Observer, Handler.Callback{
    private TextView    tv_time;
    private boolean     buyStatus=false;
    private boolean     bindStatus=false;
    private View        layout_online;
    private ImageView   ImageCode;
    private MyImgScroll myPager;
    private List<View>  listViews;
    private ImageView   imageView;
    private TextView    tv_freecharge;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private double volume=0.00;
    private final static String TAG = BuyWater.class.getSimpleName();
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            if(portService!=null){
                if (portService.isConnection()){
                    ImageCode.setVisibility(View.VISIBLE);
                    layout_online.setVisibility(View.GONE);
                }else {
                    ImageCode.setVisibility(View.GONE);
                    layout_online.setVisibility(View.VISIBLE);
                }
            }
            handler.postDelayed(this,1000);
        }
    };
    final Handler handlerStop=new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:
                    handler.removeCallbacks(runnable);
                    break;
            }
            super.handleMessage(msg);

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_water);
        mContext=this;
        VariableUtil.mPos=0;
        initViewImages();
        myCountDownTimer=new MyCountDownTimer(120000,1000);
        initView();
        iniWaterQualiy();
        OpenService();
        initNetWork();
        initData();
        boolean isFirstOpen = CachePreferencesUtil.getBoolean(mContext, CachePreferencesUtil.FIRST_OPEN, true);
        if (isFirstOpen){
            initsetData();
        }
    }
    private void initsetData() {
        if (portService != null) {
            if (portService.isConnection()){
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
            }
        }
    }
    private void initNetWork() {
        handler.post(runnable);
    }
    private void OpenService(){
        serviceConnection = new ComServiceConnection(BuyWater.this, new ComServiceConnection.ConnectionCallBack() {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OpenService();
        ShowUi();
        myCountDownTimer.start();
        if (requestCode==2){
            initNetWork();
        }
    }
    private void initView() {
        TextView tv_version=(TextView)findViewById(R.id.id_tv_version);
        tv_freecharge=(TextView)findViewById(R.id.id_free_charge);
        tv_time=(TextView)findViewById(R.id.id_time) ;
        ImageCode=(ImageView)findViewById(R.id.id_erwei_code) ;
        layout_online=findViewById(R.id.id_online_view);
        ((TextView)findViewById(R.id.id_equipment)).setText(String.valueOf(FormatToken.DeviceId));
        int ChargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ChargeMode,0);
        if (ChargeMode==1){
            tv_freecharge.setVisibility(View.VISIBLE);
        }else {
            tv_freecharge.setVisibility(View.GONE);
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            String Name=pi.versionName;
            if (Name!=null){
                tv_version.setText("version:"+Name);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initData() {
        myCountDownTimer.start();
        String downloadapp="http://msbapp.cn/download/success.html?QRCodeJson=";
        String result=downloadapp+SellTypeToJson();
        if (VariableUtil.QrCodebitmap!=null&&!VariableUtil.QrCodebitmap.isRecycled()){
            ImageCode.setImageBitmap(VariableUtil.QrCodebitmap);
        }else {
            createQRCodeWithLogo(result);
        }
    }
    private void createQRCodeWithLogo(final String result) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return CodeUtils.createImage(result, 500, 500, BitmapFactory.decodeResource(getResources(), R.mipmap.icon_xh));
            }
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null&&(!bitmap.isRecycled())) {
                    VariableUtil.QrCodebitmap=bitmap;
                    if (VariableUtil.QrCodebitmap!=null&&(!VariableUtil.QrCodebitmap.isRecycled())){
                        ImageCode.setImageBitmap(VariableUtil.QrCodebitmap);
                    }
                } else {
                    Toast.makeText(mContext, "系统故障", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
    private String SellTypeToJson() {
        String jsonresult="";
        String QrcodeType="1";
        String equipmentNo=String.valueOf(FormatToken.DeviceId);
        JSONObject object=new JSONObject();
        try{
            JSONObject jsondata=new JSONObject();
            object.put("QrcodeType",QrcodeType);
            jsondata.put("equipmentNo",equipmentNo);
            object.put("data",jsondata);
            jsonresult=object.toString();
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jsonresult;
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
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("主板控制指令104：", CreateOrderType.getPacketString(packet1));
                    intAnalist(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    MyLogUtil.d("主板回复指令204",CreateOrderType.getPacketString(packet1));
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板状态指令105：",CreateOrderType.getPacketString(packet1));
                    initCom105Data(packet1.getData());
                }else {
                    MyLogUtil.d("主板其他指令类型",CreateOrderType.getPacketString(packet1));
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    MyLogUtil.d("服务端业务指令107：",CreateOrderType.getPacketString(packet2));
                    ResponseSever(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    MyLogUtil.d("服务端设备设置指令102：",CreateOrderType.getPacketString(packet2));
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    MyLogUtil.d("服务端回复指令203：",CreateOrderType.getPacketString(packet2));
                    initCom203Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("服务端回复指令205：",CreateOrderType.getPacketString(packet2));
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
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ChargeMode,FormatToken.ChargeMode);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.ShowTds,FormatToken.ShowTDS);
                CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom102Data2(ArrayList<Byte> data) {
        if (BusinessInstruct.ControlModel(mContext,data)){
            ShowUi();
        }
    }
    private void ShowUi() {
        int tds= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ShowTds,0);
        int ChargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.ChargeMode,0);
        if (ChargeMode==1){
            tv_freecharge.setVisibility(View.VISIBLE);
        }else {
            tv_freecharge.setVisibility(View.GONE);
        }
        if (tds==0){
            layout_TDS.setVisibility(View.GONE);
        }else {
            layout_TDS.setVisibility(View.VISIBLE);
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
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,2);
            CloseService();
            handler.removeCallbacks(runnable);
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
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (StatusInstructUtil.StatusInstruct(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuywater.class);
                    startActivityForResult(intent,2);
                    CloseService();
                    handler.removeCallbacks(runnable);
                    myCountDownTimer.cancel();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom204Data() {
        if (buyStatus){
            buyStatus=false;
            if (FormatToken.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWater.class);
                startActivityForResult(intent,1);
                CloseService();
                myCountDownTimer.cancel();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppoutWater.class);
                startActivityForResult(intent,1);
                CloseService();
                myCountDownTimer.cancel();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,OutWater.class);
                startActivityForResult(intent,1);
                CloseService();
                myCountDownTimer.cancel();
            }
        }
    }
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);//数据更变
            buyStatus=true;
            if (FormatToken.Balance<20){
                Intent intent=new Intent(mContext,AppNotSufficient.class);
                startActivityForResult(intent,1);
                CloseService();
                myCountDownTimer.cancel();
            }else {
                if (FormatToken.BusinessType==1){
                    setBusiness(1);
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
                    byte[] data= ControlInstructUtil.setBusinessType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
                }else if (business==2){
                    byte[] data= ControlInstructUtil.setBusinessType02();
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
    private void intAnalist(ArrayList<Byte> data) {
        try {
                if(ControlInstructUtil.ControlInstruct(data)){
                    if (FormatToken.Balance<=1){
                        Intent intent=new Intent(mContext,NotSufficient.class);
                        startActivityForResult(intent,1);
                        CloseService();
                        myCountDownTimer.cancel();
                    }else {
                        String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                        if (!DataCalculateUtils.isEvent(stringWork,3)){
                            if (FormatToken.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardoutWater.class);
                                startActivityForResult(intent,1);
                                CloseService();
                                myCountDownTimer.cancel();
                            }else if (FormatToken.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppoutWater.class);
                                startActivityForResult(intent,1);
                                CloseService();
                                myCountDownTimer.cancel();
                            }else if (FormatToken.ConsumptionType==5){
                                Intent intent=new Intent(mContext,OutWater.class);
                                startActivityForResult(intent,1);
                                CloseService();
                                myCountDownTimer.cancel();
                            }
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
                            CloseService();
                            myCountDownTimer.cancel();

                        }
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void CalculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.Volume,"5");
        String Time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.outWaterTime,"30");
        int mVolume=Integer.valueOf(waterVolume).intValue();
        int mTime=Integer.valueOf(Time).intValue();
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
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
    private void initViewImages() {
        myPager = (MyImgScroll) findViewById(R.id.myvp);
        imageView = (ImageView) findViewById(R.id.textView);
        InitViewPagers();
        if (!listViews.isEmpty()&&listViews.size()>0) {
            myPager.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            myPager.start(this, listViews, 3000);
        }else{
            myPager.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
        }
    }
    private void InitViewPagers() {
        listViews = new ArrayList<View>();
        List<String> fileImagelist = new ArrayList<String>();
        File scanner5Directory = new File(Environment.getExternalStorageDirectory().getPath() + "/WaterSystem/images/");
        if (scanner5Directory.exists() && scanner5Directory.isDirectory()&&scanner5Directory.list().length > 0) {

            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg")|| path.endsWith(".png")) {
                    fileImagelist.add(path);
                    System.out.println("==path="+path);
                    System.out.println("==fileImagelist="+fileImagelist.size());
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
            finish();
            return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            //上一个
            finish();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            //下一个
            finish();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
        }
        return super.onKeyDown(keyCode, event);
    }
    private void removeback() {
        Message msg=new Message();
        msg.what=1;
        handlerStop.sendMessage(msg);
    }
    private void endTimeCount(){
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
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
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        removeback();
        endTimeCount();

    }
}
