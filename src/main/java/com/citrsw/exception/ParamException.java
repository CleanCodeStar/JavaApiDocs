package com.citrsw.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Api异常类
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-24 2:33
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ParamException extends RuntimeException {

    private static final long serialVersionUID = 8668329831985671815L;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性类型
     */
    private String type;

    /**
     * 异常消息
     */
    private String message;

    /**
     * 属性描述
     */
    private String description;

    public ParamException(String message) {
        super(message);
        this.message = message;
    }
}
