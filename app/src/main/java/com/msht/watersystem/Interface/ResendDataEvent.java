package com.msht.watersystem.Interface;

import com.msht.watersystem.entity.OrderInfo;

import java.util.List;

/**
 * Created by hong on 2018/4/13.
 */

/**
 * Demo class
 * 〈一句话功能简述〉
 * 〈功能详细描述〉
 * @author hong
 * @date 2018/7/2  
 */
public interface ResendDataEvent {
    /**
     *  数据重发
     * @param orderData  订单数据
     */
    void onHaveDataChange(List<OrderInfo> orderData);
    /**
     * 为某段时间时执行
     * @param status  时间标志
     */
   // void onControlScreenBack(int status);
}
