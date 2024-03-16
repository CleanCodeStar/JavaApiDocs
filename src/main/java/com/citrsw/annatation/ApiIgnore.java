package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 过滤注解
 *
 * @author 李振峰
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiIgnore {
}
