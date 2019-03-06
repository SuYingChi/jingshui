package com.mcloyal.serialport.exception;

/**
 * CRC校验的异常处理
 */

public class CRCException extends Exception {
    public CRCException() {

    }

    public CRCException(String des) {
        super(des);
    }
}
