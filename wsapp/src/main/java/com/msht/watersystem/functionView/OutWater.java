package com.msht.watersystem.functionView;

import android.content.Context;
import android.content.Intent;
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
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.MainSerialPort;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.StatusInstructUtil;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.LEDView;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class OutWater extends BaseActivity implements Observer, Handler.Callback{
    private View layout_finish;
    private MyImgScroll myPager;
    private List<View>  listViews;
    private ImageView   textView;
    private TextView    tv_CardNo;
    private TextView    tv_volume;
    private LEDView     le_water,le_amount;
    private boolean     bindStatus=false;
    private int         second=0;
    private double      volume=0.00;
    private double      priceNum=0.00;
    private String      afterAmount;
    private String      afetrWater;
    private int         EnsureState=0;
    private int         ReceiveState=0;
    private boolean     Currentstatus=false;
    private ArrayList<Byte> byteArrayList=new ArrayList<Byte>();
    private ArrayList<Byte> byteList=new ArrayList<Byte>();
    private Context     mContext;
    private PortService portService;
    private ComServiceConnection serviceConnection;
    private final static String TAG = MainSerialPort.class.getSimpleName();
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
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            second++;
            double Volume=second*volume;
            double amount=second*priceNum;
            double mVolume=DataCalculateUtils.TwoDecinmal2(Volume);
            double mAmount=DataCalculateUtils.TwoDecinmal2(amount);
            afetrWater=String.valueOf(mVolume);
            afterAmount=String.valueOf(mAmount);
            le_water.setLedView(getString(R.string.default_bg_digital),String.valueOf(mVolume));
            le_amount.setLedView(getString(R.string.default_bg_digital),String.valueOf(mAmount));
            handler.postDelayed(this,1000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_water);
        mContext=this;
        initViewImages();
        initView();
        iniWaterQualiy();
        OpenService();
    }
    private void initView() {
        layout_finish=findViewById(R.id.id_re_code);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_customerNo);
        tv_volume=(TextView)findViewById(R.id.id_getwater_volume);
        le_amount=(LEDView)findViewById(R.id.id_pay_amount);
        le_water=(LEDView)findViewById(R.id.id_waster_yield);
        tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
        tv_volume.setText(String.valueOf(FormatToken.WaterNum));
    }
    private void OpenService(){
        serviceConnection = new ComServiceConnection(OutWater.this, new ComServiceConnection.ConnectionCallBack() {
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
                byteArrayList.clear();
                byteArrayList=packet1.getData();
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    initCom204Data();
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                byteList.clear();
                byteList=packet2.getData();
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x07})){
                    initCom207Data();
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }
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

    private void initCom105Data(ArrayList<Byte> data) {
        try {
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
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom204Data() {
        if (Currentstatus){
            volume= DataCalculateUtils.getWaterVolume(FormatToken.WaterNum,FormatToken.OutWaterTime);
            priceNum=DataCalculateUtils.getWaterPrice(FormatToken.PriceNum);
            handler.post(runnable);
            Currentstatus=false;
        }else {
            if (ReceiveState==2){
                Message msg=new Message();
                msg.what=1;
                handlerStop.sendMessage(msg);
            }else if (ReceiveState==0){
                Toast.makeText(mContext,"未进行任何操作",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
        if (ByteUtils.byteToInt(data.get(15))==5){
            Message msg=new Message();
            msg.what=1;
            handlerStop.sendMessage(msg);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent=new Intent(OutWater.this,PaySuccess.class);
                    intent.putExtra("afterAmount",afterAmount) ;
                    intent.putExtra("afetrWater",afetrWater);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        }
    }
    private void initCom207Data() {
        layout_finish.setVisibility(View.VISIBLE);
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
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            if (EnsureState==0){
                if (portService != null) {
                    Currentstatus=true;
                    EnsureState=1;
                    ReceiveState=1;
                    portService.sendToCom1(Cmd.ComCmd._START_);
                }
            }else if (EnsureState==1){
                if (portService != null) {
                    EnsureState=0;
                    Currentstatus=false;
                    ReceiveState=2;
                    portService.sendToCom1(Cmd.ComCmd._END_);
                    Message msg=new Message();
                    msg.what=1;
                    handlerStop.sendMessage(msg);
                }
            }
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){

        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){

        }else if (keyCode==KeyEvent.KEYCODE_F1){
            showTips();
        }
        return false;
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
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
    }

}
