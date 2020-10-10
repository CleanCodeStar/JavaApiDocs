package com.citrsw.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/citrsw/**").addResourceLocations("classpath:/citrsw/");
    }
}
