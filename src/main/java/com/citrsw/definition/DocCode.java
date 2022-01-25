package com.citrsw.definition;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 状态码定义
 *
 * @author 李振峰
 * @version 1.0
 * @date 2020-09-27 6:29
 */
@Data
@Accessors(chain = true)
public class DocCode implements Comparable<DocCode> {
    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;
    /**
     * 描述
     */
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocCode docCode = (DocCode) o;
        return new EqualsBuilder()
                .append(name, docCode.name)
                .append(value, docCode.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(value)
                .toHashCode();
    }

    @Override
    public int compareTo(DocCode docCode) {
        return name.compareTo(docCode.name) == 0 ? value.compareTo(docCode.value) : name.compareTo(docCode.name);
    }
}
