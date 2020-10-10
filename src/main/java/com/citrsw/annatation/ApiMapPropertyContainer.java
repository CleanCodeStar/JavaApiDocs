package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Map类型属性注解集合容器
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiMapPropertyContainer {
    ApiMapProperty[] value();
}
