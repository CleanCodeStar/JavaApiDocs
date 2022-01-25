package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 请求参数注解
 *
 * @author 李振峰
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiBasicReturn {
    /**
     * 描述
     */
    String description() default "";

    /**
     * 数据格式(一般用于日期)[yyyy-MM-dd HH:mm:ss]
     */
    String format() default "";
}
