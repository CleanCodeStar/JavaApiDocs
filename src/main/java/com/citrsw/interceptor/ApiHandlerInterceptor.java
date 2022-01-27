package com.citrsw.interceptor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.citrsw.common.ApiConstant;
import com.citrsw.definition.DocProperty;
import com.citrsw.definition.TempMethod;
import com.citrsw.enums.ApiParamHandle;
import com.citrsw.exception.ApiParamException;
import com.citrsw.filter.ApiParameterRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.thymeleaf.util.MapUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Api拦截器
 *
 * @author 李振峰
 */
@Component
@Slf4j
public class ApiHandlerInterceptor implements HandlerInterceptor {

    /**
     * 当前的项目名
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 当前的项目名
     */
    @Value("${spring.mvc.servlet.path:}")
    private String servletPath;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @Nullable Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        try {
            boolean paramVerification = ApiConstant.paramVerification;
            ApiParamHandle paramHandle = ApiConstant.paramHandle;
            if (!paramVerification) {
                return true;
            }
            String uri = request.getRequestURI();
            String path = (contextPath + servletPath).replace("//", "/");
            if (StringUtils.isNotBlank(path) && !"/".equals(path)) {
                uri = uri.replace(path, "");
            }
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String methodKey = handlerMethod.getBeanType().getName() + "#" + request.getMethod().toUpperCase() + "#" + uri;
            Map<String, TempMethod> methodMap = ApiConstant.methodMap;
            if (MapUtils.isEmpty(methodMap)) {
                return true;
            }
            Map<String, Boolean> returnMap = new HashMap<>(125);
            if (!methodMap.containsKey(methodKey)) {
                methodKey = matchingMethod(methodKey, returnMap);
                if (StringUtils.isBlank(methodKey)) {
                    return true;
                }
            }
            //获取当前请求方法的必传参数集合
            TempMethod tempMethod = methodMap.get(methodKey);
            Map<String, DocProperty> paramRequireMap = tempMethod.getParamRequireMap();
            if (paramRequireMap.size() == 0) {
                return true;
            }
            if (request instanceof ApiParameterRequestWrapper requestWrapper) {
                String body = requestWrapper.getBody();
                //获取body入参
                try {
                    JSONObject jsonObject = JSONObject.parseObject(body);
                    returnMap.putAll(convertJsonParam(jsonObject, null));
                } catch (JSONException ignored) {
                }
            }
            //获取param入参
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                if (key.contains(".")) {
                    String[] names = key.split("\\.");
                    StringBuilder nameBuilder = new StringBuilder(names[0]);
                    returnMap.put(nameBuilder.toString().split("\\[")[0], true);
                    for (int i = 1; i < names.length - 1; i++) {
                        nameBuilder.append(".").append(names[i]);
                        returnMap.put(nameBuilder.toString().split("\\[")[0], true);
                    }
                }
                if (entry.getValue() == null || entry.getValue().length == 0) {
                    returnMap.put(key.split("\\[")[0], false);
                } else {
                    returnMap.put(key.split("\\[")[0], true);
                    for (String value : entry.getValue()) {
                        if (StringUtils.isBlank(value)) {
                            returnMap.put(key.split("\\[")[0], false);
                            break;
                        }
                    }
                }
            }
            //校验参数是否必传
            for (Map.Entry<String, DocProperty> entry : paramRequireMap.entrySet()) {
                DocProperty docProperty = entry.getValue();
                if (!returnMap.containsKey(entry.getKey()) || !returnMap.get(entry.getKey())) {
                    log.error("参数[{}]（{}）为空  ===>>> {}#{}#[{}]#{}",
                            entry.getKey(),
                            docProperty.getDescription(),
                            handlerMethod.getBeanType().getName(),
                            handlerMethod.getMethod().getName(),
                            request.getMethod().toUpperCase(),
                            uri
                    );
                    if (paramHandle.equals(ApiParamHandle.EXCEPTION)) {
                        //参数校验不通过则抛出异常，由开发人员自行处理
                        throw new ApiParamException(String.format("参数[%s]（%s）为空", entry.getKey(), docProperty.getDescription()), docProperty);
                    } else if (paramHandle.equals(ApiParamHandle.DEFAULT)) {
                        //参数校验不通过则抛出异常，使用默认返回形式
                        JSONObject jsonObject = new JSONObject();
                        //向返回码中赋值
                        jsonObject.put("code", 300);
                        //向返回消息内容中赋值
                        jsonObject.put("msg", String.format("参数[%s]（%s）为空", entry.getKey(), docProperty.getDescription()));
                        //重置response
                        response.reset();
                        //设置编码格式
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(jsonObject.toJSONString());
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof ApiParamException) {
                throw e;
            } else {
                log.error("参数校验发生异常，异常信息：{}", e.getMessage());
            }
        }
        //校验出错时，为了不影响正常业务流程，这里选择继续放行
        return true;
    }

    /**
     * 将body中的json进行转换
     */
    private Map<String, Boolean> convertJsonParam(Object object, String superName) {
        if (StringUtils.isBlank(superName)) {
            superName = "";
        }
        Map<String, Boolean> returnMap = new HashMap<>(125);
        if (object instanceof JSONArray) {
            for (Object o : (JSONArray) object) {
                returnMap.putAll(convertJsonParam(o, superName));
            }
        } else if (object instanceof JSONObject jsonObject) {
            if (StringUtils.isNotBlank(superName)) {
                superName = superName + ".";
            }
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                if (entry.getValue() instanceof JSONObject || entry.getValue() instanceof JSONArray) {
                    returnMap.putAll(convertJsonParam(entry.getValue(), superName + entry.getKey()));
                } else {
                    if (entry.getValue() != null && StringUtils.isNotBlank(String.valueOf(entry.getValue()))) {
                        returnMap.put(superName + entry.getKey(), true);
                    } else {
                        returnMap.put(superName + entry.getKey(), false);
                    }
                }
            }

        }
        return returnMap;
    }

    /**
     * 匹配请求对应的处理方法
     */
    private String matchingMethod(String methodKey, Map<String, Boolean> returnMap) {
        String[] methodKeys = methodKey.split("/");
        for (String key : ApiConstant.methodMap.keySet()) {
            String[] keys = key.split("/");
            if (methodKeys.length == keys.length) {
                boolean pass = false;
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i].contains("{")) {
                        if (StringUtils.isNotBlank(methodKeys[i])) {
                            //将路由中的参数提取出来
                            returnMap.put(keys[i].replaceAll("\\{", "").replaceAll("\\}", ""), Boolean.TRUE);
                        }
                        pass = true;
                    } else if (keys[i].equals(methodKeys[i])) {
                        pass = true;
                    } else {
                        break;
                    }
                }
                if (pass) {
                    return key;
                }
            }
        }
        return null;
    }
}
