package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * ApiReturnModelProperty容器
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:53:31
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiParamModelPropertyContainer {
    ApiParamModelProperty[] value();
}
