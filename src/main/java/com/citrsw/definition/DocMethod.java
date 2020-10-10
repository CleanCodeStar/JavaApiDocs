package com.citrsw.definition;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

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
public class DocMethod implements Comparable<DocMethod> {

    /**
     * 描述
     */
    private String description;

    /**
     * 请求方式
     */
    @JsonProperty("mode_set")
    private Set<String> modeSet;

    /**
     * 请求地址
     */
    @JsonProperty("uri_set")
    private Set<String> uriSet;

    /**
     * form-data入参
     */
    private Set<DocProperty> params;

    /**
     * json入参
     */
    @JsonProperty("param_json")
    private String paramJson;

    /**
     * json入参例子
     */
    @JsonProperty("param_example")
    private String paramExample;

    /**
     * json出参
     */
    @JsonProperty("return_json")
    private String returnJson;

    /**
     * 状态码
     */
    @JsonProperty("doc_codes")
    private Set<DocCode> docCodes = new TreeSet<>();


    @Override
    public int compareTo(DocMethod o) {
        if (StringUtils.isNotBlank(description) && (StringUtils.isNotBlank(o.description))) {
            return description.compareTo(o.description);
        }
        if (StringUtils.isNotBlank(description)) {
            return 1;
        }
        if (StringUtils.isNotBlank(o.description)) {
            return -1;
        }
        return 0;
    }
}
