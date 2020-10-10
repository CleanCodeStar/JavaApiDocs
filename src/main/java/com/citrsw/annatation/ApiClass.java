package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Controller注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:25:37
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
