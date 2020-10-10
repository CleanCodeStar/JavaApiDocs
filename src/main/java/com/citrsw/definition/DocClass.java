package com.citrsw.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * 类信息
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-23 23:12
 */
@Data
@Accessors(chain = true)
public class DocClass implements Comparable<DocClass> {

    /**
     * 描述
     */
    private String description;

    /**
     * 方法集合
     */
    @JsonProperty("doc_methods")
    private Set<DocMethod> docMethods;

    @Override
    public int compareTo(DocClass o) {
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
