package com.mcloyal.serialport.connection.client;
/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public interface ClientStateListener {
    void sessionCreated();

    void sessionOpened();

    void sessionClosed();

    void messageReceived(byte[] message);

    void messageSent(byte[] message);
}
