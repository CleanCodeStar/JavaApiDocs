package com.citrsw.core;

import com.citrsw.annatation.ApiEnable;
import com.citrsw.annatation.ApiGlobalClass;
import com.citrsw.annatation.ApiGlobalCode;
import com.citrsw.annatation.ApiProperty;
import com.citrsw.common.ApiConstant;
import com.citrsw.common.ApiUtils;
import com.citrsw.controller.ApiController;
import com.citrsw.definition.Doc;
import com.citrsw.definition.DocClass;
import com.citrsw.definition.DocCode;
import com.citrsw.enumeration.TypeEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * 上下文(核心)
 *
 * @author 李振峰
 * @version 1.0
 * @date 2020-09-23 20:14
 */
@Slf4j
@Component
public class ApiContext implements CommandLineRunner {

    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * 当前的环境
     */
    @Value("${spring.profiles.active:}")
    private String active;
    /**
     * 当前的端口号
     */
    @Value("${server.port:8080}")
    private String port;

    /**
     * 当前的项目名
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 当前的项目名
     */
    @Value("${spring.mvc.servlet.path:}")
    private String servletPath;

    /**
     * ssl是否激活
     */
    @Value("${server.ssl.key-store:}")
    private String sslEnabled;

    private final ControllerHandle controllerHandle;

    /**
     * 最终的Api文档类
     */
    @Getter
    private Doc doc;

    public ApiContext(RequestMappingHandlerMapping handlerMapping, ControllerHandle controllerHandle) {
        this.handlerMapping = handlerMapping;
        this.controllerHandle = controllerHandle;
    }

    @Override
    public void run(String... args) {
        try {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    Class<?> mainApplicationClass = Class.forName(stackTraceElement.getClassName());
                    //获取ApiEnable注解
                    ApiEnable apiEnable = mainApplicationClass.getAnnotation(ApiEnable.class);
                    if (apiEnable == null) {
                        return;
                    }
                    log.info("======Api启动======");
                    boolean pass = true;
                    String[] actives = apiEnable.actives();
                    if (actives.length > 0 && StringUtils.isNotBlank(this.active)) {
                        pass = false;
                        //适用环境
                        for (String active : actives) {
                            if (active.equals(this.active)) {
                                pass = true;
                                break;
                            }
                        }
                        if (!pass) {
                            ApiConstant.apiEnable = false;
                            log.warn("Api已禁用,当前启动环境为[{}],api指定环境为{}", this.active, actives);
                            return;
                        }
                    } else {
                        log.warn("未配置启动环境,已适用全部环境");
                    }
                    //获取全局类配置
                    ApiGlobalClass[] apiGlobalClasses = mainApplicationClass.getAnnotationsByType(ApiGlobalClass.class);
                    for (ApiGlobalClass apiGlobalClass : apiGlobalClasses) {
                        String description = apiGlobalClass.description();
                        if (TypeEnum.PARAM.equals(apiGlobalClass.type())) {
                            if (StringUtils.isNotBlank(description)) {
                                ApiConstant.PARAM_GLOBAL_CLASS_DESCRIPTION_MAP.put(apiGlobalClass.name(), description);
                            }
                            for (ApiProperty property : apiGlobalClass.properties()) {
                                Map<String, ApiProperty> apiPropertyMap;
                                if (ApiConstant.PARAM_GLOBAL_CLASS_MAP.containsKey(apiGlobalClass.name())) {
                                    apiPropertyMap = ApiConstant.PARAM_GLOBAL_CLASS_MAP.get(apiGlobalClass.name());
                                    apiPropertyMap.put(property.name(), property);
                                } else {
                                    apiPropertyMap = new HashMap<>(256);
                                    apiPropertyMap.put(property.name(), property);
                                    ApiConstant.PARAM_GLOBAL_CLASS_MAP.put(apiGlobalClass.name(), apiPropertyMap);
                                }
                            }
                        } else {
                            if (StringUtils.isNotBlank(description)) {
                                ApiConstant.RETURN_GLOBAL_CLASS_DESCRIPTION_MAP.put(apiGlobalClass.name(), description);
                            }
                            for (ApiProperty property : apiGlobalClass.properties()) {
                                Map<String, ApiProperty> apiPropertyMap;
                                if (ApiConstant.RETURN_GLOBAL_CLASS_MAP.containsKey(apiGlobalClass.name())) {
                                    apiPropertyMap = ApiConstant.RETURN_GLOBAL_CLASS_MAP.get(apiGlobalClass.name());
                                    apiPropertyMap.put(property.name(), property);
                                } else {
                                    apiPropertyMap = new HashMap<>(256);
                                    apiPropertyMap.put(property.name(), property);
                                    ApiConstant.RETURN_GLOBAL_CLASS_MAP.put(apiGlobalClass.name(), apiPropertyMap);
                                }
                            }
                        }
                    }
                    //获取全局状态码配置
                    ApiGlobalCode[] apiGlobalCodes = mainApplicationClass.getAnnotationsByType(ApiGlobalCode.class);
                    for (ApiGlobalCode apiGlobalCode : apiGlobalCodes) {
                        String name = apiGlobalCode.name();
                        String value = apiGlobalCode.value();
                        String description = apiGlobalCode.description();
                        DocCode docCode = new DocCode();
                        docCode.setName(name);
                        docCode.setValue(value);
                        docCode.setDescription(description);
                        ApiConstant.DOC_GLOBAL_CODES.add(docCode);
                    }
                    //获取需要扫描的包
//                    List<String> packages = controllerHandle.takePackages(mainApplicationClass);
                    Set<Class<?>> classes = new HashSet<>();
                    Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
                    for (HandlerMethod handlerMethod : handlerMethods.values()) {
                        Class<?> controllerClass = handlerMethod.getBeanType();
                        if (controllerClass != BasicErrorController.class && controllerClass != ApiController.class) {
                            classes.add(controllerClass);
                        }
                    }
//                    for (String packageName : packages) {
//                        controllerHandle.scanner(packageName, classes);
//                    }
                    //springboot中配置需要扫描的类
//                    SpringBootApplication applicationClassAnnotation = mainApplicationClass.getAnnotation(SpringBootApplication.class);
//                    Class<?>[] scanClasses = applicationClassAnnotation.scanBasePackageClasses();
//                    if (scanClasses.length > 0) {
//                        log.info("已获取applicationClassAnnotation配置中需要扫描的类:{}", Arrays.toString(scanClasses));
//                        classes.addAll(Arrays.asList(scanClasses));
//                    }
                    log.info("已获取到所有需要扫描的Controller类({})个:{}", classes.size(), classes);
                    //处理Controller类
                    Set<DocClass> docClasses = controllerHandle.handleClass(classes);
                    doc = new Doc().setDocClasses(docClasses);
                    //获取header中token的名称
                    doc.setTokenName(apiEnable.tokenName());
                    if (StringUtils.isNotBlank(apiEnable.name())) {
                        doc.setName(apiEnable.name());
                    }
                    System.out.println("\n" +
                            "       __                  ___          _ ____                 \n" +
                            "      / /___ __   ______ _/   |  ____  (_) __ \\____  __________\n" +
                            " __  / / __ `/ | / / __ `/ /| | / __ \\/ / / / / __ \\/ ___/ ___/\n" +
                            "/ /_/ / /_/ /| |/ / /_/ / ___ |/ /_/ / / /_/ / /_/ / /__(__  ) \n" +
                            "\\____/\\__,_/ |___/\\__,_/_/  |_/ .___/_/_____/\\____/\\___/____/  \n" +
                            "                             /_/                               \n" +
                            "                                                  1.6.12-jdk1.8   \n");
                    //获取本机地址及端口号
                    try {
                        List<String> outIps = ApiUtils.getLocalIps();
                        for (String ip : outIps) {
                            String uri = ip + ":" + port + contextPath + servletPath + "/citrsw/index.html";
                            uri = uri.replaceAll("//", "/");
                            System.out.println("Api访问地址:  " + (StringUtils.isNotBlank(sslEnabled) ? "https" : "http") + "://" + uri);
                        }
                    } catch (Exception ignored) {
                    }
                    String uri = port + contextPath + servletPath + "/citrsw/index.html";
                    uri = uri.replaceAll("//", "/");
                    System.out.println("Api访问地址:  " + (StringUtils.isNotBlank(sslEnabled) ? "https" : "http") + "://127.0.0.1" + ":" + uri);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("======Api解析异常======");
        }
    }
}

