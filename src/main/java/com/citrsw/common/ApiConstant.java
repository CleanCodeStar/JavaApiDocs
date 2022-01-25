package com.citrsw.common;

import com.citrsw.annatation.ApiProperty;
import com.citrsw.definition.DocCode;
import com.citrsw.definition.TempMethod;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 写点注释
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2021-08-04 21:09
 */
public class ApiConstant {

    /**
     * 是否使用下滑线命名方式
     */
    public static boolean underscore;

    /**
     * 参数校验不通过的返回对象
     */
    public static boolean paramVerification;

    /**
     * Api是否可用
     */
    public static boolean apiEnable = true;

    /**
     * 参数校验不通过的返回对象
     */
    public static Map<String, TempMethod> methodMap = new HashMap<>(156);

    /**
     * java基本数据类型
     */
    public static Map<Class<?>, String> baseTypeMap = new HashMap<>(256);

    static {
        baseTypeMap.put(Integer.class, "int");
        baseTypeMap.put(int.class, "int");
        baseTypeMap.put(Long.class, "long");
        baseTypeMap.put(long.class, "long");
        baseTypeMap.put(Boolean.class, "boolean");
        baseTypeMap.put(boolean.class, "boolean");
        baseTypeMap.put(Float.class, "float");
        baseTypeMap.put(float.class, "float");
        baseTypeMap.put(Double.class, "double");
        baseTypeMap.put(double.class, "double");
        baseTypeMap.put(BigDecimal.class, "double");
        baseTypeMap.put(String.class, "string");
        baseTypeMap.put(Date.class, "datetime");
        baseTypeMap.put(LocalDateTime.class, "datetime");
        baseTypeMap.put(LocalDate.class, "date");
        baseTypeMap.put(LocalTime.class, "time");
        baseTypeMap.put(short.class, "short");
        baseTypeMap.put(Short.class, "short");
        baseTypeMap.put(Object.class, "");
        baseTypeMap.put(MultipartFile.class, "file");
    }

    /**
     * java基本数据类型
     */
    public static Map<String, String> typeReverseMap = new HashMap<>(256);

    static {
        typeReverseMap.put("int", "Integer");
        typeReverseMap.put("long", "Long");
        typeReverseMap.put("boolean", "Boolean");
        typeReverseMap.put("float", "Float");
        typeReverseMap.put("double", "Double");
        typeReverseMap.put("string", "String");
        typeReverseMap.put("datetime", "LocalDateTime");
        typeReverseMap.put("date", "LocalDate");
        typeReverseMap.put("time", "LocalTime");
        typeReverseMap.put("short", "Short");
        typeReverseMap.put("file", "MultipartFile");
    }

    /**
     * 全局状态码
     */
    public static final Set<DocCode> DOC_GLOBAL_CODES = new TreeSet<>();

    /**
     * 全局入参类注解
     */
    public static final Map<Class<?>, Map<String, ApiProperty>> PARAM_GLOBAL_CLASS_MAP = new HashMap<>(256);
    public static final Map<Class<?>, String> PARAM_GLOBAL_CLASS_DESCRIPTION_MAP = new HashMap<>(256);

    /**
     * 全局出参类注解
     */
    public static final Map<Class<?>, Map<String, ApiProperty>> RETURN_GLOBAL_CLASS_MAP = new HashMap<>(256);
    public static final Map<Class<?>, String> RETURN_GLOBAL_CLASS_DESCRIPTION_MAP = new HashMap<>(256);
}
