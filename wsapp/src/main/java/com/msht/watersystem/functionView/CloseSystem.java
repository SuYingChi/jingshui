package com.msht.watersystem.functionView;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.CreateOrderType;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.MyLogUtil;
import com.msht.watersystem.widget.LoadingDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class CloseSystem extends BaseActivity implements Observer, Handler.Callback {
    private PortService portService;
    private boolean  bindStatus=false;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_system);
        loadingdialog = new LoadingDialog(mContext);
        loadingdialog.setLoadText("很抱歉，此设备暂停服务...");
        loadingdialog.show();
        OpenService();
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
                    MyLogUtil.d("主板控制指令104：", CreateOrderType.getPacketString(packet1));
                    initCom104Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    MyLogUtil.d("主板控制指令105：", CreateOrderType.getPacketString(packet1));
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    MyLogUtil.d("服务端控制指令205：", CreateOrderType.getPacketString(packet2));
                    initCom205Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    MyLogUtil.d("服务端控制指令104：", CreateOrderType.getPacketString(packet2));
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                }
            }
        }
    }
    private void initCom104Data() {}
    private void initCom105Data(ArrayList<Byte> data) {}
    private void initCom205Data() {}
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
        int Switch= ByteUtils.byteToInt(data.get(31));
        if (Switch==1&&DataCalculateUtils.isEvent(stringWork,0)){
            if (loadingdialog != null && loadingdialog.isShowing()) {
                loadingdialog.dismiss();
            }
            finish();
        }
    }

    private void OpenService(){
        serviceConnection = new ComServiceConnection(CloseSystem.this, new ComServiceConnection.ConnectionCallBack() {
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
            showTips();
        }
        return false;
    }
    private void showTips() {
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
    }
}
