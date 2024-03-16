package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 自定义参数
 * 使用场景：Map类型的参数
 *
 * @author 李振峰
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiMapParam {
    ApiParam[] value();
}
