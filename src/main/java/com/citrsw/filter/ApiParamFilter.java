package com.citrsw.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;

/**
 * Json过滤器
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2021-09-22 15:38
 */
@Slf4j
@Component
public class ApiParamFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ApiParameterRequestWrapper parameterRequestWrapper = null;
        if (request instanceof HttpServletRequest) {
            String header = ((HttpServletRequest) request).getHeader("Content-Type");
            if (StringUtils.isNotBlank(header) && header.toLowerCase().contains("application/json")) {
                parameterRequestWrapper = new ApiParameterRequestWrapper((HttpServletRequest) request);
            }
        }
        chain.doFilter((Objects.isNull(parameterRequestWrapper) ? request : parameterRequestWrapper), response);
    }
}