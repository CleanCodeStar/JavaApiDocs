package com.citrsw.core;

import com.citrsw.annatation.*;
import com.citrsw.definition.DocModel;
import com.citrsw.definition.DocProperty;
import com.citrsw.exception.ApiParamException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ValueConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 处理方法入参
 *
 * @author 李振峰
 * @version 1.0
 * @date 2021-08-04 20:23
 */
@Component
public class ParamHandle {

    private final ModelHandle handleModel;

    public ParamHandle(ModelHandle handleModel) {
        this.handleModel = handleModel;
    }

    /**
     * 处理方法的入参
     *
     * @param apiMapParam        自定义方法的入参注解
     * @param apiAppointParam    指定对象属性的注解
     * @param apiModelProperties 重新配置入参属性信息注解
     * @param parameters         方法的入参
     * @return 处理后的入参模型
     */
    public DocModel handleParam(ApiMapParam apiMapParam, ApiAppointParam
            apiAppointParam, ApiParamModelProperty[] apiModelProperties, Parameter[] parameters) throws ApiParamException {
        DocModel docModel = new DocModel();
        Map<String, ApiParamModelProperty> apiModelPropertyMap = new HashMap<>(256);
        if (apiModelProperties != null && apiModelProperties.length > 0) {
            for (ApiParamModelProperty apiParamModelProperty : apiModelProperties) {
                String name = apiParamModelProperty.name();
                apiModelPropertyMap.put(name, apiParamModelProperty);
            }
        }
        int num = 0;
        docModel.setForm("form-data");
        //自定义入参对象属性
        Map<String, Boolean> propertyMap = new HashMap<>(256);
        if (apiAppointParam != null) {
            String[] requires = apiAppointParam.require();
            String[] nonRequires = apiAppointParam.nonRequire();
            for (String nonRequire : nonRequires) {
                propertyMap.put(nonRequire, false);
            }
            //如果必须和非必须的同时配置了相同的属性，则必须覆盖非必须
            for (String require : requires) {
                propertyMap.put(require, true);
            }
        }
        for (Parameter parameter : parameters) {
            if (parameter.getAnnotation(ApiIgnore.class) != null) {
                //不处理添加过滤注解的参数
                continue;
            }
            if (parameter.getType() == HttpSession.class || parameter.getType() == HttpServletRequest.class
                    || parameter.getType() == HttpServletResponse.class) {
                //不处理这些类
                continue;
            }
            //对于map类型的入参 用ApiMapParam 来处理
            Map<String, ApiParam> apiMapParamMap = new HashMap<>(256);
            if (apiMapParam != null && apiMapParam.value().length > 0) {
                for (ApiParam apiParam : apiMapParam.value()) {
                    String value = apiParam.name();
                    apiMapParamMap.put(value, apiParam);
                }
            }
            //循环依赖收集集合
            Set<Class<?>> repeats = new HashSet<>();
            //json形式入参的标识RequestBody
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            Validated validated = parameter.getAnnotation(Validated.class);
            DocProperty docProperty = new DocProperty();
            if (requestBody != null) {
                if (num > 0) {
                    //入参中RequestBody的数量不能超过1
                    throw new ApiParamException("入参中@RequestBody的数量超过1", null);
                }
                num++;
                //如果是json,那么肯定不是基本数据类型，直接调用handleModel()
                docProperty = handleModel.handleModel(validated,docProperty, parameter.getType(), parameter.getParameterizedType(), propertyMap, true, true, repeats, apiModelPropertyMap, null, null, apiMapParamMap, null, null, null);
                if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
                    return docModel;
                }
                //借用apiProperty为壳返回apiModel
                docModel = docProperty.getDocModel();
                //如果docModel为空标识不是对象则直接创建一个新的
                if (docModel == null) {
                    //只能是json字符串
                    docModel = new DocModel();
                    docModel.setForm("json");
                    docModel.setType(docProperty.getType());
                } else {
                    //标记为非基本数据类型
                    docModel.setForm("json");
                    docModel.setType(docProperty.getType());
                    //类名
                    docModel.setClassName(docProperty.getClassName());
                    //类描述
                    docModel.setDescription(docProperty.getDocModel() != null ? docProperty.getDocModel().getDescription() : "json字符串");
                }

            }
            if (num < 1) {
                //非json则为form-data形式入参
                docProperty = handleModel.handleModel(validated, docProperty, parameter.getType(), parameter.getParameterizedType(), propertyMap, true, false, repeats, apiModelPropertyMap, null, null, apiMapParamMap, null, null, null);
                if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
                    return docModel;
                }
                //借用apiProperty为壳返回apiModel
                if (docProperty.getDocModel() == null) {
                    //为空表示为基本数据类型
                    //从注解中获取配置信息
                    ApiParam parameterAnnotation = parameter.getAnnotation(ApiParam.class);
                    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                    if (pathVariable != null) {
                        String value = pathVariable.value();
                        if (StringUtils.isNotBlank(value)) {
                            docProperty.setName(value);
                        } else {
                            docProperty.setName(parameter.getName());
                        }
                        docProperty.setRequited(true);
                    }
                    if (parameterAnnotation != null) {
                        String value = parameterAnnotation.name();
                        //PathVariable设置名称的优先级最高
                        if (StringUtils.isBlank(docProperty.getName())) {
                            if (StringUtils.isNotBlank(value)) {
                                docProperty.setName(value);
                            } else {
                                docProperty.setName(parameter.getName());
                            }
                        }
                        String description = parameterAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            docProperty.setDescription(description);
                        }
                        String defaultValue = parameterAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            docProperty.setDefaultValue(defaultValue);
                        }
                        boolean required = parameterAnnotation.required();
                        if (docProperty.getRequited() == null) {
                            docProperty.setRequited(required);
                        }
                        String example = parameterAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            docProperty.setExample(example);
                        }
                    } else {
                        //未配置则使用参数名称
                        docProperty.setName(parameter.getName());
                    }
                    docModel.getApiProperties().add(docProperty);
                    //标记为基本数据类型
                    docModel.setType(docProperty.getType());
                } else {
                    //非基本数据类型则合并参数
                    docModel.getApiProperties().addAll(docProperty.getDocModel().getApiProperties());
                    //标记为非基本数据类型
                    docModel.setType(docProperty.getType());
                    //类名
                    docModel.setClassName(docProperty.getDocModel().getClassName());
                    //类描述
                    docModel.setDescription(docProperty.getDocModel().getDescription());
                }
            }
        }
        return docModel;
    }
}
