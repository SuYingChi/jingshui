package com.mcloyal.serialport.exception;

/**
 * CMD类型的异常处理
 */

public class CmdTypeException extends Exception {
    public CmdTypeException() {

    }

    public CmdTypeException(String des) {
        super(des);
    }
}
