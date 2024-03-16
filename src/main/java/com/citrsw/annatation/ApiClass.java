package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Controller注解
 *
 * @author 李振峰
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiClass {
    /**
     * 描述
     */
    String value();
}
