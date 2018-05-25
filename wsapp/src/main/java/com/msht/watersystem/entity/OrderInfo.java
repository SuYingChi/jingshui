package com.msht.watersystem.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hong on 2018/1/26.
 */
@Entity
public class OrderInfo {
   @Id(autoincrement = true)
   private Long id;
   private byte[] orderData;
   public byte[] getOrderData() {
      return this.orderData;
   }
   public void setOrderData(byte[] orderData) {
      this.orderData = orderData;
   }
   public Long getId() {
      return this.id;
   }
   public void setId(Long id) {
      this.id = id;
   }
   @Generated(hash = 436302447)
   public OrderInfo(Long id, byte[] orderData) {
      this.id = id;
      this.orderData = orderData;
   }
   @Generated(hash = 1695813404)
   public OrderInfo() {
   }
}
