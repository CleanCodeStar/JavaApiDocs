package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 全局状态码
 *
 * @author 李振峰
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//制定容器类的类型
@Repeatable(ApiGlobalCodeContainer.class)
public @interface ApiGlobalCode {
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
