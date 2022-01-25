package com.citrsw.core;

import com.citrsw.annatation.*;
import com.citrsw.common.ApiConstant;
import com.citrsw.common.ApiUtils;
import com.citrsw.common.ParameterizedTypeImpl;
import com.citrsw.definition.DocModel;
import com.citrsw.definition.DocProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理模型类
 *
 * @author 李振峰
 * @version 1.0
 * @date 2021-08-04 20:44
 */
@Component
@Slf4j
public class ModelHandle {

    /**
     * 全局日期格式化
     */
    @Value("${spring.mvc.date-format:}")
    private String dateFormat;

    /**
     * 全局日期格式化
     */
    @Value("${spring.jackson.date-format:}")
    private String jsonFormat;
    /**
     * 正则
     */
    private final Pattern pattern = Pattern.compile("[A-Z]");

    /**
     * 处理模型
     *
     * @param docProperty                属性类
     * @param aClass                     类
     * @param type                       类
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param repeats                    循环依赖集合
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          Map类型属性注解集合
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型出参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleModel(DocProperty docProperty,
                                   Class<?> aClass, Type type, Map<String, Boolean> propertyMap,
                                   boolean isParam, boolean isJson, Set<Class<?>> repeats,
                                   Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                   Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                   Map<String, ApiMapProperty> apiMapPropertyMap,
                                   Map<String, ApiParam> apiMapParamMap,
                                   Map<String, ApiReturn> apiMapReturnMap,
                                   Map<String, ApiProperty> paramGlobalApiPropertyMap,
                                   Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        //先判断是否为循环依赖
        if (repeats.contains(aClass)) {
            //如为循环依赖则直接返回
            return docProperty;
        }
        if (Map.class.isAssignableFrom(aClass)) {
            //处理Map
            return handleMap(docProperty, type, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (Collection.class.isAssignableFrom(aClass)) {
            //处理集合
            docProperty.setType(docProperty.getType() + "[0]");
            return handleGeneric(docProperty, type, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (aClass.isArray()) {
            //处理数组
            docProperty.setType(docProperty.getType() + "[0]");
            return handleGeneric(docProperty, aClass.getComponentType(), propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (aClass.isEnum()) {
            //处理枚举
            Object[] enums = aClass.getEnumConstants();
            Field[] fields = aClass.getDeclaredFields();
            StringBuilder fieldBuilder = new StringBuilder();
            StringBuilder nameBuilder = new StringBuilder();
            for (Object object : enums) {
                nameBuilder.append(object.toString());
                for (Field field : fields) {
                    if (field.isAnnotationPresent(JsonValue.class)) {
                        field.setAccessible(true);
                        try {
                            fieldBuilder.append(field.get(object)).append(",");
                        } catch (IllegalAccessException ignored) {
                        }
                        break;
                    }
                }
            }
            if (fieldBuilder.length() == 0) {
                fieldBuilder = nameBuilder;
            }
            fieldBuilder.insert(0, "取值范围:[");
            fieldBuilder.deleteCharAt(fieldBuilder.length() - 1);
            fieldBuilder.append("]");
            String typeString = docProperty.getType();
            docProperty.setFormat(fieldBuilder.toString());
            docProperty.setType("enum" + typeString);
            docProperty.setClassName(aClass.getSimpleName());
            return docProperty;
        } else if (int.class.isAssignableFrom(aClass)
                || long.class.isAssignableFrom(aClass)
                || double.class.isAssignableFrom(aClass)
                || float.class.isAssignableFrom(aClass)
                || short.class.isAssignableFrom(aClass)
                || boolean.class.isAssignableFrom(aClass)
                || aClass.getPackage().getName().startsWith("java.lang")
                || Date.class.isAssignableFrom(aClass)
                || LocalDateTime.class.isAssignableFrom(aClass)
                || BigDecimal.class.isAssignableFrom(aClass)
                || LocalDate.class.isAssignableFrom(aClass)
                || MultipartFile.class.isAssignableFrom(aClass)
                || LocalTime.class.isAssignableFrom(aClass)) {
            //处理基本数据类型
            String typeString = docProperty.getType();
            docProperty.setType(ApiConstant.baseTypeMap.get(aClass) + typeString);
            docProperty.setClassName(aClass.getSimpleName());
            return docProperty;
        } else {
            DocModel docModel = new DocModel();
            ApiModel apiModel = aClass.getAnnotation(ApiModel.class);
            //如果属性为对象，但是属性未配置注解描述的，取类上的注解为属性描述
            if (StringUtils.isBlank(docProperty.getDescription())) {
                if (apiModel != null) {
                    String value = apiModel.value();
                    if (StringUtils.isNotBlank(value)) {
                        docProperty.setDescription(value);
                    }
                }
            }
            if (apiModel != null && StringUtils.isNotBlank(apiModel.value())) {
                docModel.setDescription(apiModel.value());
            }
            docModel.setClassName(aClass.getSimpleName());
            docProperty.setClassName(aClass.getSimpleName());
            Set<DocProperty> apiProperties = new TreeSet<>();
            //处理类类型
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                ApiIgnore apiIgnore = method.getAnnotation(ApiIgnore.class);
                if (apiIgnore != null) {
                    //不处理添加过滤注解的属性
                    continue;
                }
                String methodName = method.getName();
                if ("getDeclaringClass".equals(methodName)) {
                    //是getDeclaringClass()方法直接跳过
                    continue;
                }
                if ("getClass".equals(methodName)) {
                    //是getClass()方法直接跳过
                    continue;
                }
                if (!methodName.startsWith("set") && !methodName.startsWith("get") && !methodName.startsWith("is")) {
                    //不是get/set/is形式的方法直接跳过
                    continue;
                }
                if (isParam) {
                    if (methodName.startsWith("set")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("set", ""));
                    } else {
                        continue;
                    }
                } else {
                    if (methodName.startsWith("get")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("get", ""));
                    } else if (methodName.startsWith("is")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("is", ""));
                    } else {
                        continue;
                    }
                }
                DocProperty property = new DocProperty();
                property.setClassName(method.getReturnType().getSimpleName());
                Map<String, Boolean> childPropertyMap = new HashMap<>(256);
                if (!propertyMap.isEmpty()) {
                    Set<String> strings = propertyMap.keySet();
                    //通过状态
                    boolean pass = false;
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childPropertyMap.put(string.replaceFirst(methodName + ".", ""), propertyMap.get(string));
                                pass = true;
                            }
                        } else if (string.equals(methodName)) {
                            pass = true;
                        }
                    }
                    if (!pass) {
                        continue;
                    }
                    property.setRequited(propertyMap.get(methodName));
                }
                //全局针对多级配置
                Map<String, ApiProperty> childParamGlobalApiPropertyMap = new HashMap<>(256);
                if (paramGlobalApiPropertyMap != null && !paramGlobalApiPropertyMap.isEmpty()) {
                    Set<String> strings = paramGlobalApiPropertyMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childParamGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), paramGlobalApiPropertyMap.get(string));
                            } else {
                                childParamGlobalApiPropertyMap.put(string, paramGlobalApiPropertyMap.get(string));
                            }
                        } else if (string.equals(methodName)) {
                            ApiProperty apiPropertyConf = paramGlobalApiPropertyMap.get(methodName);
                            String description = apiPropertyConf.description();
                            if (StringUtils.isNotBlank(description)) {
                                property.setDescription(description);
                            }
                            String defaultValue = apiPropertyConf.defaultValue();
                            if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                property.setDefaultValue(defaultValue);
                            }
                            String example = apiPropertyConf.example();
                            if (StringUtils.isNotBlank(example)) {
                                property.setExample(example);
                            }
                            boolean required = apiPropertyConf.required();
                            if (property.getRequited() == null) {
                                property.setRequited(required);
                            }
                        }
                    }
                }
                //全局针对多级配置
                Map<String, ApiProperty> childReturnGlobalApiPropertyMap = new HashMap<>(256);
                if (returnGlobalApiPropertyMap != null && !returnGlobalApiPropertyMap.isEmpty()) {
                    Set<String> strings = returnGlobalApiPropertyMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childReturnGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), returnGlobalApiPropertyMap.get(string));
                            } else {
                                childReturnGlobalApiPropertyMap.put(string, returnGlobalApiPropertyMap.get(string));
                            }
                        } else if (string.equals(methodName)) {
                            ApiProperty apiPropertyConf = returnGlobalApiPropertyMap.get(methodName);
                            String description = apiPropertyConf.description();
                            if (StringUtils.isNotBlank(description)) {
                                property.setDescription(description);
                            }
                            String defaultValue = apiPropertyConf.defaultValue();
                            if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                property.setDefaultValue(defaultValue);
                            }
                            String example = apiPropertyConf.example();
                            if (StringUtils.isNotBlank(example)) {
                                property.setExample(example);
                            }
                            boolean required = apiPropertyConf.required();
                            if (property.getRequited() == null) {
                                property.setRequited(required);
                            }
                        }
                    }
                }
                //判断是否为全局类配置
                if (isParam) {
                    //入参
                    if (ApiConstant.PARAM_GLOBAL_CLASS_MAP.containsKey(aClass)) {
                        Map<String, ApiProperty> apiPropertyMap = ApiConstant.PARAM_GLOBAL_CLASS_MAP.get(aClass);
                        Set<String> strings = apiPropertyMap.keySet();
                        //是否包含
                        boolean contain = false;
                        for (String string : strings) {
                            if (string.contains(".")) {
                                if (string.startsWith(methodName + ".")) {
                                    childParamGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiPropertyMap.get(string));
                                    contain = true;
                                }
                            } else if (string.equals(methodName)) {
                                contain = true;
                                ApiProperty apiPropertyConf = apiPropertyMap.get(methodName);
                                String description = apiPropertyConf.description();
                                if (StringUtils.isNotBlank(description)) {
                                    property.setDescription(description);
                                }
                                String defaultValue = apiPropertyConf.defaultValue();
                                if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                    property.setDefaultValue(defaultValue);
                                }
                                String example = apiPropertyConf.example();
                                if (StringUtils.isNotBlank(example)) {
                                    property.setExample(example);
                                }
                                boolean required = apiPropertyConf.required();
                                if (property.getRequited() == null) {
                                    property.setRequited(required);
                                }
                            }
                        }
                        if (!contain) {
                            continue;
                        }
                        String value = ApiConstant.PARAM_GLOBAL_CLASS_DESCRIPTION_MAP.get(aClass);
                        if (StringUtils.isNotBlank(value)) {
                            docModel.setDescription(value);
                            docProperty.setDescription(value);
                        }
                    }
                } else {
                    //出参
                    if (ApiConstant.RETURN_GLOBAL_CLASS_MAP.containsKey(aClass)) {
                        Map<String, ApiProperty> apiPropertyMap = ApiConstant.RETURN_GLOBAL_CLASS_MAP.get(aClass);
                        Set<String> strings = apiPropertyMap.keySet();
                        //是否包含
                        boolean contain = false;
                        for (String string : strings) {
                            if (string.contains(".")) {
                                if (string.startsWith(methodName + ".")) {
                                    childReturnGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiPropertyMap.get(string));
                                    contain = true;
                                }
                            } else if (string.equals(methodName)) {
                                contain = true;
                                ApiProperty apiPropertyConf = apiPropertyMap.get(methodName);
                                String description = apiPropertyConf.description();
                                if (StringUtils.isNotBlank(description)) {
                                    property.setDescription(description);
                                }
                                String defaultValue = apiPropertyConf.defaultValue();
                                if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                    property.setDefaultValue(defaultValue);
                                }
                                String example = apiPropertyConf.example();
                                if (StringUtils.isNotBlank(example)) {
                                    property.setExample(example);
                                }
                                boolean required = apiPropertyConf.required();
                                if (property.getRequited() == null) {
                                    property.setRequited(required);
                                }
                            }
                        }
                        if (!contain) {
                            continue;
                        }
                        String value = ApiConstant.RETURN_GLOBAL_CLASS_DESCRIPTION_MAP.get(aClass);
                        if (StringUtils.isNotBlank(value)) {
                            docModel.setDescription(value);
                            docProperty.setDescription(value);
                        }
                    }
                }
                //Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap
                //复制入参Map类型注解信息
                Map<String, ApiParam> childApiMapParamMap = null;
                if (apiMapParamMap != null && !apiMapParamMap.isEmpty()) {
                    childApiMapParamMap = new HashMap<>(256);
                    Set<String> strings = apiMapParamMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childApiMapParamMap.put(string.replaceFirst(methodName + ".", ""), apiMapParamMap.get(string));
                            }
                        }
                    }
                }
                //复制出参Map类型注解信息
                Map<String, ApiReturn> childApiMapReturnMap = null;
                if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty()) {
                    childApiMapReturnMap = new HashMap<>(256);
                    Set<String> strings = apiMapReturnMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childApiMapReturnMap.put(string.replaceFirst(methodName + ".", ""), apiMapReturnMap.get(string));
                            }
                        }
                    }
                }
                //对于map类型的属性
                //方法上的Map注解
                apiMapPropertyMap = new HashMap<>(256);
                ApiMapProperty[] apiMapProperties = method.getAnnotationsByType(ApiMapProperty.class);
                if (apiMapProperties != null && apiMapProperties.length > 0) {
                    for (ApiMapProperty apiMapProperty : apiMapProperties) {
                        String value = apiMapProperty.name();
                        apiMapPropertyMap.put(value, apiMapProperty);
                    }
                }
                //方法上的注解
                ApiProperty apiPropertyAnnotation = method.getAnnotation(ApiProperty.class);
                //format-data形式的时间格式化注解
                DateTimeFormat dateTimeFormat = method.getAnnotation(DateTimeFormat.class);
                //json形式的时间格式化注解
                JsonFormat jsonFormat = method.getAnnotation(JsonFormat.class);
                //jsonProperty注解
                JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
                if (ApiConstant.underscore) {
                    //如果启用下划线命名，则转换为下划线命名
                    property.setName(humpToLine(methodName));
                } else {
                    property.setName(methodName);
                }
                try {
                    Field field = getParentField(aClass, methodName);
                    if (field == null) {
                        throw new NoSuchFieldException();
                    }
                    apiIgnore = field.getAnnotation(ApiIgnore.class);
                    if (apiIgnore != null) {
                        //不处理添加过滤注解的属性
                        continue;
                    }
                    if (apiMapPropertyMap.isEmpty()) {
                        //对于map类型的属性
                        //属性上的Map注解
                        apiMapProperties = field.getAnnotationsByType(ApiMapProperty.class);
                        if (apiMapProperties != null && apiMapProperties.length > 0) {
                            for (ApiMapProperty apiMapProperty : apiMapProperties) {
                                String value = apiMapProperty.name();
                                apiMapPropertyMap.put(value, apiMapProperty);
                            }
                        }
                    }
                    if (apiPropertyAnnotation == null) {
                        //属性上的注解
                        apiPropertyAnnotation = field.getAnnotation(ApiProperty.class);
                    }
                    //format-data形式的时间格式化注解
                    if (dateTimeFormat == null) {
                        //属性上的注解
                        dateTimeFormat = field.getAnnotation(DateTimeFormat.class);
                    }
                    //json形式的时间格式化注解
                    if (jsonFormat == null) {
                        //属性上的注解
                        jsonFormat = field.getAnnotation(JsonFormat.class);
                    }
                    //get/set方法上的注解优先级高
                    if (apiPropertyAnnotation != null) {
                        String value = apiPropertyAnnotation.name();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                        String description = apiPropertyAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            property.setDescription(description);
                        }
                        String defaultValue = apiPropertyAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            property.setDefaultValue(defaultValue);
                        }
                        if (propertyMap.isEmpty()) {
                            boolean required = apiPropertyAnnotation.required();
                            property.setRequited(required);
                        }
                        String example = apiPropertyAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            property.setExample(example);
                        }
                    }
                    //jsonProperty注解
                    if (jsonProperty == null) {
                        jsonProperty = method.getAnnotation(JsonProperty.class);
                    }
                    if (jsonProperty != null) {
                        String value = jsonProperty.value();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                    }
                    if (isParam && method.getName().startsWith("set")) {
                        Parameter[] parameters = method.getParameters();
                        if (!StringUtils.equals(field.getGenericType().getTypeName(), parameters[0].getParameterizedType().getTypeName())) {
                            //在set方法中如果入参类型和属性类型不一致，则不进行处理
                            continue;
                        }
                        //复制重新定义属性信息Map
                        Map<String, ApiParamModelProperty> childApiModelPropertyMap = null;
                        if (apiModelPropertyMap != null && !apiModelPropertyMap.isEmpty()) {
                            childApiModelPropertyMap = new HashMap<>(256);
                            Set<String> strings = apiModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiParamModelProperty modelProperty = apiModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                    boolean required = modelProperty.required();
                                    property.setRequited(required);
                                    String defaultValue = modelProperty.defaultValue();
                                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                        property.setDefaultValue(defaultValue);
                                    }
                                    String example = modelProperty.example();
                                    if (StringUtils.isNotBlank(example)) {
                                        property.setExample(example);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        DocProperty returnDocProperty;
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Class<?> paramClass = parameters[0].getType();
                            Type realType = realType(parameters[0].getParameterizedType(), aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                paramClass = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                paramClass = (Class<?>) realType;
                            }

                            returnDocProperty = handleModel(property, paramClass, realType, childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            returnDocProperty = handleModel(property, parameters[0].getType(), parameters[0].getParameterizedType(), childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (returnDocProperty.getType().contains("date") || returnDocProperty.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (!isJson && dateTimeFormat != null) {
                                //form-data形式的时间格式
                                String pattern = dateTimeFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (isJson) {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    returnDocProperty.setFormat(this.jsonFormat);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(dateFormat)) {
                                    returnDocProperty.setFormat(dateFormat);
                                }
                            }
                        }
                        apiProperties.add(returnDocProperty);
                    } else if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                        Class<?> returnType = method.getReturnType();
                        Type genericReturnType = method.getGenericReturnType();
                        if (!StringUtils.equals(field.getGenericType().getTypeName(), genericReturnType.getTypeName())) {
                            //在get方法中如果返回参数类型和属性类型不一致，则不进行处理
                            continue;
                        }
                        //复制重新定义属性信息Map
                        Map<String, ApiReturnModelProperty> childApiReturnModelPropertyMap = null;
                        if (apiReturnModelPropertyMap != null && !apiReturnModelPropertyMap.isEmpty()) {
                            childApiReturnModelPropertyMap = new HashMap<>(256);
                            Set<String> strings = apiReturnModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiReturnModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiReturnModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiReturnModelProperty modelProperty = apiReturnModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Type realType = realType(genericReturnType, aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                returnType = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                returnType = (Class<?>) realType;
                            }
                            property = handleModel(property, returnType, realType, childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            property = handleModel(property, returnType, ApiUtils.regenerateType(genericReturnType), childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (property.getType().contains("date") || property.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    property.setFormat(pattern);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    property.setFormat(this.jsonFormat);
                                }
                            }
                        }
                        apiProperties.add(property);
                    }
                } catch (NoSuchFieldException exception) {
                    //get/set方法上的注解优先级高
                    if (apiPropertyAnnotation != null) {
                        String value = apiPropertyAnnotation.name();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                        String description = apiPropertyAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            property.setDescription(description);
                        }
                        String defaultValue = apiPropertyAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            property.setDefaultValue(defaultValue);
                        }
                        if (propertyMap.isEmpty()) {
                            boolean required = apiPropertyAnnotation.required();
                            property.setRequited(required);
                        }
                        String example = apiPropertyAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            property.setExample(example);
                        }
                    }
                    if (jsonProperty != null) {
                        String value = jsonProperty.value();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                    }
                    if (isParam && method.getName().startsWith("set")) {
                        Parameter[] parameters = method.getParameters();
                        //复制重新定义属性信息Map
                        Map<String, ApiParamModelProperty> childApiModelPropertyMap = null;
                        if (apiModelPropertyMap != null && !apiModelPropertyMap.isEmpty()) {
                            childApiModelPropertyMap = new HashMap<>(256);
                            Set<String> strings = apiModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiParamModelProperty modelProperty = apiModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                    boolean required = modelProperty.required();
                                    property.setRequited(required);
                                    String defaultValue = modelProperty.defaultValue();
                                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                        property.setDefaultValue(defaultValue);
                                    }
                                    String example = modelProperty.example();
                                    if (StringUtils.isNotBlank(example)) {
                                        property.setExample(example);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        DocProperty returnDocProperty;
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Class<?> paramClass = parameters[0].getType();
                            Type realType = realType(parameters[0].getParameterizedType(), aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                paramClass = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                paramClass = (Class<?>) realType;
                            }
                            returnDocProperty = handleModel(property, paramClass, realType, childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            returnDocProperty = handleModel(property, parameters[0].getType(), parameters[0].getParameterizedType(), childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (returnDocProperty.getType().contains("date") || returnDocProperty.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (!isJson && dateTimeFormat != null) {
                                //form-data形式的时间格式
                                String pattern = dateTimeFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (isJson) {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    returnDocProperty.setFormat(this.jsonFormat);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(dateFormat)) {
                                    returnDocProperty.setFormat(dateFormat);
                                }
                            }
                        }
                        apiProperties.add(returnDocProperty);
                    } else if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                        Class<?> returnType = method.getReturnType();
                        Type genericReturnType = method.getGenericReturnType();
                        //复制重新定义属性信息Map
                        Map<String, ApiReturnModelProperty> childApiReturnModelPropertyMap = null;
                        if (apiReturnModelPropertyMap != null && !apiReturnModelPropertyMap.isEmpty()) {
                            childApiReturnModelPropertyMap = new HashMap<>(256);
                            Set<String> strings = apiReturnModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiReturnModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiReturnModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiReturnModelProperty modelProperty = apiReturnModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Type realType = realType(genericReturnType, aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                returnType = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                returnType = (Class<?>) realType;
                            }
                            property = handleModel(property, returnType, realType, childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            property = handleModel(property, returnType, ApiUtils.regenerateType(genericReturnType), childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (property.getType().contains("date") || property.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    property.setFormat(pattern);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    property.setFormat(this.jsonFormat);
                                }
                            }
                        }
                        apiProperties.add(property);
                    }
                }
            }
            docModel.setApiProperties(apiProperties);
            docProperty.setDocModel(docModel);
            //解析完成后存入到模型集合中
            //指定对象属性的除外，泛型的也除外,重新指定对象属性信息的也除外
//            if (!ApiUtils.PARAM_GLOBAL_CLASS_MAP.containsKey(aClass) && !ApiUtils.RETURN_GLOBAL_CLASS_MAP.containsKey(aClass) && apiModelPropertyMap != null && apiReturnModelPropertyMap != null && propertyMap.isEmpty() && !(type != null && aClass.getTypeParameters().length > 0)) {
//                apiModelMap.put(aClass, docModel);
//            }
            return docProperty;
        }
    }


    /**
     * 获取父类中的属性
     *
     * @param clazz     当前类
     * @param fieldName 属性名
     * @return 获取到到属性
     */
    public Field getParentField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        try {
            try {
                //首字符小写先进行获取获取不到用首字母大写在进行获取
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                //首字符小写先进行获取获取不到用首字母大写在进行获取
                return clazz.getDeclaredField(StringUtils.capitalize(fieldName));
            }
        } catch (NoSuchFieldException exception) {
            return getParentField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * 对包含泛型的属性类型进行重新包装
     *
     * @param type           属性类型
     * @param typeParameters 当前类的泛型集合
     * @param genericType    当前类的真实返回类型
     * @return 包装后的真实类型
     */
    public Type realType(Type type, TypeVariable<? extends Class<?>>[] typeParameters, Type genericType) {
        if (type instanceof ParameterizedType pType) {
            // 强制类型转换
            Type rawType = pType.getRawType();
            List<Type> types = new ArrayList<>();
            for (Type actualTypeArgument : pType.getActualTypeArguments()) {
                types.add(realType(actualTypeArgument, typeParameters, genericType));
            }
            return ParameterizedTypeImpl.make((Class<?>) rawType, types.toArray(new Type[0]), null);
        } else {
            for (int i = 0; i < typeParameters.length; i++) {
                if (type.getTypeName().equals(typeParameters[i].getName())) {
                    return ((ParameterizedType) genericType).getActualTypeArguments()[i];
                }
            }
            return type;
        }
    }

    /**
     * 处理泛型
     *
     * @param docProperty                属性类
     * @param gType                      类型
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param repeats                    循环依赖集合
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          Map类型属性注解集合
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型出参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleGeneric(DocProperty docProperty, Type gType,
                                     Map<String, Boolean> propertyMap, boolean isParam, boolean isJson,
                                     Set<Class<?>> repeats, Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                     Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                     Map<String, ApiMapProperty> apiMapPropertyMap,
                                     Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap, Map<String, ApiProperty> paramGlobalApiPropertyMap, Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        // 如果gType类型是ParameterizedType对象
        if (gType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) gType;
            // 取得泛型类型的泛型参数
            Type[] tArgs = pType.getActualTypeArguments();
            Type tArg = tArgs[0];
            if (tArg instanceof Class && ((Class<?>) tArg).isArray()) {
                //处理数组
                docProperty.setType(docProperty.getType() + "[0]");
                return handleGeneric(docProperty, ((Class<?>) tArg).getComponentType(), propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            } else if (tArg instanceof ParameterizedType && Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) tArg).getRawType())) {
                //处理集合
                docProperty.setType(docProperty.getType() + "[0]");
                return handleGeneric(docProperty, tArg, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            } else {
                if (!(tArg instanceof Class) && !(tArg instanceof ParameterizedType)) {
                    //泛型不为类也不是基本数据类型的不作处理
                    return docProperty;
                }
                //处理类
                Class<?> aClass = (tArg instanceof Class) ? (Class<?>) tArg : (Class<?>) ((ParameterizedType) tArg).getRawType();
                return handleModel(docProperty, aClass, tArg, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            }
        } else {
            return handleModel(docProperty, (Class<?>) gType, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        }
    }


    /**
     * 处理Map
     *
     * @param docProperty                属性类
     * @param gType                      类型
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          类属性为Map的注解
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型入参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleMap(DocProperty docProperty,
                                 Type gType, Map<String, Boolean> propertyMap, boolean isParam, boolean isJson,
                                 Set<Class<?>> repeats, Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                 Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                 Map<String, ApiMapProperty> apiMapPropertyMap,
                                 Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap, Map<String, ApiProperty> paramGlobalApiPropertyMap, Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        DocModel docModel = new DocModel();
        docModel.setClassName("Map");
        docModel.setDescription(docProperty.getDescription());
        Set<DocProperty> apiProperties = new TreeSet<>();
        Class<?> keyClass = Object.class;
        if (gType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) gType;
            // 取得泛型类型的泛型参数
            Type[] tArgs = pType.getActualTypeArguments();
            if (tArgs.length == 0) {
                return docProperty;
            }
            if (tArgs[1] instanceof ParameterizedType) {
                //处理类上的泛型
                keyClass = (Class<?>) ((ParameterizedType) tArgs[1]).getRawType();
            } else {
                keyClass = (Class<?>) tArgs[1];
            }
        }
        if ((apiMapPropertyMap == null || apiMapPropertyMap.isEmpty()) && (apiMapParamMap == null || apiMapParamMap.isEmpty()) && (apiMapReturnMap == null || apiMapReturnMap.isEmpty())) {
            return handleModel(docProperty, keyClass, keyClass, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        }
        //全局针对多级配置
        Map<String, ApiProperty> childParamGlobalApiPropertyMap = new HashMap<>(256);
        if (paramGlobalApiPropertyMap != null && !paramGlobalApiPropertyMap.isEmpty()) {
            Set<String> strings = paramGlobalApiPropertyMap.keySet();
            for (String string : strings) {
                if (string.contains(".")) {
                    String[] split = string.split("\\.");
                    childParamGlobalApiPropertyMap.put(string.replaceFirst(split[0] + ".", ""), paramGlobalApiPropertyMap.get(string));

                }
            }
        }
        //全局针对多级配置
        Map<String, ApiProperty> childReturnGlobalApiPropertyMap = new HashMap<>(256);
        if (returnGlobalApiPropertyMap != null && !returnGlobalApiPropertyMap.isEmpty()) {
            Set<String> strings = returnGlobalApiPropertyMap.keySet();
            for (String string : strings) {
                if (string.contains(".")) {
                    String[] split = string.split("\\.");
                    childReturnGlobalApiPropertyMap.put(string.replaceFirst(split[0] + ".", ""), returnGlobalApiPropertyMap.get(string));
                }
            }
        }

        Set<String> fieldKeySet = new HashSet<>();
        Map<String, ApiMapProperty> copyApiMapPropertyMap = new HashMap<>(256);
        if (apiMapPropertyMap != null && !apiMapPropertyMap.isEmpty()) {
            copyApiMapPropertyMap.putAll(apiMapPropertyMap);
            //属性Map的name配置
            fieldKeySet = apiMapPropertyMap.keySet();
        }

        if (apiMapParamMap != null && !apiMapParamMap.isEmpty() && isParam) {
            //入参Map
            Set<String> keySet = apiMapParamMap.keySet();
            Map<String, Map<String, ApiParam>> mapMap = new HashMap<>(256);
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                for (String fieldKey : fieldKeySet) {
                    //ApiParam配置了则MapModelProperty失效
                    if (split[0].equals(key.split("\\.")[0])) {
                        copyApiMapPropertyMap.remove(fieldKey);
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiParam> childApiMapParamMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapParamMap = mapMap.get(split[0]);
                        childApiMapParamMap.put(key.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(key));
                    } else {
                        childApiMapParamMap = new HashMap<>(256);
                        childApiMapParamMap.put(key.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(key));
                        mapMap.put(split[0], childApiMapParamMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiParam apiParam = apiMapParamMap.get(key);
                    String description = apiParam.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String defaultValue = apiParam.defaultValue();
                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                        property.setDefaultValue(defaultValue);
                    }
                    String type = apiParam.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiParam.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    String example = apiParam.example();
                    if (StringUtils.isNotBlank(example)) {
                        property.setExample(example);
                    }
                    boolean required = apiParam.required();
                    property.setRequited(required);
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, keyClass, gType, propertyMap, true, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, mapMap.get(property.getName()), apiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    if (StringUtils.isNotBlank(type) && (StringUtils.isBlank(property.getClassName()) || "Object".equals(property.getClassName()))) {
                        property.setClassName(ApiConstant.typeReverseMap.get(property.getType()));
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, keyClass, gType, propertyMap, true, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, mapMap.get(key), apiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        } else if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty() && !isParam) {
            //出参Map
            Set<String> keySet = apiMapReturnMap.keySet();
            Map<String, Map<String, ApiReturn>> mapMap = new HashMap<>(256);
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                for (String fieldKey : fieldKeySet) {
                    //ApiParam配置了则MapModelProperty失效
                    if (split[0].equals(key.split("\\.")[0])) {
                        copyApiMapPropertyMap.remove(fieldKey);
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiReturn> childApiMapReturnMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapReturnMap = mapMap.get(split[0]);
                        childApiMapReturnMap.put(key.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(key));
                    } else {
                        childApiMapReturnMap = new HashMap<>(256);
                        childApiMapReturnMap.put(key.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(key));
                        mapMap.put(split[0], childApiMapReturnMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiReturn apiReturn = apiMapReturnMap.get(key);
                    String description = apiReturn.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String type = apiReturn.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiReturn.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, keyClass, gType, propertyMap, false, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, copyApiMapPropertyMap, apiMapParamMap, mapMap.get(property.getName()), childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    if (StringUtils.isNotBlank(type) && (StringUtils.isBlank(property.getClassName()) || "Object".equals(property.getClassName()))) {
                        property.setClassName(ApiConstant.typeReverseMap.get(property.getType()));
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, keyClass, gType, propertyMap, false, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, copyApiMapPropertyMap, apiMapParamMap, mapMap.get(key), childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        }
        if (!copyApiMapPropertyMap.isEmpty()) {
            //属性Map
            Set<String> keySet = copyApiMapPropertyMap.keySet();
            Map<String, Map<String, ApiMapProperty>> mapMap = new HashMap<>(256);
            //复制入参Map类型注解信息
            Map<String, ApiParam> childApiMapParamMap = null;
            //复制出参Map类型注解信息
            Map<String, ApiReturn> childApiMapReturnMap = null;
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                //复制入参Map类型注解信息
                if (apiMapParamMap != null && !apiMapParamMap.isEmpty() && isParam) {
                    childApiMapParamMap = new HashMap<>(256);
                    Set<String> strings = apiMapParamMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(split[0] + ".")) {
                                childApiMapParamMap.put(string.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(string));
                            } else {
                                childApiMapParamMap.put(string, apiMapParamMap.get(string));
                            }
                        } else {
                            childApiMapParamMap.put(string, apiMapParamMap.get(string));
                        }
                    }
                }
                //复制出参Map类型注解信息
                if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty() && !isParam) {
                    childApiMapReturnMap = new HashMap<>(256);
                    Set<String> strings = apiMapReturnMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(split[0] + ".")) {
                                childApiMapReturnMap.put(string.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(string));
                            } else {
                                childApiMapReturnMap.put(string, apiMapReturnMap.get(string));
                            }
                        } else {
                            childApiMapReturnMap.put(string, apiMapReturnMap.get(string));
                        }
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiMapProperty> childApiMapPropertyMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapPropertyMap = mapMap.get(split[0]);
                        childApiMapPropertyMap.put(key.replaceFirst(split[0] + ".", ""), copyApiMapPropertyMap.get(key));
                    } else {
                        childApiMapPropertyMap = new HashMap<>(256);
                        childApiMapPropertyMap.put(key.replaceFirst(split[0] + ".", ""), copyApiMapPropertyMap.get(key));
                        mapMap.put(split[0], childApiMapPropertyMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiMapProperty apiReturn = copyApiMapPropertyMap.get(key);
                    String description = apiReturn.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String defaultValue = apiReturn.defaultValue();
                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                        property.setDefaultValue(defaultValue);
                    }
                    String type = apiReturn.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiReturn.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    String example = apiReturn.example();
                    if (StringUtils.isNotBlank(example)) {
                        property.setExample(example);
                    }
                    boolean required = apiReturn.required();
                    property.setRequited(required);
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, keyClass, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, mapMap.get(property.getName()), null, null, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    if (StringUtils.isNotBlank(type) && (StringUtils.isBlank(property.getClassName()) || "Object".equals(property.getClassName()))) {
                        property.setClassName(ApiConstant.typeReverseMap.get(property.getType()));
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, keyClass, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, mapMap.get(key), childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        }
        docModel.setApiProperties(apiProperties);
        docProperty.setDocModel(docModel);
        return docProperty;
    }

    /**
     * 驼峰转下划线
     *
     * @param str 原始字符串
     * @return 下滑线格式的字符串
     */
    public String humpToLine(String str) {
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
