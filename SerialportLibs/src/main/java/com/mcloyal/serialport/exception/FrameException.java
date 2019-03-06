package com.mcloyal.serialport.exception;

/**
 * 数据帧序的异常处理
 */

public class FrameException extends Exception {
    public FrameException() {

    }

    public FrameException(String des) {
        super(des);
    }
}
