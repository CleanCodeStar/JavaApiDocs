package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 模型注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:25:37
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiModel {
    /**
     * 描述
     */
    String value();
}
