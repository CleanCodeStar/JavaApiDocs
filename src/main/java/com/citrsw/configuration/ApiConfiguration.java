package com.citrsw.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置类
 *
 * @author 李振峰
 */
@Configuration
@ComponentScan("com.citrsw")
@RequiredArgsConstructor
public class ApiConfiguration implements WebMvcConfigurer {

//    private final ApiHandlerInterceptor apiHandlerInterceptor;
//    private final ApiParamFilter apiParamFilter;

//    @Bean
//    public FilterRegistrationBean<ApiParamFilter> servletRegistrationBean() {
//        FilterRegistrationBean<ApiParamFilter> bean = new FilterRegistrationBean<>();
//        bean.setFilter(apiParamFilter);
//        bean.setName(ApiParamFilter.class.getName());
//        bean.addUrlPatterns("/*");
//        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        return bean;
//    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/citrsw/**").addResourceLocations("classpath:/citrsw/");
    }

//    /**
//     * 配置拦截器执行顺序
//     */
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(apiHandlerInterceptor)
//                .addPathPatterns("/**")
//                .excludePathPatterns("/citrsw/**")
//                .order(Ordered.HIGHEST_PRECEDENCE);
//    }
}
