package com.citrsw.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 重新指定入参Model属性注解
 *
 * @author 李振峰
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//制定容器类的类型
@Repeatable(ApiParamModelPropertyContainer.class)
public @interface ApiParamModelProperty {
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
