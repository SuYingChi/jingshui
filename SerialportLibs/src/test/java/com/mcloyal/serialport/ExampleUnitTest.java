package com.mcloyal.serialport;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.utils.AnalysisUtils;
import com.mcloyal.serialport.utils.NumberUtil;
import com.mcloyal.serialport.utils.logs.LogUtils;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void ssss() throws Exception {
        byte[] by = new byte[]{0x51, 0x00, 0x2e, 0x10, 0x00, 0x00, 0x00, 0x05, (byte) 0xbc, 0x00, 0x00, 0x01, 0x05, 0x01, 0x31, 0x7f, (byte) 0xf2, 0x00, 0x08, 0x0a, 0x1f, 0x10, 0x1b, 0x00, 0x00, 0x00, 0x00, 0x0b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xeb, (byte) 0xcd};
        Packet packet = AnalysisUtils.analysisFrame(by, by.length);
//        LogUtils.d("ExampleUnitTest", packet.toString());
        System.out.print(packet.toString());
    }

    @Test
    public void s2() throws Exception {
        char[] bits = "112345678".toCharArray();
        if (bits[0] == '0') {
            System.out.print("true");
        } else {
            System.out.print("xxxxxxxxxxx");
        }
    }

    @Test
    public void bigNumber() throws Exception {
        byte[] data = {0x10, 0x10, 0x10, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, (byte) 0x80};
        BigInteger bigNum = new BigInteger(data);
        long intNum = bigNum.longValue();
        System.out.println(intNum);
    }
    @Test
    public void TestMax() throws Exception {
        String context="\nCLOSED";
        System.out.println("context:"+context);
        if (context.contains("CLOSED") || context.contains("DISCONNECT")) {//COM2网络模块主动触发断网消息
            System.out.println("执行");
        }
    }
}