package com.citrsw.core;

import com.citrsw.annatation.ApiBasicReturn;
import com.citrsw.annatation.ApiMapReturn;
import com.citrsw.annatation.ApiReturn;
import com.citrsw.annatation.ApiReturnModelProperty;
import com.citrsw.common.ApiUtils;
import com.citrsw.definition.DocModel;
import com.citrsw.definition.DocProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 处理方法返回参数
 *
 * @author 李振峰
 * @version 1.0
 * @date 2021-08-04 20:23
 */
@Component
@Slf4j
public class ReturnHandle {
    private final ModelHandle handleModel;

    public ReturnHandle(ModelHandle handleModel) {
        this.handleModel = handleModel;
    }

    /**
     * 处理方法的出参
     *
     * @param apiMapReturn             自定义方法的出参
     * @param apiBasicReturn           自定义基本数据类型出参
     * @param returnType               方法的出参类型
     * @param genericReturnType        方法的出参类型
     * @param apiReturnModelProperties 重新配置出参属性信息注解
     * @return 处理后的出参模型
     */
    public DocModel handleReturn(ApiMapReturn apiMapReturn, ApiBasicReturn
            apiBasicReturn, Class<?> returnType, Type genericReturnType, ApiReturnModelProperty[] apiReturnModelProperties) {
        Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap = new HashMap<>(256);
        if (apiReturnModelProperties != null && apiReturnModelProperties.length > 0) {
            for (ApiReturnModelProperty apiReturnModelProperty : apiReturnModelProperties) {
                String name = apiReturnModelProperty.name();
                apiReturnModelPropertyMap.put(name, apiReturnModelProperty);
            }
        }
        //对于map类型的出参 用ApiMapParam 来处理
        Map<String, ApiReturn> apiMapReturnMap = new HashMap<>(256);
        if (apiMapReturn != null && apiMapReturn.value().length > 0) {
            for (ApiReturn apiReturn : apiMapReturn.value()) {
                String value = apiReturn.name();
                apiMapReturnMap.put(value, apiReturn);
            }
        }
        //循环依赖收集集合
        Set<Class<?>> repeats = new HashSet<>();
        DocProperty docProperty = new DocProperty();
        docProperty = handleModel.handleModel(docProperty, returnType, ApiUtils.regenerateType(genericReturnType), new HashMap<>(256), false, true, repeats, null, apiReturnModelPropertyMap, null, null, apiMapReturnMap, null, null);
        DocModel docModel = new DocModel();
        if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
            return docModel;
        }
        //借用apiProperty为壳返回apiModel
        //借用apiProperty为壳返回apiModel
        if (docProperty.getDocModel() == null) {
            if (apiBasicReturn != null) {
                String description = apiBasicReturn.description();
                if (StringUtils.isNotBlank(description)) {
                    docProperty.setDescription(description);
                }
                String format = apiBasicReturn.format();
                if (StringUtils.isNotBlank(format)) {
                    docProperty.setFormat(format);
                }
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
        return docModel;
    }

}
