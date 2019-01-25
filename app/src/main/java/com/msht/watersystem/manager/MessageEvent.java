package com.msht.watersystem.manager;

import com.msht.watersystem.entity.OrderInfo;

import java.util.List;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/21  
 */
public class MessageEvent {
    private List<OrderInfo> messageData;
    public MessageEvent(List<OrderInfo> orderData) {
        this.messageData = orderData;
    }
    public List<OrderInfo> getMessage() {
        return messageData;
    }
    public void setMessage(List<OrderInfo> message) {
        this.messageData = message;
    }
}
