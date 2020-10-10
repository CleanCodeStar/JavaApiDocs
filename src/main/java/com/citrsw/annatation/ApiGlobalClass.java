package com.citrsw.annatation;

import com.citrsw.enumeration.TypeEnum;

import java.lang.annotation.*;

/**
 * 全局类配置
 * 凡是遇到此类则按一下配置生成Api
 * 未配置在此处的属性将不会显示
 * 使用场景：使用第三方的实体时使用
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-23 20:42
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//制定容器类的类型
@Repeatable(ApiGlobalClassContainer.class)
public @interface ApiGlobalClass {
    /**
     * 类名
     */
    Class<?> name();

    /**
     * 类描述
     */
    String description() default "";

    /**
     * 类型 入参或出参
     */
    TypeEnum type();

    /**
     * 属性集合
     */
    ApiProperty[] properties();
}
