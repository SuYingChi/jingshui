package com.msht.watersystem.Manager;

import com.msht.watersystem.entity.OrderInfo;

import java.util.List;

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/8/21  
 */
public class DateMassageEvent {
    private int message;
    public DateMassageEvent(int message) {
        this.message = message;
    }
    public int getMessage() {
        return message;
    }
    public void setMessage(int message) {
        this.message = message;
    }
}
