package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 状态码容器
 *
 * @author 李振峰
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiCodeContainer {
    ApiCode[] value();
}
