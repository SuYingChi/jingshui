package com.mcloyal.serialport.exception;

/**
 * 解析错误的异常处理
 */

public class AnalysisException extends Exception {
    public AnalysisException() {

    }

    public AnalysisException(String des) {
        super(des);
    }
}
