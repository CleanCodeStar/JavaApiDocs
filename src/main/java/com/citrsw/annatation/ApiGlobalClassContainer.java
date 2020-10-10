package com.citrsw.annatation;

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
public @interface ApiGlobalClassContainer {
    ApiGlobalClass[] value();
}
