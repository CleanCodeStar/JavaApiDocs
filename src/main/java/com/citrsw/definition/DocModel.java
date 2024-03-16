package com.citrsw.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 模型信息
 *
 * @author 李振峰
 * @version 1.0
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

    public Set<DocProperty> params(Map<String, DocProperty> paramRequireMap) {
        if (StringUtils.isNotBlank(form) && "json".equals(form)) {
            return new LinkedHashSet<>();
        }
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            DocProperty docProperty = apiProperties.iterator().next();
            if (docProperty.getRequited() != null && docProperty.getRequited()) {
                paramRequireMap.put(docProperty.getName(), docProperty);
            }
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
            if (docProperty.getRequited() != null && docProperty.getRequited()) {
                paramRequireMap.put(docProperty.getName(), docProperty);
            }
            apiProperties.addAll(docProperty.param(property, paramRequireMap));
        }
        return apiProperties;
    }

    public String paramJson(Map<String, DocProperty> paramRequireMap) {
        if (StringUtils.isNotBlank(form) && "form-data".equals(form)) {
            return "";
        }
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            return type;
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
            String json = docProperty.getJson(null, new StringBuilder("    ").append(tab), true, false, paramRequireMap);
            if (!json.startsWith("[") && !json.contains("{") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
            if ((json.endsWith("}") || json.endsWith("]")) && it.hasNext()) {
                builder.append(",");
            }
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
            String json = docProperty.getJson(null, new StringBuilder("    ").append(tab), true, true, null);
            if (!json.startsWith("[") && !json.contains("{") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
            if ((json.endsWith("}") || json.endsWith("]")) && it.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("\r\n").append(tab).append("}");
        if (StringUtils.isNotBlank(type) && type.contains("[0]")) {
            builder.append("\r\n").append("]");
        }
        return builder.toString();
    }

    public String returnJson() {
        if (StringUtils.isNotBlank(type) && !type.contains("[0]")) {
            String json = apiProperties.iterator().next().getJson(null, new StringBuilder(), false, false, null);
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
            String json = docProperty.getJson(null, new StringBuilder("    ").append(tab), false, false, null);
            if (!json.startsWith("[") && !json.contains("{") && !it.hasNext()) {
                json = json.replaceFirst("\\,", "");
            }
            builder.append(tab).append("    ").append(json);
            if ((json.endsWith("}") || json.endsWith("]")) && it.hasNext()) {
                builder.append(",");
            }
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
        for (DocProperty docProperty : apiProperties) {
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
            builder.append("class ").append(className).append(" {");
        }
        for (DocProperty docProperty : apiProperties) {
            String android = docProperty.ios(strings);
            builder.append(android);
        }
        if (StringUtils.isNotBlank(className) && builder.length() > 0) {
            builder.append("\r\n}\r\n\r\n");
        }
        strings.add(builder.toString());
    }

    /**
     * 生成请求Vue代码
     */
    public void paramVue(Map<String, Map<String, String>> mapList) {
        if (StringUtils.isBlank(className)) {
            if (mapList.isEmpty()) {
                className = "Class0";
            }
            for (int i = 0; i < mapList.size(); i++) {
                if (!mapList.containsKey("Class" + i)) {
                    className = "Class" + i;
                    break;
                }
            }

        }
        StringBuilder htmlBuilder = new StringBuilder();
        StringBuilder rulesBuilder = new StringBuilder();
        rulesBuilder.append("export default {").append("\r\n  ")
                .append("name: \"").append(StringUtils.capitalize(className)).append("Vue\",").append("\r\n  ")
                .append("data() {").append("\r\n    ")
                .append("return {").append("\r\n      ");
        rulesBuilder.append(StringUtils.uncapitalize(className)).append(": {},").append("\r\n      ");
        rulesBuilder.append("visible: false,").append("\r\n      ");
        htmlBuilder.append("<el-dialog title=\"").append("新增");
        if (StringUtils.isNotBlank(description)) {
            htmlBuilder.append(description);
        }
        htmlBuilder.append("\" center :visible.sync=\"visible\" width=\"30%\">").append("\r\n");
        htmlBuilder.append("    ").append("<el-form :model=\"").append(StringUtils.uncapitalize(className)).append("\" :rules=\"rules\" ref=\"").append(StringUtils.uncapitalize(className)).append("\" ").append("\r\n");
        htmlBuilder.append("              ").append("label-width=\"100px\" @keyup.enter.native=\"save").append(className).append("()\">").append("\r\n");
        rulesBuilder.append("rules: {").append("\r\n");
        for (DocProperty docProperty : apiProperties) {
            docProperty.paramVue(rulesBuilder, htmlBuilder, StringUtils.uncapitalize(className), mapList);
        }
        rulesBuilder.delete(rulesBuilder.length() - 3, rulesBuilder.length() - 2);
        htmlBuilder.append("    ").append("</el-form>").append("\r\n");
        htmlBuilder.append("    ").append("<span slot=\"footer\" class=\"dialog-footer\">").append("\r\n");
        htmlBuilder.append("        ").append("<el-button type=\"primary\" @click=\"save").append(className).append("()\" size=\"mini\">提 交</el-button>").append("\r\n");
        htmlBuilder.append("    ").append("</span>").append("\r\n");
        htmlBuilder.append("</el-dialog>").append("\r\n").append("\r\n");
        rulesBuilder.append("      }").append("\r\n    ");
        rulesBuilder.append("}").append("\r\n  ");
        rulesBuilder.append("},").append("\r\n  ");
        rulesBuilder.append("methods: {").append("\r\n      ");
        rulesBuilder.append("save").append(className).append("() {").append("\r\n      ");
        rulesBuilder.append("}").append("\r\n  ");
        rulesBuilder.append("}").append("\r\n");
        rulesBuilder.append("}").append("\r\n");
        Map<String, String> map = new HashMap<>(3);
        map.put("HTML", htmlBuilder.toString());
        map.put("JavaScript", rulesBuilder.toString());
        mapList.put(StringUtils.capitalize(className), map);
    }

    /**
     * 生成响应Vue代码
     */
    public void returnVue(Map<String, Map<String, String>> mapList) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isBlank(className)) {
            if (mapList.isEmpty()) {
                className = "Class0";
            }
            for (int i = 0; i < mapList.size(); i++) {
                if (!mapList.containsKey("Class" + i)) {
                    className = "Class" + i;
                    break;
                }
            }
        }
        builder.append("<el-table size=\"mini\" :data=\"").append(StringUtils.uncapitalize(className)).append("s\">").append("\r\n");
        for (DocProperty docProperty : apiProperties) {
            docProperty.returnVue(builder, StringUtils.uncapitalize(className), mapList);
        }
        builder.append("</el-table>").append("\r\n");
        builder.append("<el-pagination").append("\r\n");
        builder.append("    ").append("background").append("\r\n");
        builder.append("    ").append(":current-page=\"page.current*1\"").append("\r\n");
        builder.append("    ").append(":page-sizes=\"[10, 20, 50,100]\"").append("\r\n");
        builder.append("    ").append("layout=\"->,sizes,total,prev, pager, next,jumper\"").append("\r\n");
        builder.append("    ").append("@current-change=\"function(v) {").append("\r\n");
        builder.append("          ").append("page.current = v").append("\r\n");
        builder.append("          ").append("query").append(className).append("s();").append("\r\n");
        builder.append("        ").append("}\"").append("\r\n");
        builder.append("    ").append("@size-change=\"function(v) {").append("\r\n");
        builder.append("          ").append("page.size = v").append("\r\n");
        builder.append("          ").append("query").append(className).append("s();").append("\r\n");
        builder.append("        ").append("}\"").append("\r\n");
        builder.append("    ").append(":total=\"page.total*1\">").append("\r\n");
        builder.append("</el-pagination>").append("\r\n");
        Map<String, String> map = new HashMap<>(3);
        map.put("HTML", builder.toString());
        String rulesBuilder = "export default {" + "\r\n  " +
                "name: \"" + StringUtils.capitalize(className) + "Vue\"," + "\r\n  " +
                "data() {" + "\r\n    " +
                "return {" + "\r\n      " +
                StringUtils.uncapitalize(className) + "s: []," + "\r\n      " +
                "page: {}" + "\r\n    " +
                "}" + "\r\n  " +
                "}," + "\r\n  " +
                "methods: {" + "\r\n      " +
                "query" + className + "s() {" + "\r\n      " +
                "}" + "\r\n  " +
                "}" + "\r\n" +
                "}" + "\r\n";
        map.put("JavaScript", rulesBuilder);

        mapList.put(StringUtils.capitalize(className), map);
    }

}
