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
 * @date 2020-09-23 23:12
 */
@Data
@Accessors(chain = true)
public class Doc {
    /**
     * 项目名
     */
    private String name;

    /**
     * 类集合
     */
    @JsonProperty("doc_classes")
    private Set<DocClass> docClasses;
}
