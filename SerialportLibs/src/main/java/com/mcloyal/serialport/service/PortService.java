package com.mcloyal.serialport.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.R;
import com.mcloyal.serialport.connection.ClientConfig;
import com.mcloyal.serialport.connection.MinaClient;
import com.mcloyal.serialport.constant.Cmd;
import com.mcloyal.serialport.constant.ConstantUtil;
import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.AnalysisException;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.utils.AnalysisUtils;
import com.mcloyal.serialport.utils.ByteUtils;
import com.mcloyal.serialport.utils.NumberUtil;
import com.mcloyal.serialport.utils.PacketUtils;
import com.mcloyal.serialport.utils.SpecialUtils;
import com.mcloyal.serialport.utils.StringUtils;
import com.mcloyal.serialport.utils.logs.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android_serialport_api.SerialPort;

/**
 * 串口通信常驻后台Service
 * @author hong
 */
public class PortService extends Service {
    private final static String TAG = PortService.class.getSimpleName();
    private AppLibsContext appLibsContext = null;
    private MinaClient minaClient;
    /**
     *主控制板为COM1，参数
     */
    protected SerialPort mSerialPort1;
    protected volatile static OutputStream mOutputStream1;
    private volatile static InputStream mInputStream1;
    private ScheduledExecutorService scheduledThreadPool;
    private Vector<Byte> data1 = new Vector<Byte>();
    public static AtomicInteger PACKET_LEN1 = new AtomicInteger(100);
    /**
     *通讯模块为COM2，参数
     */
    protected SerialPort mSerialPort2;
    protected volatile static OutputStream mOutputStream2;
    private volatile static InputStream mInputStream2;
    private  Vector<Byte> data2 = new Vector<Byte>();
    public static AtomicInteger  PACKET_LEN2 = new AtomicInteger(100);
    private MyObservable mObservable;
    /**
     * 是否联网
     */
    private volatile boolean  isConnection = false;
    /**
     * 是否在联网中
     */
    private volatile boolean isAtCommandTaskRunning = false;
    /**
     * COM1常量标记
     */
    private final static int COM1_SERIAL = 1;
    /**
     * COM2常量标记
     */
    private final static int COM2_SERIAL = 2;
   // private final static int COM1_COUNT = 4;
    /**
     *  重启服务标记
     */
    private final static int RESTART = 3;
    /**
     *  最大包数
     */
    private final static int MAX_PGK_TIME = 5;
    /**
     *  发送包数据计数
     */
    private static AtomicInteger  pgkTime = new AtomicInteger(0);
    //TCP连接返回值等待时间
    private final static int TCP_MAX_OUT_TIME = 10 * 1000;


    private static  AtomicInteger  count = new AtomicInteger(0);
    private Handler handler = new PortServiceHandler(PortService.this);
    /**
     * 线程间通信Handler，通知关机，触发activity主线程界面更新
     */
    /**
     *倒计时最大等大时长
     */
    private static final long MAX_COUNT = 5 * 60;

    private static class PortServiceHandler extends Handler{
        private  WeakReference<PortService> weakPortService;
        PortServiceHandler(PortService portService){
            weakPortService= new WeakReference<PortService>(portService);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COM1_SERIAL:
                    Packet packet1 = (Packet) msg.obj;
                    if (weakPortService.get().mObservable != null && packet1 != null) {
                        weakPortService.get().mObservable.setCom1ReceivedPacket(packet1);
                    }
                    break;
                case COM2_SERIAL:
                    Packet packet2 = (Packet) msg.obj;
                    if (weakPortService.get().mObservable != null && packet2 != null) {
                        weakPortService.get().mObservable.setCom2ReceivedPacket(packet2);
                    }
                    break;
                case RESTART:
                    //重启服务
                    weakPortService.get().sendNetRestartCmd();
                    break;
                default:
                    break;
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mObservable = new MyObservable();
        appLibsContext = (AppLibsContext) getApplication();
        /*
         * 定时接收Com1数据常驻子线程
         * 定时接收Com2数据常驻子线程
         * 定时解析Com1数据常驻子线程
         * 定时解析Com2数据常驻子线程
         * 倒计时常驻子线程
         * At指令操作子线程，由其他常驻子线程触发开启任务
         */
        scheduledThreadPool = new ScheduledThreadPoolExecutor(6, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable  r) {
                return new Thread(r,Thread.currentThread().getName());
            }
        });
        //开启COM1接收线程  主板和Android板之前数据通信
        startReadCom1Thread();
        //启动COM1数据拼包线程
        scheduledThreadPool.scheduleAtFixedRate(new ParserCom1ReceivedDataTask(), 0, 100,
                TimeUnit.MILLISECONDS);

        //开启COM2接收线程 ,通信模块和Android板之间通信
        startCom2Received();
        //启动COM2数据拼包线程
        scheduledThreadPool.scheduleAtFixedRate(new ParserCom2ReceivedDataTask(),0,100,TimeUnit.MILLISECONDS);
        scheduledThreadPool.scheduleAtFixedRate(new CountdownTask(),0,1,TimeUnit.SECONDS);
        //建造者模式进行相关配置
        ClientConfig clientConfig = new ClientConfig.Builder().setIp(Cmd.IP_ADDRESS).setPort(Cmd.PORT).build();
        minaClient = new MinaClient(clientConfig,scheduledThreadPool);
        //状态进行监听
        minaClient.setClientStateListener(new MinaClient.ClientStateListener() {
            @Override
            public void sessionCreated() {
                Log.d(TAG, "client sessionCreated");
            }

            @Override
            public void sessionOpened() {
                Log.d(TAG, "client sessionOpened ");

            }

            @Override
            public void sessionClosed() {
                Log.d(TAG, "client sessionClosed ");
            }

            @Override
            public void messageReceived(byte[] message) {
                Log.d(TAG, "client messageReceived "+ByteUtils.byte2hex(message));
            }
            @Override
            public void messageSent(byte[] message) {
                Log.d(TAG, "client messageSent "+ByteUtils.byte2hex(message));
            }
        });

    }
  private class CountdownTask implements Runnable {
        @Override
        public void run() {
            int incrementAndGet = count.incrementAndGet();
            if (incrementAndGet > MAX_COUNT) {
                count.set(0);
             //   LogUtils.d(TAG, MAX_COUNT + "s 倒计时结束，执行断电重启");
                handler.sendEmptyMessage(RESTART);
            }
        }
    }
    /*---------------------------  主控制板COM1数据接收发（开始）-----------------------------------*/
    /**
     * COM1轮询线程任务
     */
    private class ParserCom1ReceivedDataTask implements Runnable {
        @Override
        public void run() {
           // LogUtils.d(TAG, "ParseReadCom1Task线程");
            if (data1 != null && data1.size() >= PACKET_LEN1.get()) {
                Byte[] data = data1.subList(0, PACKET_LEN1.get()).toArray(new Byte[PACKET_LEN1.get()]);
                if (data[0] != ConstantUtil.START_) {
                    data1.clear();
                } else {
                    parserCom1DataTask(data);
                }
            }
        }
    }
    /**
     * 开启COM1口数据接收线程
     */
    private void startReadCom1Thread() {
        try {
            mSerialPort1 = appLibsContext.getSerialPort1();
            mOutputStream1 = mSerialPort1.getOutputStream();
            mInputStream1 = mSerialPort1.getInputStream();
            scheduledThreadPool.scheduleWithFixedDelay(new ReadCom1DataTask(),100,100,TimeUnit.MILLISECONDS);
        } catch (SecurityException e) {
            LogUtils.d(TAG, getString(R.string.error_security));
        } catch (IOException e) {
            LogUtils.d(TAG, getString(R.string.error_unknown));
        } catch (InvalidParameterException e) {
            LogUtils.d(TAG, getString(R.string.error_configuration));
        }
    }
    private class ReadCom1DataTask implements Runnable {
        @Override
        public void run() {
                int size;
                try {
                    byte[] buffer = new byte[PACKET_LEN1.get()];
                    if (mInputStream1 == null) {
                        return;
                    }
                    size = mInputStream1.read(buffer);
                    //过滤出现一个单字节的数据包
                    if (size > 0) {
                        byte[] data = Arrays.copyOfRange(buffer, 0, size);
                       // LogUtils.d(TAG, "COM1接收到" + size + " 字节");
                       // LogUtils.d(TAG, "COM1接收数据data==" + ByteUtils.byte2hex(data));
                        //有新的数据包
                        if (  data[0] == ConstantUtil.START_) {
                            data1.clear();
                            //data1 = new ArrayList<>();
                            //包头+2字节长度
                            if (size >= 3) {
                                int len = NumberUtil.byte2ToUnsignedShort(new byte[]{data[1], data[2]});
                                //len不包含包头长度，1为包头的长度
                                PACKET_LEN1.set( len + 1);
                            }
                        }
                        for (byte aData : data) {
                            data1.add(aData);
                        }
                        //在此处进行包条件对比
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }
    /**
     * COM1数据解析
     *
     * @param bf 要解析的数据
     */
    protected void parserCom1DataTask(Byte[] bf) {
        int size = bf.length;
        byte[] buffer = new byte[size];
        for (int i = 0; i < bf.length; i++) {
            buffer[i] = bf[i];
        }
        data1.clear();
       // LogUtils.d(TAG, "COM1数据包完整 parserCom1DataTask data==" + ByteUtils.byte2hex(buffer));
        if (size >= PACKET_LEN1.get()) {
            try {
                Packet packet = AnalysisUtils.analysisFrame(buffer, PACKET_LEN1.get());
                if (packet != null) {
                    //此处无需做CRC校验判断
                    byte[] cmd = packet.getCmd();
                    //不管是否断网都直接转发固定105
                    byte[] frame=packet.getFrame();
                    if (Arrays.equals(cmd, new byte[]{0x01, 0x05})) {
                       // LogUtils.d(TAG, "COM1接收到105，重置倒计时操作");
                        //重置倒计时计时
                        count.set(0);
                       // sendToControlBoard(Cmd.ComCmd._NONET_HEARTBEAT_);
                        response205ToControlBoard(frame);
                        //COM1接收到104 直接回复
                    }else if (Arrays.equals(cmd, new byte[]{0x01, 0x04})){
                        response204ToControlBoard(frame);
                    }
                    //如果在联网的情况下直接把数据转发到TCP平台
                    if (true/*isConnection*/) {
                      //  LogUtils.d(TAG, "上送到服务器" + ByteUtils.byte2hex(buffer));
                        //主控板的105指令是连接指令，5次未连接上就断电重启
                        if (Arrays.equals(cmd, new byte[]{0x01, 0x05})) {
                            int incrementAndGet= pgkTime.incrementAndGet();
                           // LogUtils.e(TAG, "数据包发送次数：" + incrementAndGet);
                            //如果超过最大包数判断
                            if (incrementAndGet > MAX_PGK_TIME) {
                              //  LogUtils.d(TAG, "已经超过最大包数阈值，执行断电重启");
                                handler.sendEmptyMessage(RESTART);
                            }
                        }
                        /*判断是否com104,消费类型3,结账数据,为该数据不发后台*/
                        if (AnalysisUtils.isCmd104ConsumptionType3(packet)){
                           // sendToServer(buffer);
                            minaClient.sendMessage(buffer);
                            Log.d(TAG, "parserCom1Data="+ByteUtils.byte2hex(buffer));
                        }
                        //主控板发送给com1的数据都会转发给后端
                       // sendToServer(buffer);
                    } else {
                        //添加注释
                        /*if (Arrays.equals(cmd, new byte[]{0x01, 0x04})) {//回复
                            sendToControlBoard(Cmd.ComCmd._NONET_AT104_);
                        }*/
                       // LogUtils.d(TAG, "离线状态，判断是否需要进行断电重启...");
                        handler.sendEmptyMessage(RESTART);
                    }
                    Message msg = Message.obtain();
                    msg.obj = packet;
                    msg.what = COM1_SERIAL;
                    handler.sendMessage(msg);
                }
            } catch (AnalysisException e) {
                e.printStackTrace();
            }
        }
    }
    private void response205ToControlBoard(byte[] frame) {
        try {
            //封装数据返回205到主控板
            byte repy[] = PacketUtils.makePackage(frame, new byte[]{0x02, 0x05}, null);
            if (repy != null && repy.length > 0) {
               // LogUtils.d(TAG, "回复205指令类型");
                sendToControlBoard(repy);
            }
        } catch (CRCException e) {
            e.printStackTrace();
        } catch (FrameException e) {
            e.printStackTrace();
        } catch (CmdTypeException e) {
            e.printStackTrace();
        }
    }
    private void response204ToControlBoard(byte[] frame) {
        try {
            //封装数据返回204到主控板
            byte repy[] = PacketUtils.makePackage(frame, new byte[]{0x02, 0x04}, null);
            if (repy != null && repy.length > 0) {
               // LogUtils.d(TAG, "回复204指令类型");
                sendToControlBoard(repy);
            }
        } catch (CRCException e) {
            e.printStackTrace();
        } catch (FrameException e) {
            e.printStackTrace();
        } catch (CmdTypeException e) {
            e.printStackTrace();
        }
    }
    /**
     * 向com1发送操作指令
     *
     * @param cmd 操作指令
     */
    public  void sendToControlBoard(byte[] cmd) {
        if (mOutputStream1 != null && cmd != null) {
            try {
               // LogUtils.d(TAG, "向COM1口发送：" + ByteUtils.byte2hex(cmd));
                mOutputStream1.write(cmd);
                mOutputStream1.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /*--------------------主控制板COM1数据接收发（结束）----------------------------------------------------*/
    /*--------------------通信模块COM2数据接收发（开始）----------------------------------------------------*/
    /**
     * COM2轮询线程任务
     */
    private class ParserCom2ReceivedDataTask implements Runnable {
        @Override
        public void run() {
            if (data2 != null && data2.size() > 3) {
                int size = data2.size();
                if (size >= PACKET_LEN2.get()) {
                    Byte[] data = data2.subList(0, PACKET_LEN2.get()).toArray(new Byte[PACKET_LEN2.get()]);
                    if (data[0] != ConstantUtil.START_) {
                        data2.clear();
                        //data2 = new ArrayList();
                    } else {
                        parseCom2ReceivedTask(data);
                    }
                }
            }
        }
    }
    /**
     * 开启COM2口数据接收线程
     */
    private void startCom2Received() {
        try {
            mSerialPort2 = appLibsContext.getSerialPort2();
            mOutputStream2 = mSerialPort2.getOutputStream();
            mInputStream2 = mSerialPort2.getInputStream();
            //启动接收数据线程,根据isConnect的状态值进行数据读取判断

           scheduledThreadPool.scheduleWithFixedDelay(new ReadCom2DataTask(),100,100,TimeUnit.MILLISECONDS);
        } catch (SecurityException e) {
            LogUtils.d(TAG, getString(R.string.error_security));
        } catch (IOException e) {
            LogUtils.d(TAG, getString(R.string.error_unknown));
        } catch (InvalidParameterException e) {
            LogUtils.d(TAG, getString(R.string.error_configuration));
        }
    }
    private class ReadCom2DataTask implements Runnable {

        @Override
        public void run() {
                if (isConnection) {
                    int size;
                    try {
                        byte[] buffer = new byte[PACKET_LEN2.get()];
                        if (mInputStream2 == null) {
                           // LogUtils.d(TAG, "mInputStream2  is null");
                            return;
                        }
                        size = mInputStream2.read(buffer);
                        if (size > 0) {
                            //过滤掉出现一个字节的0x00或者一个字节的0xff
                            byte[] data = Arrays.copyOfRange(buffer, 0, size);
                            String context = new String(data, "UTF-8").trim().toUpperCase();
                            LogUtils.d(TAG, "COM2接收到" + size + " 字节");
                            LogUtils.d(TAG, "COM2字符串：" + context);
                            LogUtils.d(TAG, "COM2接收数据data==" + ByteUtils.byte2hex(data));
                            LogUtils.d(TAG, "COM2接收状态下：isConnection==" + isConnection);
                            if (context.contains("CLOSED") || context.contains("DISCONNECT")) {
                                LogUtils.d(TAG, "接收到CLOSED或者DISCONNECT，判断是否需要进行断电重启...");
                                handler.sendEmptyMessage(RESTART);
                            }
                            //网络连接成功的情况下进行数据拼接
                            if (isConnection) {
                                if ( data[0] == ConstantUtil.START_) {
                                    data2.clear();
                                    //data2 = new ArrayList<>();
                                    //包头+2字节长度
                                    if (size >= 3) {
                                        int len = NumberUtil.byte2ToUnsignedShort(new byte[]{data[1], data[2]});
                                        //len不包含包头长度，1为包头的长度
                                        PACKET_LEN2.set(len+1);
                                    }
                                }
                                for (int i = 0; i < size; i++) {
                                    data2.add(data[i]);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }

    }
    /**
     * 发送网络模块重启指令
     */
    public void sendNetRestartCmd() {
        //向主控板发送网络模块断电重启
        if (!isAtCommandTaskRunning) {
            isConnection = false;
            //重置计数
            pgkTime.set(0);
            //发送断电重启
            sendToControlBoard(Cmd.ComCmd._RESTART_NET_);
           // LogUtils.d(TAG, "开始启动AT操作指令发送");
            //网络连接流程标记，正在联网中
            isAtCommandTaskRunning = true;
            /*atScheduled = Executors.newSingleThreadScheduledExecutor();
            atScheduled.schedule(new AtCommandTask(), 10, TimeUnit.SECONDS);//延时10s*/
            scheduledThreadPool.schedule(new AtCommandTask(),10,TimeUnit.SECONDS);
        } else {
            LogUtils.d(TAG, "AT操作指令已经正在运行...");
        }
    }

    /**
     * AT指令操作，包含信号检测，透传设置，TCP连接
     */
    private class AtCommandTask implements Runnable {
        @Override
        public void run() {
            //forAtTaskHandler = new ForAtTaskHandler(Looper.myLooper());
            LogUtils.d(TAG, "ForAtTask开始执行");
            //默认未达到可以进行透传指令的设置操作
            boolean isCsq = false;
            //信号检测最大次数
            int csqmax = 5;
            //检测AT信号状态
            for (int i = 1; i <= csqmax; i++) {
                LogUtils.e(TAG, "信号检测：开始第" + (i) + "次信号检测");
                //ForAtTask的执行线程会阻塞到sendAtCmd返回结果
                String aTResult = sendAtCmdToServer(Cmd.AT._CIP_CSQ_.getBytes(), 2 * 1000);//延时2s读取管道数据
                LogUtils.d(TAG, "信号检测返回：" + aTResult);
                if (!TextUtils.isEmpty(aTResult) && aTResult.contains("+CSQ") && aTResult.contains("OK")) {
                    //信号检测的返回 +CSQ
//                    LogUtils.d(TAG, "进入信号检测的返回:" + atcontext);
                    LogUtils.d(TAG, "读取信号强度成功");
                    int csq = StringUtils.getcqs(aTResult);
                    LogUtils.d(TAG, "信号强度==" + csq);
                    //信号强度超过10，进行网络连接
                    if (csq >= 10) {
                        isCsq = true;
                        break;
                    }
                }
                //等待1s继续执行
                SystemClock.sleep(1000);
            }
            if (isCsq) {
                //如果信号检测符合继续进行下一步的条件，则执行设置透传指令发送
                String modeContext = sendAtCmdToServer(Cmd.AT._CIP_MODE_.getBytes(), 1000);
                LogUtils.d(TAG, "透传返回内容：" + modeContext);
                //设置透传成功
                if (!TextUtils.isEmpty(modeContext) && modeContext.contains("OK")) {
                    String tcpContext = sendAtCmdToServer(Cmd.AT._CIP_START_.getBytes(), TCP_MAX_OUT_TIME);
                    LogUtils.d(TAG, "TCP返回内容：" + tcpContext);
                    if (!TextUtils.isEmpty(tcpContext) && tcpContext.contains("CONNECT") && !tcpContext.contains("DIS") && !tcpContext.contains("FAIL")) {
                        //设置网络连接流程结束标记
                        isAtCommandTaskRunning = false;
                        LogUtils.d(TAG, "网络连接成功,启动COM2数据接收服务");
                        //重置计数
                        pgkTime .set(0);
                        isConnection = true;
                    } else {
                        LogUtils.d(TAG, "判断为TCP连接不成功，执断电重启");
                        isAtCommandTaskRunning = false;
                       // atScheduled.shutdown();
                        handler.sendEmptyMessage(RESTART);
                    }
                } else {
                    //透传设置不成功，断电重启
                    LogUtils.d(TAG, csqmax + "透传设置不成功，执断电重启");
                    isAtCommandTaskRunning = false;
                    //atScheduled.shutdown();
                    handler.sendEmptyMessage(RESTART);
                }
            } else {
                LogUtils.d(TAG, csqmax + "次信号检测，均未满足信号大于10，执断电重启");
                isAtCommandTaskRunning = false;
                //atScheduled.shutdown();
                handler.sendEmptyMessage(RESTART);
            }

        }
    }

    /**
     * 网络状态标记字段
     *
     *
     */
    public boolean isConnection() {
        return isConnection;
    }
    public String sendAtCmdToServer(final byte[] aTcmd, final long time) {
                try {
                    if (mOutputStream2 != null && aTcmd != null) {
                        mOutputStream2.write(aTcmd);
                        mOutputStream2.flush();
                    }
                    SystemClock.sleep(time);
                    byte[] buffer = new byte[1024];
                    int size = mInputStream2.read(buffer);
                   // LogUtils.d(TAG, "AT返回数据size==" + size);
                    if (size > 1) {
                        byte[] data = Arrays.copyOfRange(buffer, 0, size);
                        return new String(data, "UTF-8").trim().toUpperCase();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

    /**
     * COM2数据解析
     *
     * @param bf 接收到的数据
     */
    protected void parseCom2ReceivedTask(Byte[] bf) {
        int size = bf.length;
        byte[] buffer= new byte[size];
        for (int i = 0; i < bf.length; i++) {
            buffer[i] = bf[i];
        }
        data2.clear();
        //data2 = new ArrayList<>();
      //  LogUtils.d(TAG, "COM2数据包完整 parseCom2ReceivedTask data==" + ByteUtils.byte2hex(buffer));
        if (size >= PACKET_LEN2.get()) {
            try {
                Packet packet = AnalysisUtils.analysisFrame(buffer, PACKET_LEN2.get());
                if (packet != null) {
                    //判断服务器是否返回205，如果存在则重置计数
                    if (Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x05})) {
                       // LogUtils.e(TAG, "接收到服务器205指令，重置pgkTime计数为0");
                        pgkTime.set(0);
                    }
                    //服务器下发107包时则启动第二功能键
                    if (Arrays.equals(packet.getCmd(), new byte[]{0x01, 0x07})) {
                        try {
                            //封装数据返回207到服务器
                            byte[] frame = packet.getFrame();
                            byte repy[] = PacketUtils.makePackage(frame, new byte[]{0x02, 0x07}, null);
                            if (repy != null && repy.length > 0) {
                              //  LogUtils.d(TAG, "回复207指令类型");
                                sendToServer(repy);
                            }
                        } catch (CRCException e) {
                            e.printStackTrace();
                        } catch (FrameException e) {
                            e.printStackTrace();
                        } catch (CmdTypeException e) {
                            e.printStackTrace();
                        }
                    }//除了107 205 204,com2都直接转发后端的数据给主控板
                    else if(!(Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x05})||
                            Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x04}))){
                            sendToControlBoard(buffer);
                    }
                    Message msg = Message.obtain();
                    msg.obj = packet;
                    msg.what = COM2_SERIAL;
                    handler.sendMessage(msg);
                }
            } catch (AnalysisException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向com2发送操作指令
     *
     * @param cmd 操作指令
     */
    public void sendToServer(byte[] cmd) {
        if (mOutputStream2 != null && cmd != null) {
            try {
               // LogUtils.d(TAG, "向COM2口发送：" + ByteUtils.byte2hex(cmd));
                mOutputStream2.write(cmd);
                mOutputStream2.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*************************************通信模块COM2数据接收发（结束）***********************************/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       if (scheduledThreadPool != null&&!scheduledThreadPool.isShutdown()) {
           scheduledThreadPool.shutdown();
        }
     /*  if (scheduled2 != null&&!scheduled2.isShutdown()) {
           scheduled2.shutdown();
       }*/
    /*   if (atScheduled != null&&!atScheduled.isShutdown()) {
            atScheduled.shutdown();
        }*/
       /* if(countScheduled != null&&!countScheduled.isShutdown()){
            countScheduled.shutdown();
        }*/
      /*  mReadThread1.stopThread();
        mReadThread2.stopThread();*/
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return new LocalBinder();
    }

    public final class LocalBinder extends Binder {
        public PortService getService() {
            return PortService.this;
        }
    }

    /************************************观察者模式通知数据更新（开始）*************************************/
    public class MyObservable extends Observable {
        private Packet com1Packet;
        private Packet com2Packet;


        /**
         * 第二功能按键的启用和停用标记，true表示启用，false表示停止
         */
        private boolean isSKeyEnable;

        public boolean isSKeyEnable() {
            return isSKeyEnable;
        }

        public Packet getCom1Packet() {
            return com1Packet;
        }

        public void setCom1ReceivedPacket(Packet com1Packet) {
            if (com1Packet != null) {
                //重置上一次更新的数据
                this.com2Packet = null;
                this.com1Packet = com1Packet;
                setChanged();
                notifyObservers();
                if (Arrays.equals(com1Packet.getCmd(), new byte[]{0x01, 0x04})) {
                    //判断第二功能按键是否启用
                    try {
                        isSKeyEnable = SpecialUtils.sKeyEnable(com1Packet);
                    } catch (CmdTypeException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public Packet getCom2Packet() {
            return com2Packet;
        }

        public void setCom2ReceivedPacket(Packet com2Packet) {
            if (com2Packet != null) {
                //重置上一次更新的数据
                this.com1Packet = null;
                this.com2Packet = com2Packet;
                setChanged();
                notifyObservers();
                //服务器下发107包时则启动第二功能键
                if (Arrays.equals(com2Packet.getCmd(), new byte[]{0x01, 0x07})) {
                    isSKeyEnable = true;
                }
            }
        }
    }

    /**
     * 添加观察者
     *
     * @param observer mainSerialPortActivity
     */
    public void addObserver(Observer observer) {
        if (mObservable == null) {
            mObservable = new MyObservable();
        }
        mObservable.addObserver(observer);
    }

    /**
     * 移除观察者
     *
     * @param observer mainSerialPortActivity
     */
    public void removeObserver(Observer observer) {
        if (mObservable != null) {
            mObservable.deleteObserver(observer);
        }
    }
    /************************************观察者模式通知数据更新（结束）*************************************/
}
