package com.mcloyal.serialport.utils;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CmdTypeException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by rain on 2017/11/21.
 * 特例变量
 */
public class SpecialUtils {
    private static ArrayList<byte[]> cmds = new ArrayList<>();

    /**
     * @param types
     */
    public static void addTypes(ArrayList<byte[]> types) {
        if (cmds == null) {
            cmds = new ArrayList<>();
        }
        cmds.clear();
        cmds = types;
    }

    /**
     * 判断是否为特例数据类型
     *
     * @param cmd
     * @return
     */
    public static boolean isSpecial(byte[] cmd) throws CmdTypeException {
        if (cmd == null || cmd.length != 2) {
            throw new CmdTypeException("数据类型为空或者长度错误");
        }
        boolean isSpecial = false;
        if (cmds != null && cmds.size() > 0) {
            for (byte[] type : cmds) {
                if (Arrays.equals(type, cmd)) {//存在特例数据
                    isSpecial = true;
                    break;
                }
            }
        }
        return isSpecial;
    }

    /**
     * 判断第二功能按键是否启用
     *
     * @param packet
     * @return
     * @throws CmdTypeException
     */
    public static boolean sKeyEnable(Packet packet) throws CmdTypeException {
        boolean skeyEnable = false;
        ArrayList<Byte> data = packet.getData();
//                    收到刷卡104数据包后，消费后余额为0(相对地址21和22为0,相对地址44的bit5=0，bit6=0)，则启动按键第二功能；
//                    收到刷卡104数据包，消费后余额非0(相对地址21和22不为0,相对地址44的bit5=1或者bit6=1)，则停止按键第二功能。
        if (data.size() > 45) {
            byte b21 = data.get(21).byteValue();
            byte b22 = data.get(22).byteValue();
            byte b44 = data.get(44).byteValue();
            char[] bits = BitsUtils.strReverse(BitsUtils.byteToBit(b44)).toCharArray();
            if (b21 == 0x00 && b22 == 0x00 && bits[5] == '0' && bits[6] == '0') {
                skeyEnable = true;
            } else {
                skeyEnable = false;
            }
        }
        return skeyEnable;
    }
}
