package com.citrsw.core;

import com.citrsw.definition.*;
import org.apache.commons.lang3.StringUtils;

/**
 * Markdown处理类
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-10-01 20:27
 */
public class MarkdownHandler {
    private final StringBuffer buffer = new StringBuffer();

    public String generate(Doc doc) {
        String name = doc.getName();
        buffer.append("# ").append(name).append("\n");
        buffer.append("[TOC]").append("\n");
        for (DocClass docClass : doc.getDocClasses()) {
            creatClass(docClass);
        }
        return buffer.toString();
    }

    private void creatClass(DocClass docClass) {
        //类描述
        buffer.append("## ").append(docClass.getDescription()).append("\n");
        for (DocMethod docMethod : docClass.getDocMethods()) {
            creatMethod(docMethod);
        }

    }

    private void creatMethod(DocMethod docMethod) {
        //方法描述
        buffer.append("### ").append(docMethod.getDescription()).append("\n");
        //请求地址
        buffer.append("***请求地址***").append("\n");
        for (String uri : docMethod.getUriSet()) {
            buffer.append("`").append(uri).append("`");
            for (String mode : docMethod.getModeSet()) {
                buffer.append("**").append(mode).append("** ");
            }
            buffer.append("\n");
        }
        //请求参数
        buffer.append("***请求参数***").append("\n");
        if (docMethod.getParams() != null && !docMethod.getParams().isEmpty()) {
            buffer.append("| 参数名 | 类型 | 是否必传 | 描述 | 格式 | 默认值 | 示例 |").append("\n");
            buffer.append("| :----- | :--- | :------- | :--- | :--- | :----- | :--- |").append("\n");
            for (DocProperty param : docMethod.getParams()) {
                buffer.append("|").append(param.getName())
                        .append("|").append(param.getType())
                        .append("|").append(param.getRequited() != null && param.getRequited() ? "是" : "否")
                        .append("|").append(StringUtils.isNotBlank(param.getDescription()) ? param.getDescription() : "")
                        .append("|").append(StringUtils.isNotBlank(param.getFormat()) ? param.getFormat() : "")
                        .append("|").append(StringUtils.isNotBlank(param.getDefaultValue()) ? param.getDefaultValue() : "")
                        .append("|").append(StringUtils.isNotBlank(param.getExample()) ? param.getExample() : "")
                        .append("| ").append("\n");
            }
        }
        if (StringUtils.isNotBlank(docMethod.getParamJson())) {
            //json形式
            buffer.append("```json").append("\n");
            buffer.append(docMethod.getParamJson()).append("\n");
            buffer.append("```").append("\n");
        }
        //响应
        buffer.append("***响应***").append("\n");
        if (StringUtils.isNotBlank(docMethod.getReturnJson())) {
            //json形式
            buffer.append("```json").append("\n");
            buffer.append(docMethod.getReturnJson()).append("\n");
            buffer.append("```").append("\n");
        }
        //状态码
        buffer.append("***状态码***").append("\n");
        if (docMethod.getDocCodes() != null && !docMethod.getDocCodes().isEmpty()) {
            buffer.append("| 参数名 | 值 | 描述 |").append("\n");
            buffer.append("| :----- | :--- | :------- |").append("\n");
            for (DocCode docCode : docMethod.getDocCodes()) {
                buffer.append("|").append(docCode.getName())
                        .append("|").append(docCode.getValue())
                        .append("|").append(StringUtils.isNotBlank(docCode.getDescription()) ? docCode.getDescription() : "")
                        .append("| ").append("\n");
            }
        }
    }
}
