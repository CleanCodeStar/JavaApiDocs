package com.citrsw.definition;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 方法信息
 *
 * @author Zhenfeng Li
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
     * 入参模型
     */
    private DocModel paramDocModel;

    /**
     * json入参
     */
    public String getParamJson() {
        return paramDocModel.paramJson();
    }

    /**
     * json入参例子
     */
    public String getParamExample() {
        return paramDocModel.paramExample();
    }

    /**
     * json入参例子
     */
    public Set<DocProperty> getParams() {
        return paramDocModel.params();
    }

    /**
     * 出参模型
     */
    private DocModel returnDocModel;

    /**
     * json出参
     */
    public String getReturnJson() {
        return returnDocModel.returnJson();
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
    public String getParamVue() {
        return paramDocModel.paramVue();
    }

    /**
     * 生成响应Vue代码
     */
    public String getReturnVue() {
        return paramDocModel.returnVue();
    }


    /**
     * 状态码
     */
    private Set<DocCode> docCodes = new TreeSet<>();
}

