package com.msht.watersystem.functionActivity;


import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.os.CountDownTimer;
import android.os.Bundle;


import android.util.Log;
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
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.Interface.BitmapListener;
import com.msht.watersystem.R;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.ConsumeInformationUtils;
import com.msht.watersystem.utilpackage.ByteUtils;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.CodeUtils;
import com.msht.watersystem.utilpackage.CreateOrderType;
import com.msht.watersystem.utilpackage.FormatInformationBean;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.utilpackage.DataCalculateUtils;
import com.msht.watersystem.utilpackage.VariableUtil;
import com.msht.watersystem.widget.BannerM;
import com.msht.watersystem.widget.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;

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
public class BuyWaterActivity extends BaseActivity implements Observer{
    private TextView tvTime;
    private boolean  buyStatus=false;
    private boolean  bindStatus=false;
    /**
     * @parame  mAppFrame 扫码发送104帧序
     */
    private byte[]  mAppFrame;
    private View     layoutOnline;
    private ImageView imageCode;
    private TextView tvFreeCharge;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private MyCountDownTimer myCountDownTimer;
    private double volume=0.00;
    private void setNetWorkConnectionUI(){
        if(portService!=null){
            if (portService.isConnection()){
                imageCode.setVisibility(View.VISIBLE);
                layoutOnline.setVisibility(View.GONE);
                if (VariableUtil.setEquipmentStatus){
                    initSetData();
                }
            }else {
                imageCode.setVisibility(View.GONE);
                layoutOnline.setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_water);
        mContext=this;
        VariableUtil.mPos=0;
        myCountDownTimer=new MyCountDownTimer(120000,1000);
        initView();
        initViewImages();
        initWaterQuality();
        bindAndAddObserverToPortService();
        initData();
    }
    private void initViewImages() {
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
  private void initSetData() {
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
    private void bindAndAddObserverToPortService(){
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
    private void initView() {
        TextView tvVersion=findViewById(R.id.id_tv_version);
        tvFreeCharge =findViewById(R.id.id_free_charge);
        tvTime =findViewById(R.id.id_time) ;
        imageCode =findViewById(R.id.id_erwei_code) ;
        layoutOnline =findViewById(R.id.id_online_view);
        ((TextView)findViewById(R.id.id_equipment)).setText(String.valueOf(FormatInformationBean.equipmentNo));
        int chargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGE_MODE,0);
        if (chargeMode==1){
            tvFreeCharge.setVisibility(View.VISIBLE);
        }else {
            tvFreeCharge.setVisibility(View.GONE);
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            String name="version:"+pi.versionName;
            tvVersion.setText(name);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initData() {
        myCountDownTimer.start();
        String downloadApp="http://msbapp.cn/download/success.html?QRCodeJson=";
        String result=downloadApp+ createQRCodeMessageJson();
        if (VariableUtil.qrCodeBitmap !=null&&!VariableUtil.qrCodeBitmap.isRecycled()){
            imageCode.setImageBitmap(VariableUtil.qrCodeBitmap);
        }else {
            if (FormatInformationBean.equipmentNo!=0){
                createQRCodeWithLogo(result);
            }else {
                ToastUtils.onToastLong("设备号异常，请稍后25秒");
                finish();
            }
        }
    }
    private void createQRCodeWithLogo(String result) {
        CodeUtils.createQRCodeBitmap(mContext, result, new BitmapListener() {
            @Override
            public void onResultSuccess(boolean successStatus) {
                imageCode.setImageBitmap(VariableUtil.qrCodeBitmap);
            }
            @Override
            public void onResultFail(boolean failStatus) {
                Toast.makeText(mContext, "系统故障", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String createQRCodeMessageJson() {
        String jsonResult="";
        String qrCodeType="1";
        String equipmentNo=String.valueOf(FormatInformationBean.equipmentNo);
        JSONObject object=new JSONObject();
        try{
            JSONObject jsonData=new JSONObject();
            object.put("QrcodeType",qrCodeType);
            jsonData.put("equipmentNo",equipmentNo);
            object.put("data",jsonData);
            jsonResult=object.toString();
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jsonResult;
    }
    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            VariableUtil.mKeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板控制指令104：", CreateOrderType.getPacketString(packet1));
                    onCom1Received104dataFromControlBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    onCom1Received204DataControlBoard(packet1.getFrame());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataControlBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                Log.d("Com2Packet=",CreateOrderType.getPacketString(packet2));
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    response207ToServer(packet2.getFrame());
                    VariableUtil.byteArray.clear();
                    VariableUtil.byteArray=packet2.getData();
                    onCom2Received107DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
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
        try {
            if(data!=null&&data.size()>=ConstantUtil.REQUEST_MAX_SIZE){
                setEquipmentData(data.get(4));
                FormatInformationUtil.saveDeviceInformationToFormatInformation(data);
                CachePreferencesUtil.getIntData(this,CachePreferencesUtil.PRICE,FormatInformationBean.PriceNum);
                CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_OUT_TIME,FormatInformationBean.OutWaterTime);
                CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_NUM,FormatInformationBean.WaterNum);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.CHARGE_MODE, FormatInformationBean.ChargeMode);
                CachePreferencesUtil.putChargeMode(this,CachePreferencesUtil.SHOW_TDS, FormatInformationBean.ShowTDS);
                CachePreferencesUtil.getIntData(this,CachePreferencesUtil.DEDUCT_AMOUNT,FormatInformationBean.DeductAmount);
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
            onShowDTS();
        }
    }
    private void onShowDTS() {       //判断显示TDS
        int tds= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.SHOW_TDS,0);
        int chargeMode= CachePreferencesUtil.getChargeMode(this,CachePreferencesUtil.CHARGE_MODE,0);
        if (chargeMode==1){
            tvFreeCharge.setVisibility(View.VISIBLE);
        }else {
            tvFreeCharge.setVisibility(View.GONE);
        }
        if (tds==0){
            layoutTDS.setVisibility(View.GONE);
        }else {
            layoutTDS.setVisibility(View.VISIBLE);
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
        try{
            if (data!=null&&data.size()>=ConstantUtil.CONTROL_MAX_SIZE){
                String stringWork= DataCalculateUtils.intToBinary(ByteUtils.byteToInt(data.get(45)));
                int switchStatus=ByteUtils.byteToInt(data.get(31));
                if (switchStatus==2&&DataCalculateUtils.isEvent(stringWork,0)){
                    Intent intent=new Intent(mContext, CloseSystemActivity.class);
                    startActivityForResult(intent,2);
                    unbindPortServiceAndRemoveObserver();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
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
    private void onCom1Received105DataControlBoard(ArrayList<Byte> data) {
        try {
            if (data!=null&&data.size()>= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE){
                FormatInformationUtil.saveStatusInformationToFormatInformation(data);
                tvInTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
                tvOutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
                String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                    startActivityForResult(intent,2);
                    unbindPortServiceAndRemoveObserver();
                    myCountDownTimer.cancel();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void onCom1Received204DataControlBoard(byte[] frame) {
        if (Arrays.equals(frame,mAppFrame)){
            if (buyStatus){
                buyStatus=false;
                if (FormatInformationBean.ConsumptionType==1){
                    Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                    startActivity(intent);
                    unbindPortServiceAndRemoveObserver();
                    myCountDownTimer.cancel();
                }else if (FormatInformationBean.ConsumptionType==3){
                    Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                    startActivity(intent);
                    unbindPortServiceAndRemoveObserver();
                    myCountDownTimer.cancel();
                }else if (FormatInformationBean.ConsumptionType==5){
                    Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                    startActivity(intent);
                    unbindPortServiceAndRemoveObserver();
                    myCountDownTimer.cancel();
                }
            }
        }
    }
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data!=null&&data.size()>=ConstantUtil.BUSINESS_MAX_SIZE){
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            CachePreferencesUtil.putBoolean(this,CachePreferencesUtil.FIRST_OPEN,false);
            buyStatus=true;
            if (FormatInformationBean.BusinessType==1){
                if (FormatInformationBean.AppBalance<20){
                    Intent intent=new Intent(mContext,AppNotSufficientActivity.class);
                    startActivity(intent);
                   // unbindPortServiceAndRemoveObserver();
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
                mAppFrame=frame;
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
    private void onCom1Received104dataFromControlBoard(ArrayList<Byte> data) {
        try {
                if(data!=null&&data.size()>0){
                    FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                    String stringWork= DataCalculateUtils.intToBinary(FormatInformationBean.Updateflag3);
                    if (!DataCalculateUtils.isEvent(stringWork,3)){
                        if (FormatInformationBean.Balance<=1){
                            Intent intent=new Intent(mContext,NotSufficientActivity.class);
                            startActivity(intent);
                          //  unbindPortServiceAndRemoveObserver();
                            myCountDownTimer.cancel();
                        }else {
                            if (FormatInformationBean.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardOutWaterActivity.class);
                                startActivity(intent);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }else if (FormatInformationBean.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                                startActivity(intent);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }else if (FormatInformationBean.ConsumptionType==5){
                                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                                startActivity(intent);
                                unbindPortServiceAndRemoveObserver();
                                myCountDownTimer.cancel();
                            }
                        }
                    }else {
                        //刷卡结账
                        calculateData();    //没联网计算取缓存数据
                        double consumption= FormatInformationBean.ConsumptionAmount/100.0;
                        double waterVolume= FormatInformationBean.WaterYield*volume;
                        String afterAmount=String.valueOf(DataCalculateUtils.getTwoDecimal(consumption));
                        String afterWater=String.valueOf(DataCalculateUtils.getTwoDecimal(waterVolume));
                        String mAccount=String.valueOf(FormatInformationBean.StringCardNo);
                        Intent intent=new Intent(mContext,PaySuccessActivity.class);
                        intent.putExtra("afterAmount",afterAmount) ;
                        intent.putExtra("afterWater",afterWater);
                        intent.putExtra("mAccount",mAccount);
                        intent.putExtra("sign","0");
                        startActivity(intent);
                        unbindPortServiceAndRemoveObserver();
                        myCountDownTimer.cancel();

                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void calculateData() {
        int mVolume=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_NUM,5);
        int mTime=CachePreferencesUtil.getIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);
        volume=DataCalculateUtils.getWaterVolume(mVolume,mTime);
    }
    private void onControlScreenBackground(int status){
        if (portService!=null){
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                byte[] data = FormatInformationUtil.setCloseScreenData(status);
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
        private MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            String mUntilFinishedText=millisUntilFinished/1000+"";
            tvTime.setText(mUntilFinishedText);
            setNetWorkConnectionUI();
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
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            //上一个
            finish();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            //下一个
            finish();
        }
        return super.onKeyDown(keyCode, event);
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
    protected void onRestart() {
        super.onRestart();
        bindAndAddObserverToPortService();
        onShowDTS();
        myCountDownTimer.start();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
        endTimeCount();
    }
}
