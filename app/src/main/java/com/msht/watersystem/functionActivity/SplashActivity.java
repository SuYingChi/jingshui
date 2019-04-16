package com.msht.watersystem.functionActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.msht.watersystem.base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.utilpackage.CachePreferencesUtil;
import com.msht.watersystem.utilpackage.ConstantUtil;
import com.msht.watersystem.utilpackage.FormatInformationUtil;
import com.msht.watersystem.widget.LoadingDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public class SplashActivity extends BaseActivity  implements Observer{
    private PortService portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        loadingdialog = new LoadingDialog(mContext);
        loadingdialog.setLoadText("系统正在启动，请稍后...");
        loadingdialog.show();
        initData();
        bindPortService();
    }
    private void initData() {
        boolean isFirstOpen = CachePreferencesUtil.getBoolean(this, CachePreferencesUtil.FIRST_OPEN, true);
        if (isFirstOpen){
            CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_NUM,5);
            CachePreferencesUtil.putIntData(this,CachePreferencesUtil.WATER_OUT_TIME,30);
        }
    }
    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSKeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    onCom1Received105FromControlBoard(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    onCom2Received205DataFromServer(packet2.getData());
                }
            }
        }
    }
    private void onCom1Received105FromControlBoard(ArrayList<Byte> data) {      //接收到主板心跳指令
        if (data!=null&&data.size()>= ConstantUtil.HEARTBEAT_INSTRUCT_MAX_SIZE){
            FormatInformationUtil.saveStatusInformationToFormatInformation(data);
            if (loadingdialog!=null&&loadingdialog.isShowing()){
                loadingdialog.dismiss();
            }
           startActivity(new Intent(SplashActivity.this,
                    MainWaterImageActivity.class));
          /* startActivity(new Intent(SplashActivity.this,
                    MainMyVideoActivity.class));*/

            finish();
        }
    }
    private void onCom2Received205DataFromServer(ArrayList<Byte> data) {}
    private void bindPortService(){
        serviceConnection = new ComServiceConnection(SplashActivity.this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                portService = serviceConnection.getService();
            }
        });
        bindService(new Intent(mContext, PortService.class), serviceConnection,
                BIND_AUTO_CREATE);
    }
    private void unbindPortServiceAndRemoveObserver(){
        if (serviceConnection != null && portService != null) {
            portService.removeObserver(this);
            unbindService(serviceConnection);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPortServiceAndRemoveObserver();
    }
}
