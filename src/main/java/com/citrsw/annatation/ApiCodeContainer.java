package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 状态码容器
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-27 4:49
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiCodeContainer {
    ApiCode[] value();
}
