package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * ApiReturnModelProperty容器
 *
 * @author 李振峰
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiParamModelPropertyContainer {
    ApiParamModelProperty[] value();
}
