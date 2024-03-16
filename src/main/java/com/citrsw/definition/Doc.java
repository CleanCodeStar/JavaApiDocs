package com.citrsw.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * Api结果集
 *
 * @author 李振峰
 * @version 1.0
 */
@Data
@Accessors(chain = true)
public class Doc {
    /**
     * 项目名
     */
    private String name;

    /**
     * header中token的名称
     */
    private String tokenName;

    /**
     * 类集合
     */
    @JsonProperty("doc_classes")
    private Set<DocClass> docClasses;
}
