package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 方法注解
 *
 * @author 李振峰
 * @version 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiMethod {

    /**
     * 描述
     */
    String value();
}
