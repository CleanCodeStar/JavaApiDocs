package com.citrsw.common;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

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
        TYPE_MAP.put("boolean", "Bool");
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

    /**
     * 重新生成符合泛型形式的Type
     * 用于返回形式的泛型
     *
     * @param returnType 返回的Type
     * @return 符合泛型形式的Type
     */
    public static Type regenerateType(Type returnType) {
        if (returnType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) returnType;
            Type rawType = pType.getRawType();
            List<Type> types = new ArrayList<>();
            for (Type actualTypeArgument : pType.getActualTypeArguments()) {
                types.add(regenerateType(actualTypeArgument));
            }
            return ParameterizedTypeImpl.make((Class<?>) rawType, types.toArray(new Type[0]), null);
        } else {
            return returnType;
        }
    }

    /**
     * 获取内网IP
     *
     * @return
     * @throws Exception
     */
    public static String getLocalIp() throws Exception {
        // 本地IP，如果没有配置外网IP则返回它
        String localip = null;
        // 外网IP
        String netip = null;
        Enumeration<NetworkInterface> netInterfaces;
        netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip = null;
        // 是否找到外网IP
        boolean finded = false;
        while (netInterfaces.hasMoreElements() && !finded) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                ip = address.nextElement();
                if (!ip.isSiteLocalAddress()
                        && !ip.isLoopbackAddress()
                        // 外网IP
                        && !ip.getHostAddress().contains(":")) {
                    netip = ip.getHostAddress();
                    finded = true;
                    break;
                } else if (ip.isSiteLocalAddress()
                        && !ip.isLoopbackAddress()
                        // 内网IP
                        && !ip.getHostAddress().contains(":")) {
                    localip = ip.getHostAddress();
                }
            }
        }
        if (netip != null && !"".equals(netip)) {
            return netip;
        } else {
            return localip;
        }
    }
}
