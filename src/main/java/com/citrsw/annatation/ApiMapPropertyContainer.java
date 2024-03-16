package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Map类型属性注解集合容器
 *
 * @author 李振峰
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiMapPropertyContainer {
    ApiMapProperty[] value();
}
