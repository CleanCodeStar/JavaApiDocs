package com.citrsw.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 模型信息
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-23 23:14
 */
@Data
@Accessors(chain = true)
public class DocModel {
    /**
     * 描述
     */
    private String description;

    /**
     * 是否为数组类型
     */
    private String type;

    /**
     * 类名称
     */
    @JsonIgnore
    private String className;

    /**
     * 属性集合 包括只有get方法的虚拟属性
     */
    private Set<DocProperty> apiProperties = new TreeSet<>();

    /**
     * 形式  json,form-data
     */
    private String form;

    public Set<DocProperty> params() {
        if (StringUtils.isNotBlank(form) && "json".equals(form)) {
            return new LinkedHashSet<>();
        }
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            return apiProperties;
        }
        Set<DocProperty> apiProperties = new LinkedHashSet<>();
        for (DocProperty docProperty : this.apiProperties) {
            DocProperty property = new DocProperty();
            property.setName(docProperty.getName());
            property.setDescription(docProperty.getDescription());
            property.setType(docProperty.getType());
            property.setFormat(docProperty.getFormat());
            property.setRequited(docProperty.getRequited());
            property.setDefaultValue(docProperty.getDefaultValue());
            property.setExample(docProperty.getExample());
            property.setDocModel(docProperty.getDocModel());
            apiProperties.addAll(docProperty.param(property));
        }
        return apiProperties;
    }

    public String paramJson() {
        if (StringUtils.isNotBlank(form) && "form-data".equals(form)) {
            return "";
        }
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String tab = "";
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            tab = "    ";
            builder.append("[").append("\r\n");
        }
        builder.append(tab).append("{");
        for (Iterator<DocProperty> it = apiProperties.iterator(); it.hasNext(); ) {
            DocProperty docProperty = it.next();
            String json = docProperty.getJson(new StringBuilder("    ").append(tab), true, false);
            if (!json.startsWith("[") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
        }
        builder.append("\r\n").append(tab).append("}");
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            builder.append("\r\n").append("]");
        }
        return builder.toString();
    }

    public String paramExample() {
        if (StringUtils.isNotBlank(form) && "form-data".equals(form)) {
            return "";
        }
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String tab = "";
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            tab = "    ";
            builder.append("[").append("\r\n");
        }
        builder.append(tab).append("{");
        for (Iterator<DocProperty> it = apiProperties.iterator(); it.hasNext(); ) {
            DocProperty docProperty = it.next();
            String json = docProperty.getJson(new StringBuilder("    ").append(tab), true, true);
            if (!json.startsWith("[") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
        }
        builder.append("\r\n").append(tab).append("}");
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            builder.append("\r\n").append("]");
        }
        return builder.toString();
    }

    public String returnJson() {
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            String json = apiProperties.iterator().next().getJson(new StringBuilder(), false, false);
            return json.substring(json.indexOf("//") + 2);
        }
        StringBuilder builder = new StringBuilder();
        String tab = "";
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            tab = "    ";
            builder.append("[").append("\r\n");
        }
        builder.append(tab).append("{");
        for (Iterator<DocProperty> it = apiProperties.iterator(); it.hasNext(); ) {
            DocProperty docProperty = it.next();
            String json = docProperty.getJson(new StringBuilder("    ").append(tab), false, false);
            if (!json.startsWith("[") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
        }
        builder.append("\r\n").append(tab).append("}");
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            builder.append("\r\n").append("]");
        }
        return builder.toString();
    }

    /**
     * 生成安卓实体类代码
     */
    public void android(Set<String> strings) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(className)) {
            if (StringUtils.isNotBlank(description)) {
                builder.append("/**\r\n * ").append(description).append("\r\n */\r\n");
            }
            builder.append("@Accessors(chain = true)\r\n").append("@Data\r\n");
            builder.append("public class ").append(className).append(" {");
        }
        for (Iterator<DocProperty> it = apiProperties.iterator(); it.hasNext(); ) {
            DocProperty docProperty = it.next();
            String android = docProperty.android(strings);
            builder.append(android);
        }
        if (StringUtils.isNotBlank(className) && builder.length() > 0) {
            builder.append("\r\n}\r\n\r\n");
        }
        strings.add(builder.toString());
    }

    /**
     * 生成IOS实体类代码
     */
    public void ios(Set<String> strings) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(className)) {
            if (StringUtils.isNotBlank(description)) {
                builder.append("/**\r\n * ").append(description).append("\r\n */\r\n");
            }
            builder.append("struct ").append(className).append(" {");
        }
        for (Iterator<DocProperty> it = apiProperties.iterator(); it.hasNext(); ) {
            DocProperty docProperty = it.next();
            String android = docProperty.ios(strings);
            builder.append(android);
        }
        if (StringUtils.isNotBlank(className) && builder.length() > 0) {
            builder.append("\r\n}\r\n\r\n");
        }
        strings.add(builder.toString());
    }
}
