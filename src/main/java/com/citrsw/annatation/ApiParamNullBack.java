package com.citrsw.annatation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 参数校验返回对象
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2021-11-13 16:44
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiParamNullBack {

    /**
     * 是否关闭参数校验，默认false(否)
     */
    boolean close() default false;

    /**
     * 状态码属性名
     */
    String codeFieldName() default "";

    /**
     * 状态码属性值
     */
    String codeFieldValue() default "";

    /**
     * 消息内容属性名
     */
    String msgFieldName() default "";
}
