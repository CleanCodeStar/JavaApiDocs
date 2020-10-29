package com.citrsw.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 写点注释
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-10-29 1:24
 */
public class ApiUtils {
    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("Integer", "Int");
        TYPE_MAP.put("int", "Int");
        TYPE_MAP.put("Long", "Int");
        TYPE_MAP.put("long", "Int");
        TYPE_MAP.put("Boolean", "Bool");
        TYPE_MAP.put("boolean", " Bool ");
        TYPE_MAP.put("Float", "Float");
        TYPE_MAP.put("float", "Float");
        TYPE_MAP.put("Double", "Double");
        TYPE_MAP.put("double", "Double");
        TYPE_MAP.put("String", "String");
        TYPE_MAP.put("Date", "Date");
        TYPE_MAP.put("LocalDateTime", "Date");
        TYPE_MAP.put("LocalDate", "Date");
        TYPE_MAP.put("LocalTime", "Date");
        TYPE_MAP.put("short", "short");
        TYPE_MAP.put("Object", "Any");
        TYPE_MAP.put("MultipartFile", "[Int]");
    }

    /**
     * java类型转Ios类型
     *
     * @param className
     * @return
     */
    public static String javaToIosType(String className) {
        return TYPE_MAP.get(className);
    }

}
