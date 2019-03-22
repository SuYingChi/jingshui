package com.mcloyal.serialport.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.mcloyal.serialport.AppLibsContext;
import com.mcloyal.serialport.R;
import com.mcloyal.serialport.connection.client.ClientConfig;
import com.mcloyal.serialport.connection.client.MinaClient;
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

import static com.mcloyal.serialport.constant.Cmd.IP_ADDRESS;
import static com.mcloyal.serialport.constant.Cmd.PORT;

/**
 * 串口通信常驻后台Service 前端用到的就是包头的开始标志，判断是否是新包，包长度字段，便于开始读取之后的解析，CMD指令类型，执行相应操作。
 *
 * @author hong
 * bindservice启动该后台service ，常驻后台开启子线程做串口的数据收发工作，前台可以拿到实例，通过注册observer触发前台执行对应逻辑（其实直接使用eventbus更简单）
 */
public class PortService extends Service {
    private final static String TAG = PortService.class.getSimpleName();
    private AppLibsContext appLibsContext = null;
    private MinaClient minaClient;
    /**
     * 主控制板为COM1，参数
     */
    protected SerialPort mSerialPort1;
    protected volatile static OutputStream mOutputStream1;
    private volatile static InputStream mInputStream1;
    private ScheduledExecutorService scheduledThreadPool;
    //com1接收到的一个完整的数据包，PACKET_LEN1是完整的包的长度
    private Vector<Byte> data1 = new Vector<Byte>();
    public static AtomicInteger PACKET_LEN1 = new AtomicInteger(100);
    /**
     * 通讯模块为COM2，参数
     */
    protected SerialPort mSerialPort2;
    protected volatile static OutputStream mOutputStream2;
    private volatile static InputStream mInputStream2;
    //com2接收到的一个完整的数据包，PACKET_LEN2是完整的包的长度
    private Vector<Byte> data2 = new Vector<Byte>();
    public static AtomicInteger PACKET_LEN2 = new AtomicInteger(100);
    private MyObservable mObservable;
    /**
     * 是否联网
     */
    private volatile boolean isConnection = false;
    /**
/*     * 是否在联网中
     *//*
    private volatile boolean isAtCommandTaskRunning = false;*/
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
     * 重启服务标记
     */
    private final static int RESTART = 3;
    /**
     * 最大包数
     */
    private final static int MAX_PGK_TIME = 5;
    /**
     * 发送包数据计数
     */
    private static AtomicInteger pgkTime = new AtomicInteger(0);
    //TCP连接返回值等待时间
    private final static int TCP_MAX_OUT_TIME = 10 * 1000;


    private static AtomicInteger count = new AtomicInteger(0);
    private Handler handler = new PortServiceHandler(PortService.this);
    /**
     * 线程间通信Handler，通知关机，触发activity主线程界面更新
     */
    /**
     * 倒计时最大等大时长
     */
    private static final long MAX_COUNT = 5 * 60;

    //后台service里的线程池里边的子线程拿到数据后通过主线程Handler发回主线程后，把数据包给内部的Observable,notify前台注册到后台service 里Observable的observer，
    // 前台observer接收到更新通知后根据自身需求重写update方法触发处理相应逻辑
    private static class PortServiceHandler extends Handler {
        private WeakReference<PortService> weakPortService;

        PortServiceHandler(PortService portService) {
            weakPortService = new WeakReference<PortService>(portService);
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

    private ConnectivityManager connectivityManager;
    private NetworkInfo info;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d("mark", "网络状态已经改变");
                connectivityManager = (ConnectivityManager)

                        getSystemService(Context.CONNECTIVITY_SERVICE);
                info = connectivityManager.getActiveNetworkInfo();
                if(info != null && info.isAvailable()) {
                    String name = info.getTypeName();
                    Log.d("mark", "当前网络名称：" + name);
                    if(!isConnection) {
                        initMinaClient();
                    }
                } else {
                    Log.d("mark", "没有可用网络");
                    if(minaClient!=null) {
                        minaClient.disConnect();
                    }
                }
            }
        }
    };
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
            public Thread newThread(Runnable r) {
                return new Thread(r, Thread.currentThread().getName());
            }
        });
        //开启COM1接收线程  主板和Android板之前数据通信
        startReadCom1Thread();
        //启动COM1数据拼包线程
        scheduledThreadPool.scheduleAtFixedRate(new ParserCom1ReceivedDataTask(), 0, 100,
                TimeUnit.MILLISECONDS);

        //开启COM2接收线程 ,通信模块和Android板之间通信
     //   startCom2Received();
        //启动COM2数据拼包线程
        //scheduleAtFixedRate period指每隔多久执行下一次任务，如果任务执行时间大于周期，会等到执行完毕再执行下一次任务，每隔100毫秒解析一次数据

        //有没有可能读取子线程读取到包，解析子线程还没来的及拿去解析，又再次读取了新的数据包？其实应该把解析操作放在读取之后，在读取到完整数据包之后就新开一个子线程去做解析操作。
        //这种需求要明确读取和解析会耗时多久才好设定线程的时间间隔
        //读取子线程要延迟100MS再开始，解析子线程立即开启解析。每次接收完都要等100MS再次接受，解析子线程每隔100MS解析一次，就是为了避免这种情况。
        //假设读取要50MS，解析也要50MS，在100MS时没有数据解析，到150MS时读取到数据，到200MS时候开始第二次解析，解析的就是读取子线程150秒时的数据。250MS时开始地第三次读取，300MS开始第三次解析，
        //这样保证不会漏解析读取的数据，有必要可以调大读取间隔时间，保证解析子线程来的及解析

    //    scheduledThreadPool.scheduleAtFixedRate(new ParserCom2ReceivedDataTask(), 0, 100, TimeUnit.MILLISECONDS);
        scheduledThreadPool.scheduleAtFixedRate(new CountdownTask(), 0, 1, TimeUnit.SECONDS);
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, mFilter);
    }

    private void initMinaClient() {

        //客户端初始化
        ClientConfig clientConfig = new ClientConfig.Builder().setIp(IP_ADDRESS).setPort(PORT).build();
        //创建minaclient的时候已经启动一个常驻每隔5S的自动重连子线程
        minaClient = new MinaClient(clientConfig);
        minaClient.setClientStateListener(new MinaClient.ClientStateListener() {
            @Override
            public void sessionCreated() {
                Log.d(TAG, "client sessionCreated ");
            }

            @Override
            public void sessionOpened() {
                Log.d(TAG, "client sessionOpened ");
                isConnection = true;
            }

            @Override
            public void sessionClosed() {
                Log.d(TAG, "client sessionClosed ");
                isConnection = false;
            }

            @Override
            public void messageReceived(byte[] message) {
              logByte("接收到后台数据",message);
              onMinaClientReceived(message);
            }

            @Override
            public void messageSent(byte[] message) {
                logByte("发送给后台数据",message);
            }
        });

    }

    private void logByte(String logDesc, byte[] message) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length; i++) {
            if (i == 0 && i != message.length - 1) {
                sb.append("[").append("0x").append(String.format("%02x", message[i] & 0xff)).append(",");
            } else if (i == 0 && i == message.length - 1) {
                sb.append("[").append("0x").append(String.format("%02x", message[i] & 0xff)).append("]");
            } else if (0 < i && i < message.length - 1) {
                sb.append("0x").append(String.format("%02x", message[i] & 0xff)).append(",");
            } else if (0 < i && i == message.length - 1) {
                sb.append("0x").append(String.format("%02x", message[i] & 0xff)).append("]");
            }
        }
        Log.d(TAG, logDesc + sb.toString());
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
            scheduledThreadPool.scheduleWithFixedDelay(new ReadCom1DataTask(), 100, 100, TimeUnit.MILLISECONDS);
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
                if (size > 0) {
                    byte[] data = Arrays.copyOfRange(buffer, 0, size);
                    //有新的数据包
                    if (data[0] == ConstantUtil.START_) {
                        data1.clear();
                        //data1 = new ArrayList<>();
                        //包头+2字节长度
                        if (size >= 3) {
                            int len = NumberUtil.byte2ToUnsignedShort(new byte[]{data[1], data[2]});
                            //len不包含包头长度，1为包头的长度
                            PACKET_LEN1.set(len + 1);
                        }
                    }
                    for (byte aData : data) {
                        data1.add(aData);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onMinaClientReceived(byte[] receivedByte) {
        if (receivedByte.length > 0) {
            String context = null;
            try {
                context = new String(receivedByte, "UTF-8").trim().toUpperCase();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            LogUtils.d(TAG, "接收到" + receivedByte.length + " 字节");
            LogUtils.d(TAG, "接收转码字符串：" + context);
            logByte("接收到后台数据", receivedByte);
            if (context != null && (context.contains("CLOSED") || context.contains("DISCONNECT"))) {
                LogUtils.d(TAG, "接收到CLOSED或者DISCONNECT，判断是否需要进行断电重启...");
                handler.sendEmptyMessage(RESTART);
            }
            //有新的数据包
            if (receivedByte[0] == ConstantUtil.START_) {
                data2.clear();
                if (receivedByte.length >= 3) {
                    int len = NumberUtil.byte2ToUnsignedShort(new byte[]{receivedByte[1], receivedByte[2]});
                    //len不包含包头长度，1为包头的长度
                    PACKET_LEN2.set(len + 1);
                }
            }
            for (byte aData : receivedByte) {
                data2.add(aData);
            }
            if (data2 != null && data2.size() > 3) {
                int size = data2.size();
                //读取包的子线程拿到的数据包，可能还没读取完，并发的解析包子线程就拿去解析，所以判断读取到包长度确实已经大于等于包里长度字段标明的包长度再开始解析。
                if (size >= PACKET_LEN2.get()) {
                    Byte[] data = data2.subList(0, PACKET_LEN2.get()).toArray(new Byte[PACKET_LEN2.get()]);
                    //第一位总是以0X51开始的 代表是正确的数据包，不是的话，则清空不解析数据
                    if (data[0] != ConstantUtil.START_) {
                        data2.clear();
                    } else {
                        scheduledThreadPool.schedule(new ParseMinaClentReceivedByte(data), 0, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    private class ParseMinaClentReceivedByte implements Runnable {
        private final int size;
        private byte[] buffer;

        ParseMinaClentReceivedByte(Byte[] receivedByte) {
            size = receivedByte.length;
            buffer = new byte[size];
            //data2是读取到数据，当读取到一个完整数据包后 ，重置data2，重新读取数据，数组是地址引用，所以拷贝数据到解析子线程里去做解析操作,下次读取新数据不影响此次解析
            for (int i = 0; i < receivedByte.length; i++) {
                buffer[i] = receivedByte[i];
            }
        }

        @Override
        public void run() {
            data2.clear();
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
                        else if (!(Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x05}) ||
                                Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x04}))) {
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
        if (size >= PACKET_LEN1.get()) {
            try {
                Packet packet = AnalysisUtils.analysisFrame(buffer, PACKET_LEN1.get());
                if (packet != null) {
                    //此处无需做CRC校验判断
                    byte[] cmd = packet.getCmd();
                    //不管是否断网都直接转发固定105
                    byte[] frame = packet.getFrame();
                    if (Arrays.equals(cmd, new byte[]{0x01, 0x05})) {
                        // LogUtils.d(TAG, "COM1接收到105，重置倒计时操作");
                        //重置倒计时计时
                        count.set(0);
                        // sendToControlBoard(Cmd.ComCmd._NONET_HEARTBEAT_);
                        response205ToControlBoard(frame);
                        //COM1接收到104 直接回复
                    } else if (Arrays.equals(cmd, new byte[]{0x01, 0x04})) {
                        response204ToControlBoard(frame);
                    }
                    //如果在联网的情况下直接把数据转发到TCP平台
                    if (isConnection) {
                        //  LogUtils.d(TAG, "上送到服务器" + ByteUtils.byte2hex(buffer));
                        //主控板的105指令是连接指令，5次未连接上就断电重启
                        if (Arrays.equals(cmd, new byte[]{0x01, 0x05})) {
                            int incrementAndGet = pgkTime.incrementAndGet();
                            // LogUtils.e(TAG, "数据包发送次数：" + incrementAndGet);
                            //如果超过最大包数判断
                            if (incrementAndGet > MAX_PGK_TIME) {
                                //  LogUtils.d(TAG, "已经超过最大包数阈值，执行断电重启");
                                handler.sendEmptyMessage(RESTART);
                            }
                        }
                        /*判断是否com104,消费类型3,结账数据,为该数据不发后台*/
                        if (AnalysisUtils.isCmd104ConsumptionType3(packet)) {
                            sendToServer(buffer);
                            // minaClient.sendMessage(buffer);
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
    public void sendToControlBoard(byte[] cmd) {
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
                //读取包的子线程拿到的数据包，可能还没读取完，并发的解析包子线程就拿去解析，所以判断读取到包长度确实已经大于等于包里长度字段标明的包长度再开始解析。
                if (size >= PACKET_LEN2.get()) {
                    Byte[] data = data2.subList(0, PACKET_LEN2.get()).toArray(new Byte[PACKET_LEN2.get()]);
                    //第一位总是以0X51开始的 代表是正确的数据包，不是的话，则清空不解析数据
                    if (data[0] != ConstantUtil.START_) {
                        data2.clear();
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
            //scheduleAtFixedDelayed,delay 值得是在一次任务执行完之后隔多久执行下一次任务 ，com2口读取完一次数据后隔100毫秒读取下一次
            scheduledThreadPool.scheduleWithFixedDelay(new ReadCom2DataTask(), 100, 100, TimeUnit.MILLISECONDS);
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
                        return;
                    }
                    size = mInputStream2.read(buffer);
                    if (size > 0) {
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
                            //一次读取有可能会无法完全读取完一个完整的包，当读取到包头是开始0X51开始标志的时候，data2清空重新拼接，并且长度大于等于3，得到第一第二位表示的包长度，
                            //有没有可能本次读取还没拿到长度，没法重置数据包长度全局变量？
                            if (data[0] == ConstantUtil.START_) {
                                data2.clear();
                                if (size >= 3) {
                                    int len = NumberUtil.byte2ToUnsignedShort(new byte[]{data[1], data[2]});
                                    //len不包含包头长度，1为包头的长度
                                    PACKET_LEN2.set(len + 1);
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
      /*  //向主控板发送网络模块断电重启
        if (!isAtCommandTaskRunning) {
            isConnection = false;
            //重置计数
            pgkTime.set(0);
            //发送断电重启
            sendToControlBoard(Cmd.ComCmd._RESTART_NET_);
            //网络连接流程标记，正在联网中
            isAtCommandTaskRunning = true;
            scheduledThreadPool.schedule(new AtCommandTask(), 10, TimeUnit.SECONDS);
        } else {
            LogUtils.d(TAG, "AT操作指令已经正在运行...");
        }*/

      //使用4G后
        isConnection = false;
        //重置计数
        pgkTime.set(0);
        if(minaClient!=null){
            minaClient.disConnect();
        }
        initMinaClient();
        //发送断电重启2G模块
    //    sendToControlBoard(Cmd.ComCmd._RESTART_NET_);
    }

    /**
     * AT指令操作，包含信号检测，透传设置，TCP连接
     */
  /*  private class AtCommandTask implements Runnable {
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
                        pgkTime.set(0);
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
     *           <p>
     *           并发子线程解析接收到的数据包，根据数据包做前端做相应操作是接下来发给后台还是主控板
     */
    protected void parseCom2ReceivedTask(Byte[] bf) {
        int size = bf.length;
        //data2是读取到数据，当读取到一个完整数据包后 ，重置data2，重新读取数据，数组是地址引用，所以拷贝数据到解析子线程里去做解析操作,下次读取新数据不影响此次解析
        byte[] buffer = new byte[size];
        for (int i = 0; i < bf.length; i++) {
            buffer[i] = bf[i];
        }
        data2.clear();
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
                    else if (!(Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x05}) ||
                            Arrays.equals(packet.getCmd(), new byte[]{0x02, 0x04}))) {
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
       /* if (mOutputStream2 != null && cmd != null) {
            try {
                // LogUtils.d(TAG, "向COM2口发送：" + ByteUtils.byte2hex(cmd));
                mOutputStream2.write(cmd);
                mOutputStream2.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
       minaClient.sendMessage(cmd);
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
        if (scheduledThreadPool != null && !scheduledThreadPool.isShutdown()) {
            scheduledThreadPool.shutdown();
        }
        minaClient.disConnect();
        unregisterReceiver(mReceiver);
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
