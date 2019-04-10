package com.mcloyal.serialport.connection.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mcloyal.serialport.connection.ReConnectEvent;
import com.mcloyal.serialport.connection.codeFactory.TestCodecFactory;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;

/**
 * Created by cuihp on 2017/4/15.
 */

public class MinaClient {
    private final static String MESSAGE = "message";
    private  ExecutorService mThreadPool;
    private ClientConfig mConfig;
    private NioSocketConnector mConnection;
    private IoSession mSession;
    private InetSocketAddress mAddress;
    private ConnectRunnale mConnectRunnale;
    private ClientStateListener clientStateListener;
    private Handler mHandler = new MinaClientHandler(this);



    private static class MinaClientHandler extends Handler{
        private WeakReference<MinaClient> weakMinaClient;
        MinaClientHandler(MinaClient minaClient){
            weakMinaClient= new WeakReference<MinaClient>(minaClient);
        }
       @Override
       public void handleMessage(Message msg) {
           switch (msg.arg1) {
               case 1:
                   weakMinaClient.get().clientStateListener.sessionCreated();
                   break;
               case 2:
                   weakMinaClient.get().clientStateListener.sessionOpened();
                   break;
               case 3:
                   weakMinaClient.get().clientStateListener.sessionClosed();
                   break;
               case 4:
                   weakMinaClient.get().clientStateListener.messageReceived(msg.getData().getByteArray(MESSAGE));
                   break;
               case 5:
                   weakMinaClient.get().clientStateListener.messageSent(msg.getData().getByteArray(MESSAGE));
                   break;
                   default:break;

           }
       }
   }
    public void setClientStateListener(ClientStateListener clientStateListener) {
        this.clientStateListener = clientStateListener;
    }

    
    public MinaClient(ClientConfig mConfig) {
        this.mConfig = mConfig;
        mConnectRunnale = new ConnectRunnale();
        mThreadPool = Executors.newFixedThreadPool(1);
        mThreadPool.execute(mConnectRunnale);
    }

    /**
     * 发送消息
     * @param data
     */

    public void sendMessage(byte[] data) {
        mConnectRunnale.sendMsg(data);
    }

    private class ConnectRunnale implements Runnable {


        @Override
        public void run() {
            mAddress = new InetSocketAddress(mConfig.getIp(), mConfig.getPort());
            mConnection = new NioSocketConnector();
            mConnection.getSessionConfig().setReadBufferSize(mConfig.getReadBufferSize());
            mConnection.getFilterChain().addLast("logger", new LoggingFilter());
            mConnection.getFilterChain().addLast(
                    "codec",new ProtocolCodecFilter(new TestCodecFactory())); // 设置编码过滤器*/
            mConnection.setConnectTimeoutMillis(mConfig.getConnectionTimeout());
            mConnection.setHandler(new ClientHandler());
            mConnection.setDefaultRemoteAddress(mAddress);
            reConnect();
        }

        /**
         * 5秒重连 死循环加上sleep实现定时启动子线程任务 ，并可以在满足条件后终止执行
         *
         */
        private void reConnect() {
                boolean bool = false;
                int num = 0;
                while (!bool) {
                    bool = connect();
                    if(bool){
                        num=0;
                    }else {
                        num++;
                        if(num%5==0){
                            bool = true;
                        }else{
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
        }

        /**
         * 发送消息
         *
         * @param data
         */

        public void sendMsg(byte[] data) {
            if (mSession != null && mSession.isConnected()) {
                mSession.write(data);
            }
        }
        /**
         * 链接服务器
         *
         * @return
         */
        public boolean connect() {
            //停止重连
            if (mConnection == null) {
                Log.d(TAG, "断开链接: ");
                return true;
            }
            try {
                ConnectFuture future = mConnection.connect();
                future.awaitUninterruptibly();
                mSession = future.getSession();
                Log.d(TAG, "是否链接: "+mSession == null ?"否":"是");
            } catch (Exception e) {
                Log.d(TAG, "连接异常");
                return false;
            }
            return mSession == null ? false : true;
        }



        private class ClientHandler extends IoHandlerAdapter {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                Log.d(TAG,"client sessionCreated");
                Message message = new Message();
                message.arg1 = 1;
                mHandler.sendMessage(message);
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                Log.d(TAG,"client sessionOpened");
                Message message = new Message();
                message.arg1 = 2;
                mHandler.sendMessage(message);

            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                Log.d(TAG," client sessionClosed");
                //除非调用disconnect而关闭的链接，其他原因引起的关闭的都会自动重连
                reConnect();
                Message message = new Message();
                message.arg1 = 3;
                mHandler.sendMessage(message);

            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                Log.d(TAG,"client messageReceived");
                Message msg = new Message();
                msg.arg1 = 4;
                Bundle bundle = new Bundle();
                if(message instanceof byte[]) {
                    bundle.putByteArray(MESSAGE, (byte[]) message);
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            @Override
            public void messageSent(IoSession session, Object message) throws Exception {
                Log.d(TAG,"client messageSent");
                Message msg = new Message();
                msg.arg1 = 5;
                Bundle bundle = new Bundle();
              //  bundle.putString(MESSAGE, message.toString());
                if(message instanceof byte[]) {
                    bundle.putByteArray(MESSAGE, (byte[]) message);
                }
                msg.setData(bundle);
                mHandler.sendMessage(msg);

            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
                super.sessionIdle(session, status);
                Log.d(TAG,"client  messageSent");
            }
        }

    }
    /**
     * 断开连接
     */
    public void disConnect() {
        if (mConnection!=null){
            mConnection.dispose();
        }
        mConnection = null;
        mAddress = null;
        mSession = null;
    }

    public interface ClientStateListener {
        void sessionCreated();

        void sessionOpened();

        void sessionClosed();

        void messageReceived(byte[] message);

        void messageSent(byte[] message);
    }
}
