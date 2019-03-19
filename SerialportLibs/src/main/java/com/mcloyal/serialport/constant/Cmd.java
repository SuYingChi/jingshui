package com.mcloyal.serialport.constant;

/**
 * 服务控制指令
 * Created by rain on 2017/11/2.
 */
public class Cmd {
    private final static String TAG = Cmd.class.getSimpleName();

    /**
     * AT控制指令
     */
    public static class AT {
        public static final String _CSQ_ = "AT+CSQ;+cops?;+creg?" + "\r\n";//查询r是否检测到SIM卡
        public static final String _CPIN_ = "AT+CPIN?" + "\r\n";//查询r是否检测到SIM卡
        //线上地址：sb-server.msbapp.cn:16080
        //测试地址：120.25.195.173,
        //线上环境
       // public static final String _CIP_START_ = "AT+CIPSTART=TCP,sb-server.msbapp.cn,16080" + "\n";//建立TCP(UDP)/IP连接
        //测试环境
        public static final String _CIP_START_ = "AT+CIPSTART=TCP,120.25.195.173,16080" + "\n";//建立TCP(UDP)/IP连接
        //public static final String _CIP_MODE_IFC_ = "AT+IFC=2,2" + "\n";//开启硬件流控功能设置
        public static final String _CIP_MODE_ = "AT+CIPMODE=1\n";//设置为透传模式
        //public static final String _CIP_CSQ_ = "AT+CSQ;+cops?;+creg?\r\n";//指令检测卡信号
        public static final String _CIP_CSQ_ = "AT+CSQ\r\n";//指令检测卡信号

        public static final String _AT_ALL_ = _CIP_MODE_ + _CIP_START_;
        public static final String _CIP_MODE_OUT_ = "+++";//退出透传模式
    }

    public static final int PORT=16080;
    public static final String IP_ADDRESS="120.25.195.173";
   // public static final String IP_ADDRESS="sb-server.msbapp.cn";

    public static class ComCmd {
        //启动灌装09
        public static final byte[] _START_ = new byte[]{0x51, 0x00, 0x3E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x01, 0x04, 0x01, 0x31, 0x58, (byte) 0xE2, 0x02, 0x05, (byte) 0xF5, (byte) 0xDC, (byte) 0xA1, 0x62, 0x0B, 0x1A, 0x00, 0x24, 0x0B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x4c, 0x0a};
        //停止灌装
        public static final byte[] _END_ = new byte[]{0x51, 0x00, 0x3E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x01, 0x04, 0x01, 0x31, 0x58, (byte) 0xE2, 0x02, 0x05, (byte) 0xF5, (byte) 0xDC, (byte) 0xA1, 0x62, 0x0B, 0x1A, 0x00, 0x24, 0x0B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x38, 0x63};
        //主控板发送心跳探测，在android板断网时直接回复如下
        public static final byte[] _NONET_HEARTBEAT_ = new byte[]{0x51, 0x00, 0x0E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x02, 0x05, (byte) 0xd3, (byte) 0xe4};
        //主控板发送104类型指令，在android板断网时直接回复如下
        public static final byte[] _NONET_AT104_ = new byte[]{0x51, 0x00, 0x0E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x02, 0x04, (byte) 0xc2, 0x6d};
        //向主控板发送网络模块断电重启的指令
        public static final byte[] _RESTART_NET_ = new byte[]{0x51, 0x00, 0x3E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x01, 0x04, 0x01, 0x31, 0x58, (byte) 0xE2, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xAD, 0x63};
        /**
         * 4g心跳包105
         * */
        public static final byte[] HEART_BEAT_=new byte[]{  0x51, 0x00, 0x2e, 0x10, 0x00, 0x00, 0x00, 0x02, (byte)0xde, 0x00, 0x00, 0x01, 0x05, 0x00, (byte)0x98, (byte)0x96, (byte)0xc8, 0x00, 0x08, 0x1f, 0x13, 0x21, 0x1e, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,0x00, 0x00, 0x00,0x00,0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xe3, (byte)0xB3 };

        /**
        4g心跳205
         */
        public static final byte[] HEART_BEAT_RESPONSE=new byte[]{0x51, 0x00, 0x0E, 0x10, 0x00, 0x00, 0x00, 0x02, (byte)0xDE, 0x00, 0x00, 0x02, 0x05,  0x09, (byte)0x95  };
        public static final byte[] BEAT_RESPONSE=new byte[]{0x00, 0x0E, 0x10, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x02, 0x05};
    }
}