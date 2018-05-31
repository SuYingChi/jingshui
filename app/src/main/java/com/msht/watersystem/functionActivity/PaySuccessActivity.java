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
import com.msht.watersystem.Utils.ConsumeInformationUtils;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.FormatInformationBean;
import com.msht.watersystem.Utils.FormatInformationUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.VariableUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class PaySuccessActivity extends BaseActivity implements Observer {
    private boolean     buyStatus=false;
    private boolean     bindStatus=false;
    private Context     mContext;
    private MyCountDownTimer myCountDownTimer;
    private ImageView   textView;
    private TextView    tv_time;
    private TextView    tv_customerNo;
    private TextView    tv_water;
    private TextView    tv_amount;
    private TextView    tv_balance;
    private TextView    tv_success;
    private String      mAccount;
    private String      sign;
    private String      afterAmount;
    private String      afetrWater;
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
        initView();
        initWaterQuality();
        bindPortService();
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        myCountDownTimer.start();
    }
    private void bindPortService() {
        serviceConnection = new ComServiceConnection(PaySuccessActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
            double afterconsumpte= FormatInformationBean.AfterAmount/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }else if (sign.equals("1")){
            tv_success.setText("付款成功");
            tv_amount.setVisibility(View.VISIBLE);
            tv_amount.setText("成功消费了"+afterAmount+"元");
            tv_water.setText("共购买了"+afetrWater+"升的水");
            double afterconsumpte= FormatInformationBean.AppBalance/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }else if (sign.equals("2")){
            tv_amount.setVisibility(View.INVISIBLE);
            tv_success.setText("充值成功");
            double rechargeAmount= FormatInformationBean.rechargeAmount/100.0;
            tv_water.setText("成功充值了"+String.valueOf(DataCalculateUtils.TwoDecinmal2(rechargeAmount))+"元");
            double afterconsumpte= FormatInformationBean.Balance/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }else if (sign.equals("3")){
            tv_success.setText("付款成功");
            tv_amount.setVisibility(View.VISIBLE);
            tv_amount.setText("成功消费了"+afterAmount+"元");
            tv_water.setText("共购买了"+afetrWater+"升的水");
            double afterconsumpte= FormatInformationBean.Balance/100.0;
            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
        }
    }

    @Override
    public void update(Observable observable, Object arg) {

        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板回复指令104",CreateOrderType.getPacketString(packet1));
                    onCom1Received104DataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105DataFromControllBoard(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    onCom1Received204DataFromControllBoard();
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    onCom2Received205DataFromServer();
                }else  if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        response204ToServer(packet2.getFrame());
                    }
                    onCom2Received104DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    response207ToServer(packet2.getFrame());
                    onCom2Received107DataFromServer(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102ToServer(packet2.getFrame());
                    onCom2Received102DataFromServer(packet2.getData());
                }

            }
        }
    }
    private void onCom1Received204DataFromControllBoard() {
        if (buyStatus){
            buyStatus=false;
            if (FormatInformationBean.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                startActivityForResult(intent,1);
                finish();
            }else if (FormatInformationBean.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                startActivityForResult(intent,1);
                finish();
            }else if (FormatInformationBean.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
                startActivityForResult(intent,1);
                finish();
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
        if (ConsumeInformationUtils.controlModel(mContext,data)){
            if (FormatInformationBean.ShowTDS==0){
                layout_TDS.setVisibility(View.GONE);
            }else {
                layout_TDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void onCom2Received107DataFromServer(ArrayList<Byte> data) {
        if (data!=null&&data.size()!=0){
            ConsumeInformationUtils.saveConsumptionInformationToFormatInformation(data);
            if (FormatInformationBean.BusinessType==3){
                if (sign.equals("0")){
                    FormatInformationBean.AfterAmount= FormatInformationBean.AfterAmount+ FormatInformationBean.rechargeAmount;
                    double afterconsumpte= FormatInformationBean.AfterAmount/100.0;
                    tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
                }
            }else {
                VariableUtil.byteArray.clear();
                VariableUtil.byteArray=data;
                buyStatus=true;
                if (FormatInformationBean.BusinessType==1){
                    if (FormatInformationBean.AppBalance<20){
                        Intent intent=new Intent(mContext,AppNotSufficientActivity.class);
                        startActivityForResult(intent,1);
                        finish();
                    }else {
                        setBusiness(1);
                    }
                }else if (FormatInformationBean.BusinessType==2){
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
    private void onCom2Received104DataFromServer(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,2);
            finish();
        }
    }
    private void onCom2Received205DataFromServer() {}
    private void onCom1Received105DataFromControllBoard(ArrayList<Byte> data) {
        if (data!=null&&data.size()!=0){
            FormatInformationUtil.saveStatusInformationToFormatInformation(data);
            tv_InTDS.setText(String.valueOf(FormatInformationBean.OriginTDS));
            tv_OutTDS.setText(String.valueOf(FormatInformationBean.PurificationTDS));
            String stringWork= DataCalculateUtils.IntToBinary(FormatInformationBean.WorkState);
            if (!DataCalculateUtils.isEvent(stringWork,6)){
                Intent intent=new Intent(mContext, CannotBuyWaterActivity.class);
                startActivityForResult(intent,1);
                finish();
            }
        }
    }
    private void onCom1Received104DataFromControllBoard(ArrayList<Byte> data) {
        try {
            if( data!=null&&data.size()>0){
                FormatInformationUtil.saveCom1ReceivedDataToFormatInformation(data);
                    String stringWork= DataCalculateUtils.IntToBinary(FormatInformationBean.Updateflag3);
                    if (DataCalculateUtils.isEvent(stringWork,3)){
                        if (FormatInformationBean.ConsumptionType==1){
                            double afterconsumpte= FormatInformationBean.AfterAmount/100.0;
                            tv_balance.setText(String.valueOf(DataCalculateUtils.TwoDecinmal2(afterconsumpte)));
                        }
                    }else {
                        if (FormatInformationBean.Balance<=20){
                            Intent intent=new Intent(mContext,NotSufficientActivity.class);
                            startActivityForResult(intent,1);
                            finish();
                        }else {
                            if (FormatInformationBean.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardoutWaterActivity.class);
                                startActivityForResult(intent,1);
                                finish();
                            }else if (FormatInformationBean.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppOutWaterActivity.class);
                                startActivityForResult(intent,1);
                                finish();
                            }else if (FormatInformationBean.ConsumptionType==5){
                                Intent intent=new Intent(mContext,DeliverOutWaterActivity.class);
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
            tv_time.setText("60");
            finish();
        }
    }
    private void endTime() {
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finishPage();
           // return false;
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            finishPage();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            finishPage();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            finishPage();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            finishPage();
        }
        return true;
      //  return super.onKeyDown(keyCode, event);
    }

    private void finishPage() {
        finish();
    }
    private void unbindPortServiceAndRemoveObserver(){
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
        unbindPortServiceAndRemoveObserver();
        endTime();
    }
}
