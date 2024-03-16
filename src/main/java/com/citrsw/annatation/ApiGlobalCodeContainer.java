package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 全局状态码容器
 *
 * @author 李振峰
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiGlobalCodeContainer {
    ApiGlobalCode[] value();
}
