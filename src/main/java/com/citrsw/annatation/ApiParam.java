package com.citrsw.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 请求参数注解
 *
 * @author 李振峰
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiParam {
    /**
     * 名称
     */
    String name() default "";

    /**
     * 描述
     */
    String description() default "";

    /**
     * 是否必须
     */
    boolean required() default false;

    /**
     * 数据类型[int,long,date,string,double]
     */
    String type() default "";

    /**
     * 数据格式(一般用于日期)[yyyy-MM-dd HH:mm:ss]
     */
    String format() default "";

    /**
     * 默认值
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;

    /**
     * 示例
     */
    String example() default "";
}
