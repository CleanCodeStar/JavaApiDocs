package com.citrsw.exception;

/**
 * 写点注释
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-24 2:33
 */
public class ParamException extends RuntimeException {

    private static final long serialVersionUID = 8668329831985671815L;

    public ParamException() {
    }

    public ParamException(String message) {
        super(message);
    }

    public ParamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParamException(Throwable cause) {
        super(cause);
    }

    public ParamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
