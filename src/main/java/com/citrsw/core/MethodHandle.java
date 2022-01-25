package com.citrsw.core;

import com.citrsw.annatation.*;
import com.citrsw.common.ApiConstant;
import com.citrsw.definition.DocCode;
import com.citrsw.definition.DocMethod;
import com.citrsw.definition.DocModel;
import com.citrsw.definition.TempMethod;
import com.citrsw.exception.ApiParamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * 处理Controller类中的方法
 *
 * @author 李振峰
 * @version 1.0
 * @date 2021-08-04 20:36
 */
@Component
@Slf4j
public class MethodHandle {

    private final ParamHandle handleParam;
    private final ReturnHandle handleReturn;


    public MethodHandle(ParamHandle handleParam, ReturnHandle handleReturn) {
        this.handleParam = handleParam;
        this.handleReturn = handleReturn;
    }

    /**
     * 处理Controller中的方法
     *
     * @param methods        Controller中的方法
     * @param requestMapping Controller上的RequestMapping
     * @return 处理后的方法结果
     */
    public Set<DocMethod> handleMethod(Method[] methods, RequestMapping requestMapping) {
        Set<DocMethod> docMethods = new TreeSet<>();
        Set<String> parentModeSet = new TreeSet<>();
        Set<String> parentUriSet = new TreeSet<>();
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                parentUriSet.addAll(Arrays.asList(requestMapping.value()));
            }
            if (requestMapping.method().length > 0) {
                for (RequestMethod requestMethod : requestMapping.method()) {
                    parentModeSet.add(requestMethod.name());
                }
            }
        }
        if (parentUriSet.isEmpty()) {
            //如果delPicture类上没有配置则增加一个空的地址，从而保证下面的代码正常执行
            parentUriSet.add("");
        }
        for (Method method : methods) {
            Set<String> modeSet = new TreeSet<>(parentModeSet);
            if (method.getAnnotation(ApiIgnore.class) != null) {
                //不处理添加过滤注解的方法
                continue;
            }
            TempMethod tempMethod = new TempMethod();
            //方法上的注解
            ApiMethod methodAnnotation = method.getAnnotation(ApiMethod.class);
            if (methodAnnotation != null && StringUtils.isNotBlank(methodAnnotation.value())) {
                //从注解获取描述
                tempMethod.setDescription(methodAnnotation.value());
            } else {
                //注解获取不到描述则使用方法名称
                tempMethod.setDescription(method.getName());
            }
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
            if (getMapping == null && postMapping == null && putMapping == null && deleteMapping == null && requestMappingAnnotation == null) {
                //这四种注解都不存在则直接跳过，不作处理
                continue;
            }
            Set<String> uriSet = new TreeSet<>();
            //获取请求方式
            if (getMapping != null) {
                modeSet.add(RequestMethod.GET.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : getMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (postMapping != null) {
                modeSet.add(RequestMethod.POST.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : postMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (putMapping != null) {
                modeSet.add(RequestMethod.PUT.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : putMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (deleteMapping != null) {
                modeSet.add(RequestMethod.DELETE.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : deleteMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else {
                //加入到方法结果中
                if (requestMappingAnnotation.method().length > 0) {
                    for (RequestMethod requestMethod : requestMappingAnnotation.method()) {
                        modeSet.add(requestMethod.name());
                    }
                } else {
                    modeSet.add(RequestMethod.GET.name());
                    modeSet.add(RequestMethod.HEAD.name());
                    modeSet.add(RequestMethod.POST.name());
                    modeSet.add(RequestMethod.PUT.name());
                    modeSet.add(RequestMethod.PATCH.name());
                    modeSet.add(RequestMethod.DELETE.name());
                    modeSet.add(RequestMethod.OPTIONS.name());
                    modeSet.add(RequestMethod.TRACE.name());
                }
                for (String parentUri : parentUriSet) {
                    for (String value : requestMappingAnnotation.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            }
            tempMethod.setModeSet(modeSet);
            tempMethod.setUriSet(uriSet);
            //方法上自定义入参的注解(两种)
            ApiMapParam apiMapParam = method.getAnnotation(ApiMapParam.class);
            //指定对象属性的注解
            ApiAppointParam apiAppointParam = method.getAnnotation(ApiAppointParam.class);
            //重新配置属性信息注解
            ApiParamModelProperty[] apiModelProperties = method.getAnnotationsByType(ApiParamModelProperty.class);
            //获取入参 jdk8以上开始支持
            Parameter[] parameters = method.getParameters();
            //处理入参注解
            try {
                DocModel paramDocModel = handleParam.handleParam(apiMapParam, apiAppointParam, apiModelProperties, parameters);
                //入参加入到方法结果中
                tempMethod.setParamDocModel(paramDocModel);
            } catch (ApiParamException e) {
                log.error("方法{}{}", method.getName(), e.getMessage());
                //方法入参异常则跳过，不处理此方法
                continue;
            }
            //方法上自定义出参的注解
            ApiMapReturn apiMapReturn = method.getAnnotation(ApiMapReturn.class);
            //出参为基本数据类型的注解
            ApiBasicReturn apiBasicReturn = method.getAnnotation(ApiBasicReturn.class);
            //重新配置出参属性信息注解
            ApiReturnModelProperty[] apiReturnModelProperties = method.getAnnotationsByType(ApiReturnModelProperty.class);
            //获取出参
            Class<?> returnType = method.getReturnType();
            //无返回值的情况不做处理
            if (!"void".equals(returnType.getName())) {
                Type genericReturnType = method.getGenericReturnType();
                DocModel returnDocModel = handleReturn.handleReturn(apiMapReturn, apiBasicReturn, returnType, genericReturnType, apiReturnModelProperties);
                //出参加入到方法结果中
                tempMethod.setReturnDocModel(returnDocModel);
            }
            //获取method上的状态码配置集合
            Set<DocCode> docCodes = new TreeSet<>();
            ApiCode[] apiCodeAnnotations = method.getAnnotationsByType(ApiCode.class);
            if (apiCodeAnnotations.length > 0) {
                for (ApiCode apiCodeAnnotation : apiCodeAnnotations) {
                    String name = apiCodeAnnotation.name();
                    String value = apiCodeAnnotation.value();
                    String description = apiCodeAnnotation.description();
                    DocCode docCode = new DocCode();
                    docCode.setName(name);
                    docCode.setValue(value);
                    docCode.setDescription(description);
                    docCodes.add(docCode);

                }
            }
            //合并全局状态码
            docCodes.addAll(ApiConstant.DOC_GLOBAL_CODES);
            //状态码设置到apiMethod中
            tempMethod.setDocCodes(docCodes);
            //TempMethod转DocMethod
            DocMethod docMethod = new DocMethod();
            docMethod.setDescription(tempMethod.getDescription());
            docMethod.setParamRequireMap(tempMethod.getParamRequireMap());
            docMethod.setModeSet(tempMethod.getModeSet());
            docMethod.setUriSet(tempMethod.getUriSet());
            docMethod.setParams(tempMethod.getParams());
            docMethod.setParamJson(tempMethod.getParamJson());
            docMethod.setParamExample(tempMethod.getParamExample());
            docMethod.setParamAndroid(tempMethod.getParamAndroid());
            docMethod.setParamIos(tempMethod.getParamIos());
            docMethod.setParamVue(tempMethod.getParamVue());
            docMethod.setReturnJson(tempMethod.getReturnJson());
            docMethod.setReturnAndroid(tempMethod.getReturnAndroid());
            docMethod.setReturnIos(tempMethod.getReturnIos());
            docMethod.setReturnVue(tempMethod.getReturnVue());
            docMethod.setDocCodes(tempMethod.getDocCodes());
            docMethods.add(docMethod);
            for (String uri : uriSet) {
                for (String mode : modeSet) {
                    ApiConstant.methodMap.put(method.getDeclaringClass().getName() + "#" + mode.toUpperCase() + "#" + uri, tempMethod);
                }
            }
        }
        return docMethods;
    }

}
