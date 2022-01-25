package com.citrsw.annatation;

import com.citrsw.configuration.ApiConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 激活注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:29:15
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
     * 下划线（蛇形）命名
     *
     * @deprecated 因为在实际开发中“下划线（蛇形）命名”会给前后端联调带来极大不便，故不建议采用此种形式，此属性也将会在未来版本中移除。
     */
    @Deprecated(since = "1.6.2-bate", forRemoval = true)
    boolean underscore() default false;

    /**
     * 环境，不配置则默认所有环境
     */
    String[] actives() default {};

    /**
     * 参数校验
     */
    boolean paramVerification() default true;

}
