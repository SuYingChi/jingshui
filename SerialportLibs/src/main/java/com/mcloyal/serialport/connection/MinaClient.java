package com.mcloyal.serialport.connection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mcloyal.serialport.utils.ByteUtils;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.content.ContentValues.TAG;

/**
 *
 * @author cuihp
 * @date 2017/4/15
 */

public class MinaClient {
    private final static String MESSAGE = "message";
    private ClientConfig mConfig;
    private NioSocketConnector mConnection;
    private IoSession mSession;
    private InetSocketAddress mAddress;
    private ConnectThread mConnectThread;
    private ClientStateListener clientStateListener;
    private ScheduledExecutorService scheduledThreadPools;
   // private ExecutorService mThreadPool = Executors.newFixedThreadPool(1);
    private MinaHandler minaHandler=new MinaHandler(this);
    private static class MinaHandler extends Handler{
        private final WeakReference<MinaClient> minaClientWeakReference;
        public MinaHandler(MinaClient minaClient) {
            minaClientWeakReference = new WeakReference<MinaClient>(minaClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1) {
                case 1:
                    minaClientWeakReference.get().clientStateListener.sessionCreated();
                    break;
                case 2:
                    minaClientWeakReference.get().clientStateListener.sessionOpened();
                    break;
                case 3:
                    minaClientWeakReference.get().clientStateListener.sessionClosed();
                    break;
                case 4:
                    minaClientWeakReference.get().clientStateListener.messageReceived(msg.getData().getByteArray(MESSAGE));
                    break;
                case 5:
                    minaClientWeakReference.get().clientStateListener.messageSent(msg.getData().getByteArray(MESSAGE));
                    break;
                default:
                    break;

            }
        }
    }

    public void setClientStateListener(ClientStateListener clientStateListener) {
        this.clientStateListener = clientStateListener;
    }

    
    public MinaClient(ClientConfig mConfig, ScheduledExecutorService scheduledThreadPool) {
        this.mConfig = mConfig;
        mConnectThread = new ConnectThread();
       // mThreadPool.execute(mConnectThread);
        scheduledThreadPool.execute(mConnectThread);
    }

    /**
     * 发送消息
     * @param data
     */
    public void sendMessage(byte[] data) {
        mConnectThread.sendMsg(data);
    }

    private class ConnectThread implements Runnable {

        @Override
        public void run() {
            mAddress = new InetSocketAddress(mConfig.getIp(), mConfig.getPort());
            mConnection = new NioSocketConnector();
            mConnection.getSessionConfig().setReadBufferSize(mConfig.getReadBufferSize());
            mConnection.getFilterChain().addLast("logger", new LoggingFilter());
            mConnection.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
            mConnection.setConnectTimeoutMillis(mConfig.getConnectionTimeout());
            mConnection.setHandler(new ClientHandler());
            mConnection.setDefaultRemoteAddress(mAddress);
            reConnect();
        }

        /**
         * 5秒重连
         */
        private void reConnect() {
            boolean bool = false;
            while (!bool) {
                bool = connect();
                Log.d(TAG, "是否链接:" + bool);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                return true;
            }
            try {
                ConnectFuture future = mConnection.connect();
                future.awaitUninterruptibly();
                mSession = future.getSession();
            } catch (Exception e) {
                return false;
            }
           // return mSession == null ? false : true;
            return mSession != null ;
        }

        private class ClientHandler extends IoHandlerAdapter {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                Message message = new Message();
                message.arg1 = 1;
                minaHandler.sendMessage(message);
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                Message message = new Message();
                message.arg1 = 2;
                minaHandler.sendMessage(message);

            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                reConnect();
                Message message = new Message();
                message.arg1 = 3;
                minaHandler.sendMessage(message);

            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {

                Log.d(TAG, "client messageReceived2 ");
                Message msg = new Message();
                msg.arg1 = 4;
                Bundle bundle = new Bundle();
                if(message instanceof byte[]) {
                    bundle.putByteArray(MESSAGE, (byte[]) message);
                }
                msg.setData(bundle);
                minaHandler.sendMessage(msg);
            }

            @Override
            public void messageSent(IoSession session, Object message) throws Exception {
                Message msg = new Message();
                msg.arg1 = 5;
                Bundle bundle = new Bundle();
                if(message instanceof byte[]) {
                    bundle.putByteArray(MESSAGE, (byte[]) message);
                }
                msg.setData(bundle);
                minaHandler.sendMessage(msg);

            }
        }

    }
    /**
     * 断开连接
     */
    public void disConnect() {
        mConnection.dispose();
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
