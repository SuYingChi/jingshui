package com.msht.watersystem.functionActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.InstructUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class CannotBuyWaterActivity extends BaseActivity implements Observer {
    private boolean  bindStatus=false;
    private Context mContext;
    private ImageView textView;
    private PortService portService;
    private ComServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cannot_buywater);
        mContext=this;
        initWaterQuality();
        OpenService();
    }
    private void OpenService(){
        serviceConnection = new ComServiceConnection(CannotBuyWaterActivity.this, new ComServiceConnection.ConnectionCallBack() {
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
            boolean skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板回复指令104：", CreateOrderType.getPacketString(packet1));
                    initCom104Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    initCom205Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("服务端控制指令104：",CreateOrderType.getPacketString(packet2));
                    initCom104Data2(packet2.getData());
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                }
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
            Intent intent=new Intent(mContext, CloseSystemActivity.class);
            startActivityForResult(intent,2);
            CloseService();
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
    private void initCom104Data() {}
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (InstructUtil.StatusInstruct(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (DataCalculateUtils.isEvent(stringWork,6)){
                    finish();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom205Data() {}
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
        }else if (keyCode==KeyEvent.KEYCODE_MENU){

        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){

        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){

        }else if (keyCode==KeyEvent.KEYCODE_F1){
        }
        return false;
       // return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
    }
}
