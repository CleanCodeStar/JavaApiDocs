package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 返回参数注解
 *
 * @author 李振峰
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiReturn {
    /**
     * 名称
     */
    String name();

    /**
     * 描述
     */
    String description() default "";


    /**
     * 数据类型[int,long,datetime,date,time,string,double,[]]
     */
    String type() default "";

    /**
     * 数据格式(一般用于日期)[yyyy-MM-dd HH:mm:ss]
     */
    String format() default "";
}
