package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 过滤注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:53:48
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiIgnore {
}
