package com.msht.watersystem.functionActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Handler;
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
import com.msht.watersystem.Interface.BitmapListener;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.ConsumeInformationUtils;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.CodeUtils;
import com.msht.watersystem.Utils.FormatInformationBean;
import com.msht.watersystem.Utils.FormatInformationUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.VariableUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class BuyWaterActivity extends BaseActivity implements Observer{
    private TextView    tv_time;
    private boolean     buyStatus=false;
    private boolean     bindStatus=false;
    private View        layout_online;
    private ImageView   ImageCode;
    private TextView    tv_freecharge;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;
    private double volume=0.00;
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            if(portService!=null){
                if (portService.isConnection()){
                    ImageCode.setVisibility(View.VISIBLE);
                    layout_online.setVisibility(View.GONE);
                    if (VariableUtil.setEquipmentStatus){
                        initsetData();
                    }
                }else {
                    ImageCode.setVisibility(View.GONE);
                    layout_online.setVisibility(View.VISIBLE);
                }
            }
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_water);
        mContext=this;
        VariableUtil.mPos=0;
       // initViewImages();
        myCountDownTimer=new MyCountDownTimer(120000,1000);
        initView();
        initWaterQuality();
        bindPortService();
        initNetWork();
        initData();
    }
    private void initsetData() {
        if (portService != null) {
            if (portService.isConnection()){
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
            }
        }
    }
    private void initNetWork() {
        handler.post(runnable);
    }
    private void bindPortService(){
        serviceConnection = new ComServiceConnection(BuyWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
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
        bindPortService();
        ShowUi();
        myCountDownTimer.start();
        if (requestCode==2){
            initNetWork();
        }
    }
    private void initView() {
        TextView tv_version=findViewById(R.id.id_tv_version);
        tv_freecharge=findViewById(R.id.id_free_charge);
        tv_time=findViewById(R.id.id_time) ;
        ImageCode=findViewById(R.id.id_erwei_code) ;
        layout_online=findViewById(R.id.id_online_view);
        ((TextView)findViewById(R.id.id_equipment)).setText(String.valueOf(FormatInformationBean.DeviceId));
        int ChargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGEMODE,0);
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
        String result=downloadapp+ creatQRCodeMessageJson();
        if (VariableUtil.QrCodebitmap!=null&&!VariableUtil.QrCodebitmap.isRecycled()){
            ImageCode.setImageBitmap(VariableUtil.QrCodebitmap);
        }else {
            createQRCodeWithLogo(result);
        }
    }
    private void createQRCodeWithLogo(String result) {
        CodeUtils.createQRCodeBitmap(mContext, result, new BitmapListener() {
            @Override
            public void onResultSuccess(boolean successStatus) {
                ImageCode.setImageBitmap(VariableUtil.QrCodebitmap);
            }
            @Override
            public void onResultFail(boolean failStatus) {
                Toast.makeText(mContext, "系统故障", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String creatQRCodeMessageJson() {
        String jsonresult="";
        String QrcodeType="1";
        String equipmentNo=String.valueOf(FormatInformationBean.DeviceId);
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
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            VariableUtil.skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板控制指令104：", CreateOrderType.getPacketString(packet1));
                    onCom1Received104dataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    onCom1Received204DataControllBoard();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataControllBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    response207ToServer(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    onCom2Received107DataFromServer(packet2.getData());
                   // Log.d("com107",CreateOrderType.getPacketString(packet2));
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());   //回复
                    }
                    onCom2Received104Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x03})){
                    onCom2Received203Data(packet2.getData());
                }
            }
        }
    }
    private void onCom2Received203Data(ArrayList<Byte> data) {
        setEquipmentData(data.get(4));
        try {
            if(data!=null&&data.size()>0){
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                String waterVolume=String.valueOf(FormatInformationBean.WaterNum);
                String Time=String.valueOf(FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.VOLUME,waterVolume);
                CachePreferencesUtil.putStringData(this,CachePreferencesUtil.OUT_WATER_TIME,Time);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.CHARGEMODE, FormatInformationBean.ChargeMode);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.SHOWTDS, FormatInformationBean.ShowTDS);
                VariableUtil.setEquipmentStatus=false;
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
                byte[] data= FormatInformationUtil.setEquipmentParameter(aByte);
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
    private void onCom2Received102Data(ArrayList<Byte> data) {
        if (ConsumeInformationUtils.controlModel(mContext,data)){
            ShowUi();
        }
    }
    private void ShowUi() {       //判断显示TDS
        int tds= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.SHOWTDS,0);
        int ChargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGEMODE,0);
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
    private void onCom2Received104Data(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,2);
            unbindPortServiceAndRemoveObserver();
            handler.removeCallbacks(runnable);
        }
    }
    private void response207ToServer(byte[] frame) {
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
    private void onCom1Received105DataControllBoard(ArrayList<Byte> data) {
        try {
            if (data!=null&&data.size()!=0){
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                tv_InTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,2);
                    unbindPortServiceAndRemoveObserver();
                    handler.removeCallbacks(runnable);
                    myCountDownTimer.cancel();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onCom1Received204DataControllBoard() {
        if (buyStatus){
            buyStatus=false;
            if (FormatInformationBean.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myCountDownTimer.cancel();
            }else if (FormatInformationBean.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myCountDownTimer.cancel();
            }else if (FormatInformationBean.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                startActivityForResult(intent,1);
                unbindPortServiceAndRemoveObserver();
                myCountDownTimer.cancel();
            }
        }
    }
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data!=null&&data.size()!=0){
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            buyStatus=true;
            if (FormatInformationBean.BusinessType==1){
                if (FormatInformationBean.AppBalance<20){
                    Intent intent=new Intent(mContext,AppNotSufficientActivity.class);
                    startActivityForResult(intent,1);
                    unbindPortServiceAndRemoveObserver();
                    myCountDownTimer.cancel();
                }else {
                    setBusiness(1);
                }
            }else if (FormatInformationBean.BusinessType==2){
                setBusiness(2);
            }
        }
    }
    private void setBusiness(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business==1){
                    byte[] data= FormatInformationUtil.setConsumeType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
                }else if (business==2){
                    byte[] data= FormatInformationUtil.setConsumeType02();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToControlBoard(packet);
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
    private void onCom1Received104dataFromControllBoard(ArrayList<Byte> data) {
        try {
                if(data!=null&&data.size()>0){
                    FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                    if (FormatInformationBean.Balance<=1){
                        Intent intent=new Intent(mContext,NotSufficientActivity.class);
                        startActivityForResult(intent,1);
                        unbindPortServiceAndRemoveObserver();
                        myCountDownTimer.cancel();
                    }else {
                        String stringWork= DataCalculateUtils.IntToBinary(FormatInformationBean.Updateflag3);
                        if (!DataCalculateUtils.isEvent(stringWork,3)){
                            if (FormatInformationBean.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                                startActivityForResult(intent,1);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }else if (FormatInformationBean.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                                startActivityForResult(intent,1);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }else if (FormatInformationBean.ConsumptionType==5){
                                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                                startActivityForResult(intent,1);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }
                        }else {
                            //刷卡结账
                            calculateData();    //没联网计算取缓存数据
                            double consumption= FormatInformationBean.ConsumptionAmount/100.0;
                            double waterVolume= FormatInformationBean.WaterYield*volume;
                            String afterAmount=String.valueOf(DataCalculateUtils.TwoDecinmal2(consumption));
                            String afterWater=String.valueOf(DataCalculateUtils.TwoDecinmal2(waterVolume));
                            String mAccount=String.valueOf(FormatInformationBean.StringCardNo);
                            Intent intent=new Intent(mContext,PaySuccessActivity.class);
                            intent.putExtra("afterAmount",afterAmount) ;
                            intent.putExtra("afetrWater",afterWater);
                            intent.putExtra("mAccount",mAccount);
                            intent.putExtra("sign","0");
                            startActivityForResult(intent,1);
                            unbindPortServiceAndRemoveObserver();
                            myCountDownTimer.cancel();

                        }
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void calculateData() {
        String waterVolume=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.VOLUME,"5");
        String time=CachePreferencesUtil.getStringData(this,CachePreferencesUtil.OUT_WATER_TIME,"30");
        int mVolume=Integer.valueOf(waterVolume);
        int mTime=Integer.valueOf(time);
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
        if (handler!=null){
            handler.removeCallbacks(runnable);
        }
    }
    private void endTimeCount(){
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


}
