package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 模型注解
 *
 * @author 李振峰
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
