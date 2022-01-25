package com.citrsw.exception;

import com.citrsw.definition.DocProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Api参数校验异常类
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-24 2:33
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ApiParamException extends RuntimeException {

    private static final long serialVersionUID = 8668329831985671815L;

    /**
     * 属性
     */
    private DocProperty docProperty;

    /**
     * 异常消息内容
     */
    private String message;

    public ApiParamException(String message, DocProperty docProperty) {
        super(message);
        this.message = message;
        this.docProperty = docProperty;
    }
}
