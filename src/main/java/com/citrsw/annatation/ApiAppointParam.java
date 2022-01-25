package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 自定义入参字段
 * 使用场景：不需要类中全部的属性
 *
 * @author 李振峰
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiAppointParam {

    /**
     * 必须属性
     * 表示必须要传值的属性
     * 支持多级配置
     * 例如：user.info.level
     */
    String[] require() default {};

    /**
     * 非必须属性
     * 表示非必须传值的属性
     * 支持多级配置
     * 例如：user.info.level
     */
    String[] nonRequire() default {};
}
