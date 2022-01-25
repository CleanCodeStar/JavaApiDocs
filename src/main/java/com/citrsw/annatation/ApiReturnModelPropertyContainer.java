package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * ApiModelProperty容器
 *
 * @author 李振峰
 * @date 2020-01-10 19:53:31
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiReturnModelPropertyContainer {
    ApiReturnModelProperty[] value();
}
