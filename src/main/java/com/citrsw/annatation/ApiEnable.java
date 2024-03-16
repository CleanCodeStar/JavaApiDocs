package com.citrsw.annatation;

import com.citrsw.configuration.ApiConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 激活注解
 *
 * @author 李振峰
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ApiConfiguration.class})
public @interface ApiEnable {

    /**
     * 项目名称
     */
    String name();

    /**
     * 全局并配置不需要生成api的类
     */
    Class<?>[] excludedClasses() default {};

    /**
     * 环境，不配置则默认所有环境
     */
    String[] actives() default {};

    /**
     * header中token的名称
     */
    String tokenName() default "authorization";

}
