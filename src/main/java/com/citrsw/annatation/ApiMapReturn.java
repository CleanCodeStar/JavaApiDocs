package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 自定义参数
 * 使用场景：Map类型的参数
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:02:32
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiMapReturn {
    ApiReturn[] value();
}
