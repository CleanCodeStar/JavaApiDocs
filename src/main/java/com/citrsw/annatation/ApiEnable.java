package com.citrsw.annatation;

import com.citrsw.configuration.ApiConfiguration;
import com.citrsw.enums.ApiParamHandle;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 激活注解
 *
 * @author 李振峰
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

    /**
     * 校验结果处理方式
     * 默认处理返回示例:
     * {
     * code: 400,
     * msg:"参数[roomName]（直播间的名称）为空"
     * }
     */
    ApiParamHandle paramHandle() default ApiParamHandle.DEFAULT;

    /**
     * 打印请求参数
     */
    boolean paramOutput() default true;

}
