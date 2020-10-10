package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 状态码
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-27 4:46
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//制定容器类的类型
@Repeatable(ApiCodeContainer.class)
public @interface ApiCode {
    /**
     * 名称
     */
    String name();

    /**
     * 值
     */
    String value();

    /**
     * 描述
     */
    String description();
}
