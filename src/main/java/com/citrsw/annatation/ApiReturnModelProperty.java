package com.citrsw.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 重新指定出参Model属性注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:53:31
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
//制定容器类的类型
@Repeatable(ApiReturnModelPropertyContainer.class)
public @interface ApiReturnModelProperty {
    /**
     * 名称
     * 支持多级配置
     * 例如：user.info.level
     */
    String name();

    /**
     * 描述
     */
    String description();

    /**
     * 示例
     */
    String example() default "";
}
