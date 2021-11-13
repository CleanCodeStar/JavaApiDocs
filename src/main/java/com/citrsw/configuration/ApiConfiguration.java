package com.citrsw.configuration;

import com.citrsw.interceptor.ApiHandlerInterceptor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置类
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:11:09
 */
@Configuration
@ComponentScan("com.citrsw")
public class ApiConfiguration implements WebMvcConfigurer {

    private final ApiHandlerInterceptor apiHandlerInterceptor;

    public ApiConfiguration(ApiHandlerInterceptor apiHandlerInterceptor) {
        this.apiHandlerInterceptor = apiHandlerInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/citrsw/**").addResourceLocations("classpath:/citrsw/");
    }

    /**
     * 配置拦截器执行顺序
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(apiHandlerInterceptor)
                //需要拦截的uri
                .addPathPatterns("/**")
                //需要跳过的uri
                .excludePathPatterns("/citrsw/**")
                //拦截器的执行顺序 设置高一点方便后期扩展
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
