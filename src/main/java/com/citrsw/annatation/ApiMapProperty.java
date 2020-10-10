package com.citrsw.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * Map类型属性注解集合
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//制定容器类的类型
@Repeatable(ApiMapPropertyContainer.class)
public @interface ApiMapProperty {
    /**
     * 名称
     * 支持多级配置
     * 例如：user.info.level
     */
    String name();


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
