package com.citrsw.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Model属性注解
 *
 * @author 李振峰
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProperty {
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
     * 默认值
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;

    /**
     * 示例
     */
    String example() default "";
}
