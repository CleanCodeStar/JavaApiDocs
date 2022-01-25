package com.citrsw.definition;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * 方法信息
 *
 * @author 李振峰
 * @version 1.0
 * @date 2020-09-23 23:12
 */
@Data
@Accessors(chain = true)
public class TempMethod {

    /**
     * 描述
     */
    private String description;

    /**
     * 请求方式
     */
    private Set<String> modeSet;

    /**
     * 请求地址
     */
    private Set<String> uriSet;

    /**
     * 入参必传参数(key:全名称，value:字段属性)
     */
    private Map<String, DocProperty> paramRequireMap = new HashMap<>(156);

    /**
     * 入参模型
     */
    private DocModel paramDocModel;

    /**
     * json入参
     */
    public String getParamJson() {
        return paramDocModel.paramJson(paramRequireMap);
    }

    /**
     * json入参例子(没写错，是方法名起错了)
     */
    public String getParamExample() {
        return paramDocModel.paramExample();
    }

    /**
     * form-data入参例子
     */
    public Set<DocProperty> getParams() {
        return paramDocModel.params(paramRequireMap);
    }

    /**
     * 出参模型
     */
    private DocModel returnDocModel;

    /**
     * json出参
     */
    public String getReturnJson() {
        return returnDocModel == null ? "" : returnDocModel.returnJson();
    }

    /**
     * 生成入参安卓实体类代码
     */
    public String getParamAndroid() {
        Set<String> strings = new TreeSet<>();
        paramDocModel.android(strings);
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * 生成响应安卓实体类代码
     */
    public String getReturnAndroid() {
        if (returnDocModel == null) {
            return "";
        }
        Set<String> strings = new TreeSet<>();
        returnDocModel.android(strings);
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * 生成IOS实体类代码
     */
    public String getParamIos() {
        Set<String> strings = new TreeSet<>();
        paramDocModel.ios(strings);
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * 生成IOS实体类代码
     */
    public String getReturnIos() {
        if (returnDocModel == null) {
            return "";
        }
        Set<String> strings = new TreeSet<>();
        returnDocModel.ios(strings);
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * 生成请求Vue代码
     */
    public Map<String, Map<String, String>> getParamVue() {
        Map<String, Map<String, String>> mapList = new LinkedHashMap<>(50);
        if (paramDocModel != null) {
            paramDocModel.paramVue(mapList);
        }
        return mapList;
    }

    /**
     * 生成响应Vue代码
     */
    public Map<String, Map<String, String>> getReturnVue() {
        Map<String, Map<String, String>> mapList = new LinkedHashMap<>(50);
        if (returnDocModel != null) {
            returnDocModel.returnVue(mapList);
        }
        return mapList;
    }

    /**
     * 状态码
     */
    private Set<DocCode> docCodes = new TreeSet<>();
}

