package com.citrsw.core;

import com.citrsw.annatation.*;
import com.citrsw.definition.*;
import com.citrsw.enumeration.TypeEnum;
import com.citrsw.exception.ParamException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上下文(核心)
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-09-23 20:14
 */
@Slf4j
@Component
public class ApiContext {

    /**
     * 全局日期格式化
     */
    @Value("${spring.mvc.date-format:}")
    private String dateFormat;

    /**
     * 全局日期格式化
     */
    @Value("${spring.jackson.date-format:}")
    private String jsonFormat;
    /**
     * 当前的环境
     */
    @Value("${spring.profiles.active:}")
    private String active;

    /**
     * java基本数据类型
     */
    private final Map<Class<?>, String> baseTypeMap = new HashMap<>();

    /**
     * 实体缓存
     */
    private final Map<Class<?>, DocModel> apiModelMap = new HashMap<>();

    /**
     * 全局入参类注解
     */
    private final Map<Class<?>, Map<String, ApiProperty>> paramGlobalClassMap = new HashMap<>();
    private final Map<Class<?>, String> paramGlobalClassDescriptionMap = new HashMap<>();

    /**
     * 全局出参类注解
     */
    private final Map<Class<?>, Map<String, ApiProperty>> returnGlobalClassMap = new HashMap<>();
    private final Map<Class<?>, String> returnGlobalClassDescriptionMap = new HashMap<>();

    /**
     * 是否使用下滑线命名方式
     */
    private boolean underscore;

    /**
     * 全局状态码
     */
    private final Set<DocCode> docGlobalCodes = new TreeSet<>();

    /**
     * 最终的Api文档类
     */
    private Doc doc;

    /**
     * 正则
     */
    private final Pattern pattern = Pattern.compile("[A-Z]");

    public Doc getDoc() {
        return doc;
    }


    @PostConstruct
    public void init() {
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
        baseTypeMap.put(String.class, "string");
        baseTypeMap.put(Date.class, "datetime");
        baseTypeMap.put(LocalDateTime.class, "datetime");
        baseTypeMap.put(LocalDate.class, "date");
        baseTypeMap.put(LocalTime.class, "time");
        baseTypeMap.put(short.class, "short");
        baseTypeMap.put(Object.class, "");
        baseTypeMap.put(MultipartFile.class, "file");

        log.info("======Api启动======");
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
                    apiEnable.actives();
                    if (apiEnable.actives().length > 0 && StringUtils.isNotBlank(this.active)) {
                        //适用环境
                        boolean pass = false;
                        for (String active : apiEnable.actives()) {
                            if (active.equals(this.active)) {
                                pass = true;
                                break;
                            }
                        }
                        if (!pass) {
                            log.warn("Api生成失败,当前启动环境为{},api指定环境为{},", this.active, apiEnable.actives());
                            return;
                        }
                    }
                    //是否使用下划线命名
                    underscore = apiEnable.underscore();
                    //获取全局类配置
                    ApiGlobalClass[] apiGlobalClasses = mainApplicationClass.getAnnotationsByType(ApiGlobalClass.class);
                    if (apiGlobalClasses.length > 0) {
                        for (ApiGlobalClass apiGlobalClass : apiGlobalClasses) {
                            String description = apiGlobalClass.description();
                            if (TypeEnum.PARAM.equals(apiGlobalClass.type())) {
                                if (StringUtils.isNotBlank(description)) {
                                    paramGlobalClassDescriptionMap.put(apiGlobalClass.name(), description);
                                }
                                for (ApiProperty property : apiGlobalClass.properties()) {
                                    Map<String, ApiProperty> apiPropertyMap;
                                    if (paramGlobalClassMap.containsKey(apiGlobalClass.name())) {
                                        apiPropertyMap = paramGlobalClassMap.get(apiGlobalClass.name());
                                        apiPropertyMap.put(property.name(), property);
                                    } else {
                                        apiPropertyMap = new HashMap<>();
                                        apiPropertyMap.put(property.name(), property);
                                        paramGlobalClassMap.put(apiGlobalClass.name(), apiPropertyMap);
                                    }
                                }
                            } else {
                                if (StringUtils.isNotBlank(description)) {
                                    returnGlobalClassDescriptionMap.put(apiGlobalClass.name(), description);
                                }
                                for (ApiProperty property : apiGlobalClass.properties()) {
                                    Map<String, ApiProperty> apiPropertyMap;
                                    if (returnGlobalClassMap.containsKey(apiGlobalClass.name())) {
                                        apiPropertyMap = returnGlobalClassMap.get(apiGlobalClass.name());
                                        apiPropertyMap.put(property.name(), property);
                                    } else {
                                        apiPropertyMap = new HashMap<>();
                                        apiPropertyMap.put(property.name(), property);
                                        returnGlobalClassMap.put(apiGlobalClass.name(), apiPropertyMap);
                                    }
                                }
                            }
                        }
                    }
                    //获取全局状态码配置
                    ApiGlobalCode[] apiGlobalCodes = mainApplicationClass.getAnnotationsByType(ApiGlobalCode.class);
                    if (apiGlobalClasses.length > 0) {
                        for (ApiGlobalCode apiGlobalCode : apiGlobalCodes) {
                            String name = apiGlobalCode.name();
                            String value = apiGlobalCode.value();
                            String description = apiGlobalCode.description();
                            DocCode docCode = new DocCode();
                            docCode.setName(name);
                            docCode.setValue(value);
                            docCode.setDescription(description);
                            docGlobalCodes.add(docCode);
                        }
                    }
                    //获取需要扫描的包
                    List<String> packages = takePackages(mainApplicationClass);
                    Set<Class<?>> classes = new HashSet<>();
                    for (String packageName : packages) {
                        scanner(packageName, classes);
                    }
                    //springboot中配置需要扫描的类
                    SpringBootApplication applicationClassAnnotation = mainApplicationClass.getAnnotation(SpringBootApplication.class);
                    Class<?>[] scanClasses = applicationClassAnnotation.scanBasePackageClasses();
                    if (scanClasses.length > 0) {
                        log.info("已获取applicationClassAnnotation配置中需要扫描的类:{}", Arrays.toString(scanClasses));
                        classes.addAll(Arrays.asList(scanClasses));
                    }
                    log.info("已获取到所有需要扫描的Controller类({})个：{}", classes.size(), classes);
                    //处理Controller类
                    Set<DocClass> docClasses = handleClass(classes);
                    doc = new Doc().setDocClasses(docClasses);

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
                            "                                                       1.0.0   \n");
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("======Api解析异常======");
        }
    }

    /**
     * 获取需要扫描的包
     */
    private List<String> takePackages(Class<?> mainApplicationClass) {
        List<String> packageNames = new ArrayList<>();
        SpringBootApplication applicationClassAnnotation = mainApplicationClass.getAnnotation(SpringBootApplication.class);
        String name = mainApplicationClass.getPackage().getName();
        packageNames.add(name);
        log.info("已获取启动类所在的包:{}", name);
        //springboot中配置需要扫描的包
        String[] packages = applicationClassAnnotation.scanBasePackages();
        if (packages.length > 0) {
            log.info("已获取applicationClassAnnotation配置中需要扫描的包:{}", Arrays.toString(packages));
            //合并所有需要扫描的包，防止重复扫描
            for (String packageName : packages) {
                if (packageName.length() > name.length()) {
                    if (packageName.startsWith(name)) {
                        //重复则跳过
                        continue;
                    }
                }
                if (name.startsWith(packageName)) {
                    continue;
                }
                packageNames.add(packageName);
            }
        }
        log.info("已获取到需要扫描的包：{}", packageNames);
        return packageNames;
    }

    /**
     * 递归扫描
     * 获取包含RestController注解的类
     *
     * @param packageName 目录名
     */
    private void scanner(String packageName, Set<Class<?>> classes) {
        String s = packageName.replace(".", "/");
        URL url = ApiContext.class.getClassLoader().getResource(s);
        if (url == null) {
            return;
        }
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 得到协议的名称
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            File[] files = new File(url.getFile()).listFiles();
            assert files != null;
            for (File classFile : files) {
                if (classFile.isDirectory()) {
                    scanner(packageName + "." + classFile.getName(), classes);
                    continue;
                }
                String path = classFile.getPath();
                if (path.endsWith(".class")) {
                    Class<?> clazz = getClazz(packageName + "." + classFile.getName().replace(".class", ""));
                    if (Objects.nonNull(clazz)) {
                        if (clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                            classes.add(clazz);
                        }
                    }
                }
            }
        } else if ("jar".equals(protocol)) {
            // 如果是jar包文件
            try {
                // 获取jar
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                // 从此jar包 得到一个枚举类
                Enumeration<JarEntry> entries = jar.entries();
                // 同样的进行循环迭代
                while (entries.hasMoreElements()) {
                    // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    // 如果是以/开头的
                    if (name.charAt(0) == '/') {
                        // 获取后面的字符串
                        name = name.substring(1);
                    }
                    // 如果前半部分和定义的包名相同
                    if (name.startsWith(packageDirName)) {
                        // 如果是一个.class文件 而且不是目录
                        if (name.endsWith(".class") && !entry.isDirectory()) {
                            Class<?> clazz = getClazz(name.replace("/", ".").replace("\\", ".")
                                    .replace(".class", ""));
                            if (Objects.nonNull(clazz)) {
                                if (clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                                    classes.add(clazz);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("扫描包[{}]出现异常", packageName);
            }
        }
    }

    /**
     * 创建类的定义
     *
     * @param beanClassName 类全名
     * @return 返回类的定义
     */
    private Class<?> getClazz(String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 处理扫描到的类
     *
     * @param classes 扫描到的类
     */
    public Set<DocClass> handleClass(Set<Class<?>> classes) {
        Set<DocClass> docClasses = new TreeSet<>();
        for (Class<?> aClass : classes) {
            if (aClass.getAnnotation(ApiIgnore.class) != null) {
                //不处理添加过滤注解的类
                continue;
            }
            DocClass docClass = new DocClass();
            ApiClass aClassAnnotation = aClass.getAnnotation(ApiClass.class);
            if (aClassAnnotation != null && StringUtils.isNotBlank(aClassAnnotation.value())) {
                //从注解获取描述
                docClass.setDescription(aClassAnnotation.value());
            } else {
                //注解获取不到描述则使用类名称
                docClass.setDescription(aClass.getSimpleName());
            }
            //获取类上的requestMapping
            RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
            //获取类上的所有公开方法
            Method[] methods = aClass.getDeclaredMethods();
            Set<DocMethod> docMethods = handleMethod(methods, requestMapping);
            docClass.setDocMethods(docMethods);
            docClasses.add(docClass);
        }
        return docClasses;
    }

    /**
     * 处理Controller中的方法
     *
     * @param methods        Controller中的方法
     * @param requestMapping Controller上的RequestMapping
     * @return 处理后的方法结果
     */
    public Set<DocMethod> handleMethod(Method[] methods, RequestMapping requestMapping) {
        Set<DocMethod> tempMethods = new TreeSet<>();
        Set<String> parentModeSet = new TreeSet<>();
        Set<String> parentUriSet = new TreeSet<>();
        if (requestMapping != null) {
            if (requestMapping.value().length > 0) {
                parentUriSet.addAll(Arrays.asList(requestMapping.value()));
            }
            if (requestMapping.method().length > 0) {
                for (RequestMethod requestMethod : requestMapping.method()) {
                    parentModeSet.add(requestMethod.name());
                }
            }
        }
        if (parentUriSet.isEmpty()) {
            //如果类上没有配置则增加一个空的地址，从而保证下面的代码正常执行
            parentUriSet.add("");
        }
        for (Method method : methods) {
            Set<String> modeSet = new TreeSet<>(parentModeSet);
            if (method.getAnnotation(ApiIgnore.class) != null) {
                //不处理添加过滤注解的方法
                continue;
            }
            TempMethod tempMethod = new TempMethod();
            //方法上的注解
            ApiMethod methodAnnotation = method.getAnnotation(ApiMethod.class);
            if (methodAnnotation != null && StringUtils.isNotBlank(methodAnnotation.value())) {
                //从注解获取描述
                tempMethod.setDescription(methodAnnotation.value());
            } else {
                //注解获取不到描述则使用方法名称
                tempMethod.setDescription(method.getName());
            }
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            RequestMapping requestMappingAnnotation = method.getAnnotation(RequestMapping.class);
            if (getMapping == null && postMapping == null && putMapping == null && deleteMapping == null && requestMappingAnnotation == null) {
                //这四种注解都不存在则直接跳过，不作处理
                continue;
            }
            Set<String> uriSet = new TreeSet<>();
            //获取请求方式
            if (getMapping != null) {
                modeSet.add(RequestMethod.GET.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : getMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (postMapping != null) {
                modeSet.add(RequestMethod.POST.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : postMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (putMapping != null) {
                modeSet.add(RequestMethod.PUT.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : putMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else if (deleteMapping != null) {
                modeSet.add(RequestMethod.DELETE.name());
                //加入到方法结果中
                for (String parentUri : parentUriSet) {
                    for (String value : deleteMapping.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            } else {
                //加入到方法结果中
                if (requestMappingAnnotation.method().length > 0) {
                    for (RequestMethod requestMethod : requestMappingAnnotation.method()) {
                        modeSet.add(requestMethod.name());
                    }
                } else {
                    modeSet.add(RequestMethod.GET.name());
                    modeSet.add(RequestMethod.HEAD.name());
                    modeSet.add(RequestMethod.POST.name());
                    modeSet.add(RequestMethod.PUT.name());
                    modeSet.add(RequestMethod.PATCH.name());
                    modeSet.add(RequestMethod.DELETE.name());
                    modeSet.add(RequestMethod.OPTIONS.name());
                    modeSet.add(RequestMethod.TRACE.name());
                }
                for (String parentUri : parentUriSet) {
                    for (String value : requestMappingAnnotation.value()) {
                        uriSet.add(parentUri + value);
                    }
                }
            }
            tempMethod.setModeSet(modeSet);
            tempMethod.setUriSet(uriSet);
            //方法上自定义入参的注解(两种)
            ApiMapParam apiMapParam = method.getAnnotation(ApiMapParam.class);
            //指定对象属性的注解
            ApiAppointParam apiAppointParam = method.getAnnotation(ApiAppointParam.class);
            //重新配置属性信息注解
            ApiParamModelProperty[] apiModelProperties = method.getAnnotationsByType(ApiParamModelProperty.class);
            //获取入参 jdk8以上开始支持
            Parameter[] parameters = method.getParameters();
            //处理入参注解
            try {
                DocModel paramDocModel = handleParam(apiMapParam, apiAppointParam, apiModelProperties, parameters);
                //入参加入到方法结果中
                tempMethod.setParamDocModel(paramDocModel);
            } catch (ParamException e) {
                log.error("方法{}{}", method.getName(), e.getMessage());
                //方法入参异常则跳过，不处理此方法
                continue;
            }
            //方法上自定义出参的注解
            ApiMapReturn apiMapReturn = method.getAnnotation(ApiMapReturn.class);
            //出参为基本数据类型的注解
            ApiBasicReturn apiBasicReturn = method.getAnnotation(ApiBasicReturn.class);
            //重新配置出参属性信息注解
            ApiReturnModelProperty[] apiReturnModelProperties = method.getAnnotationsByType(ApiReturnModelProperty.class);
            //获取出参
            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            DocModel returnDocModel = handleReturn(apiMapReturn, apiBasicReturn, returnType, genericReturnType, apiReturnModelProperties);
            //出参加入到方法结果中
            tempMethod.setReturnDocModel(returnDocModel);
            //获取method上的状态码配置集合
            Set<DocCode> docCodes = new TreeSet<>();
            ApiCode[] apiCodeAnnotations = method.getAnnotationsByType(ApiCode.class);
            if (apiCodeAnnotations.length > 0) {
                for (ApiCode apiCodeAnnotation : apiCodeAnnotations) {
                    String name = apiCodeAnnotation.name();
                    String value = apiCodeAnnotation.value();
                    String description = apiCodeAnnotation.description();
                    DocCode docCode = new DocCode();
                    docCode.setName(name);
                    docCode.setValue(value);
                    docCode.setDescription(description);
                    docCodes.add(docCode);

                }
            }
            //合并全局状态码
            docCodes.addAll(docGlobalCodes);
            //状态码设置到apiMethod中
            tempMethod.setDocCodes(docCodes);
            //TempMethod转DocMethod
            DocMethod docMethod = new DocMethod();
            docMethod.setDescription(tempMethod.getDescription());
            docMethod.setModeSet(tempMethod.getModeSet());
            docMethod.setUriSet(tempMethod.getUriSet());
            docMethod.setParams(tempMethod.getParams());
            docMethod.setParamJson(tempMethod.getParamJson());
            docMethod.setParamExample(tempMethod.getParamExample());
            docMethod.setReturnJson(tempMethod.getReturnJson());
            docMethod.setDocCodes(tempMethod.getDocCodes());
            tempMethods.add(docMethod);
        }
        return tempMethods;
    }

    /**
     * 处理方法的入参
     *
     * @param apiMapParam        自定义方法的入参注解
     * @param apiAppointParam    指定对象属性的注解
     * @param apiModelProperties 重新配置入参属性信息注解
     * @param parameters         方法的入参
     * @return 处理后的入参模型
     */
    public DocModel handleParam(ApiMapParam apiMapParam, ApiAppointParam
            apiAppointParam, ApiParamModelProperty[] apiModelProperties, Parameter[] parameters) throws ParamException {
        DocModel docModel = new DocModel();
        Map<String, ApiParamModelProperty> apiModelPropertyMap = new HashMap<>();
        if (apiModelProperties != null && apiModelProperties.length > 0) {
            for (ApiParamModelProperty apiParamModelProperty : apiModelProperties) {
                String name = apiParamModelProperty.name();
                apiModelPropertyMap.put(name, apiParamModelProperty);
            }
        }
        int num = 0;
        docModel.setForm("form-data");
        //自定义入参对象属性
        Map<String, Boolean> propertyMap = new HashMap<>();
        if (apiAppointParam != null) {
            String[] requires = apiAppointParam.require();
            String[] nonRequires = apiAppointParam.nonRequire();
            for (String nonRequire : nonRequires) {
                propertyMap.put(nonRequire, false);
            }
            //如果必须和非必须的同时配置了相同的属性，则必须覆盖非必须
            for (String require : requires) {
                propertyMap.put(require, true);
            }
        }
        for (Parameter parameter : parameters) {
            if (parameter.getAnnotation(ApiIgnore.class) != null) {
                //不处理添加过滤注解的参数
                continue;
            }
            if (parameter.getType() == HttpSession.class || parameter.getType() == HttpServletRequest.class
                    || parameter.getType() == HttpServletResponse.class) {
                //不处理这些类
                continue;
            }
            //对于map类型的入参 用ApiMapParam 来处理
            Map<String, ApiParam> apiMapParamMap = new HashMap<>();
            if (apiMapParam != null && apiMapParam.value().length > 0) {
                for (ApiParam apiParam : apiMapParam.value()) {
                    String value = apiParam.name();
                    apiMapParamMap.put(value, apiParam);
                }
            }
            //循环依赖收集集合
            Set<Class<?>> repeats = new HashSet<>();
            //json形式入参的标识RequestBody
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            DocProperty docProperty = new DocProperty();
            if (requestBody != null) {
                if (num > 0) {
                    //入参中RequestBody的数量不能超过1
                    throw new ParamException("入参中@RequestBody的数量超过1");
                }
                num++;
                //如果是json,那么肯定不是基本数据类型，直接调用handleModel()
                docProperty = handleModel(docProperty, parameter.getType(), parameter.getParameterizedType(), propertyMap, true, true, repeats, apiModelPropertyMap, null, null, apiMapParamMap, null, null, null);
                if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
                    return docModel;
                }
                //借用apiProperty为壳返回apiModel
                docModel = docProperty.getDocModel();
                //标记为非基本数据类型
                docModel.setForm("json");
                docModel.setType(docProperty.getType());
            }
            if (num < 1) {
                //非json则为form-data形式入参
                docProperty = handleModel(docProperty, parameter.getType(), parameter.getParameterizedType(), propertyMap, true, true, repeats, apiModelPropertyMap, null, null, apiMapParamMap, null, null, null);
                if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
                    return docModel;
                }
                //借用apiProperty为壳返回apiModel
                if (docProperty.getDocModel() == null) {
                    //为空表示为基本数据类型
                    //从注解中获取配置信息
                    ApiParam parameterAnnotation = parameter.getAnnotation(ApiParam.class);
                    PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                    if (pathVariable != null) {
                        String value = pathVariable.value();
                        if (StringUtils.isNotBlank(value)) {
                            docProperty.setName(value);
                        } else {
                            docProperty.setName(parameter.getName());
                        }
                        docProperty.setRequited(true);
                    }
                    if (parameterAnnotation != null) {
                        String value = parameterAnnotation.name();
                        //PathVariable设置名称的优先级最高
                        if (StringUtils.isBlank(docProperty.getName())) {
                            if (StringUtils.isNotBlank(value)) {
                                docProperty.setName(value);
                            } else {
                                docProperty.setName(parameter.getName());
                            }
                        }
                        String description = parameterAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            docProperty.setDescription(description);
                        }
                        String defaultValue = parameterAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            docProperty.setDefaultValue(defaultValue);
                        }
                        boolean required = parameterAnnotation.required();
                        if (docProperty.getRequited() == null) {
                            docProperty.setRequited(required);
                        }
                        String example = parameterAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            docProperty.setExample(example);
                        }
                    } else {
                        //未配置则使用参数名称
                        docProperty.setName(parameter.getName());
                    }
                    docModel.getApiProperties().add(docProperty);
                    //标记为基本数据类型
                    docModel.setType(docProperty.getType());
                } else {
                    //非基本数据类型则合并参数
                    docModel.getApiProperties().addAll(docProperty.getDocModel().getApiProperties());
                    //标记为非基本数据类型
                    docModel.setType(docProperty.getType());
                }
            }
        }
        return docModel;
    }

    /**
     * 处理方法的出参
     *
     * @param apiMapReturn             自定义方法的出参
     * @param apiBasicReturn           自定义基本数据类型出参
     * @param returnType               方法的出参类型
     * @param genericReturnType        方法的出参类型
     * @param apiReturnModelProperties 重新配置出参属性信息注解
     * @return 处理后的出参模型
     */
    public DocModel handleReturn(ApiMapReturn apiMapReturn, ApiBasicReturn
            apiBasicReturn, Class<?> returnType, Type genericReturnType, ApiReturnModelProperty[] apiReturnModelProperties) {
        Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap = new HashMap<>();
        if (apiReturnModelProperties != null && apiReturnModelProperties.length > 0) {
            for (ApiReturnModelProperty apiReturnModelProperty : apiReturnModelProperties) {
                String name = apiReturnModelProperty.name();
                apiReturnModelPropertyMap.put(name, apiReturnModelProperty);
            }
        }
        //对于map类型的出参 用ApiMapParam 来处理
        Map<String, ApiReturn> apiMapReturnMap = new HashMap<>();
        if (apiMapReturn != null && apiMapReturn.value().length > 0) {
            for (ApiReturn apiReturn : apiMapReturn.value()) {
                String value = apiReturn.name();
                apiMapReturnMap.put(value, apiReturn);
            }
        }
        //循环依赖收集集合
        Set<Class<?>> repeats = new HashSet<>();
        DocProperty docProperty = new DocProperty();
        docProperty = handleModel(docProperty, returnType, regenerateType(genericReturnType), new HashMap<>(), false, true, repeats, null, apiReturnModelPropertyMap, null, null, apiMapReturnMap, null, null);
        DocModel docModel = new DocModel();
        if (StringUtils.isBlank(docProperty.getName()) && StringUtils.isBlank(docProperty.getType()) && docProperty.getDocModel() == null) {
            return docModel;
        }
        //借用apiProperty为壳返回apiModel
        //借用apiProperty为壳返回apiModel
        if (docProperty.getDocModel() == null) {
            if (apiBasicReturn != null) {
                String description = apiBasicReturn.description();
                if (StringUtils.isNotBlank(description)) {
                    docProperty.setDescription(description);
                }
                String format = apiBasicReturn.format();
                if (StringUtils.isNotBlank(format)) {
                    docProperty.setFormat(format);
                }
            }
            docModel.getApiProperties().add(docProperty);
            //标记为基本数据类型
            docModel.setType(docProperty.getType());
        } else {
            //非基本数据类型则合并参数
            docModel.getApiProperties().addAll(docProperty.getDocModel().getApiProperties());
            //标记为非基本数据类型
            docModel.setType(docProperty.getType());
        }
        return docModel;
    }

    /**
     * 处理模型
     *
     * @param docProperty                属性类
     * @param aClass                     类
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param repeats                    循环依赖集合
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          Map类型属性注解集合
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型出参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleModel(DocProperty docProperty,
                                   Class<?> aClass, Type type, Map<String, Boolean> propertyMap,
                                   boolean isParam, boolean isJson, Set<Class<?>> repeats,
                                   Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                   Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                   Map<String, ApiMapProperty> apiMapPropertyMap,
                                   Map<String, ApiParam> apiMapParamMap,
                                   Map<String, ApiReturn> apiMapReturnMap, Map<String, ApiProperty> paramGlobalApiPropertyMap, Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        //先判断是否为循环依赖
        if (repeats.contains(aClass)) {
            //如为循环依赖则直接返回
            return docProperty;
        }
        if (Map.class.isAssignableFrom(aClass)) {
            //处理Map
            return handleMap(docProperty, type, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (Collection.class.isAssignableFrom(aClass)) {
            //处理集合
            docProperty.setType(docProperty.getType() + "[0]");
            return handleGeneric(docProperty, type, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (aClass.isArray()) {
            //处理数组
            docProperty.setType(docProperty.getType() + "[0]");
            return handleGeneric(docProperty, aClass.getComponentType(), propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        } else if (int.class.isAssignableFrom(aClass)
                || long.class.isAssignableFrom(aClass)
                || double.class.isAssignableFrom(aClass)
                || float.class.isAssignableFrom(aClass)
                || short.class.isAssignableFrom(aClass)
                || boolean.class.isAssignableFrom(aClass)
                || aClass.getPackage().getName().startsWith("java.lang")
                || Date.class.isAssignableFrom(aClass)
                || LocalDateTime.class.isAssignableFrom(aClass)
                || LocalDate.class.isAssignableFrom(aClass)
                || MultipartFile.class.isAssignableFrom(aClass)
                || LocalTime.class.isAssignableFrom(aClass)) {
            //处理基本数据类型
            String typeString = docProperty.getType();
            docProperty.setType(baseTypeMap.get(aClass) + typeString);
            return docProperty;
        } else {
            //先从模型集合中取，取不到则进行解析
            //指定对象属性的除外，泛型的也除外,重新指定对象属性信息的也除外,全局配置的也除外
            if (!paramGlobalClassMap.containsKey(aClass) && !returnGlobalClassMap.containsKey(aClass) && apiModelPropertyMap != null && apiReturnModelPropertyMap != null && propertyMap.isEmpty() && apiModelMap.containsKey(aClass) && !(type != null && aClass.getTypeParameters().length > 0)) {
                //取到后直接返回
                docProperty.setDocModel(apiModelMap.get(aClass));
                return docProperty;
            }
            //如果属性为对象，但是属性未配置注解描述的，取类上的注解为属性描述
            if (StringUtils.isBlank(docProperty.getDescription())) {
                ApiModel apiModel = aClass.getAnnotation(ApiModel.class);
                if (apiModel != null) {
                    String value = apiModel.value();
                    if (StringUtils.isNotBlank(value)) {
                        docProperty.setDescription(value);
                    }
                }
            }
            DocModel docModel = new DocModel();
            Set<DocProperty> apiProperties = new TreeSet<>();
            //处理类类型
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                ApiIgnore apiIgnore = method.getAnnotation(ApiIgnore.class);
                if (apiIgnore != null) {
                    //不处理添加过滤注解的属性
                    continue;
                }
                String methodName = method.getName();
                if ("getClass".equals(methodName)) {
                    //不是getClass()方法直接跳过
                    continue;
                }
                if (!methodName.startsWith("set") && !methodName.startsWith("get") && !methodName.startsWith("is")) {
                    //不是get/set/is形式的方法直接跳过
                    continue;
                }
                if (isParam) {
                    if (methodName.startsWith("set")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("set", ""));
                    } else {
                        continue;
                    }
                } else {
                    if (methodName.startsWith("get")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("get", ""));
                    } else if (methodName.startsWith("is")) {
                        methodName = StringUtils.uncapitalize(methodName.replaceFirst("is", ""));
                    } else {
                        continue;
                    }
                }
                DocProperty property = new DocProperty();
                Map<String, Boolean> childPropertyMap = new HashMap<>();
                if (!propertyMap.isEmpty()) {
                    Set<String> strings = propertyMap.keySet();
                    //通过状态
                    boolean pass = false;
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childPropertyMap.put(string.replaceFirst(methodName + ".", ""), propertyMap.get(string));
                                pass = true;
                            }
                        } else if (string.equals(methodName)) {
                            pass = true;
                        }
                    }
                    if (!pass) {
                        continue;
                    }
                    property.setRequited(propertyMap.get(methodName));
                }
                //全局针对多级配置
                Map<String, ApiProperty> childParamGlobalApiPropertyMap = new HashMap<>();
                if (paramGlobalApiPropertyMap != null && !paramGlobalApiPropertyMap.isEmpty()) {
                    Set<String> strings = paramGlobalApiPropertyMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childParamGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), paramGlobalApiPropertyMap.get(string));
                            } else {
                                childParamGlobalApiPropertyMap.put(string, paramGlobalApiPropertyMap.get(string));
                            }
                        } else if (string.equals(methodName)) {
                            ApiProperty apiPropertyConf = paramGlobalApiPropertyMap.get(methodName);
                            String description = apiPropertyConf.description();
                            if (StringUtils.isNotBlank(description)) {
                                property.setDescription(description);
                            }
                            String defaultValue = apiPropertyConf.defaultValue();
                            if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                property.setDefaultValue(defaultValue);
                            }
                            String example = apiPropertyConf.example();
                            if (StringUtils.isNotBlank(example)) {
                                property.setExample(example);
                            }
                            boolean required = apiPropertyConf.required();
                            if (property.getRequited() == null) {
                                property.setRequited(required);
                            }
                        }
                    }
                }
                //全局针对多级配置
                Map<String, ApiProperty> childReturnGlobalApiPropertyMap = new HashMap<>();
                if (returnGlobalApiPropertyMap != null && !returnGlobalApiPropertyMap.isEmpty()) {
                    Set<String> strings = returnGlobalApiPropertyMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childReturnGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), returnGlobalApiPropertyMap.get(string));
                            } else {
                                childReturnGlobalApiPropertyMap.put(string, returnGlobalApiPropertyMap.get(string));
                            }
                        } else if (string.equals(methodName)) {
                            ApiProperty apiPropertyConf = returnGlobalApiPropertyMap.get(methodName);
                            String description = apiPropertyConf.description();
                            if (StringUtils.isNotBlank(description)) {
                                property.setDescription(description);
                            }
                            String defaultValue = apiPropertyConf.defaultValue();
                            if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                property.setDefaultValue(defaultValue);
                            }
                            String example = apiPropertyConf.example();
                            if (StringUtils.isNotBlank(example)) {
                                property.setExample(example);
                            }
                            boolean required = apiPropertyConf.required();
                            if (property.getRequited() == null) {
                                property.setRequited(required);
                            }
                        }
                    }
                }
                //判断是否为全局类配置
                if (isParam) {
                    //入参

                    if (paramGlobalClassMap.containsKey(aClass)) {
                        Map<String, ApiProperty> apiPropertyMap = paramGlobalClassMap.get(aClass);
                        Set<String> strings = apiPropertyMap.keySet();
                        //是否包含
                        boolean contain = false;
                        for (String string : strings) {
                            if (string.contains(".")) {
                                if (string.startsWith(methodName + ".")) {
                                    childParamGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiPropertyMap.get(string));
                                    contain = true;
                                }
                            } else if (string.equals(methodName)) {
                                contain = true;
                                ApiProperty apiPropertyConf = apiPropertyMap.get(methodName);
                                String description = apiPropertyConf.description();
                                if (StringUtils.isNotBlank(description)) {
                                    property.setDescription(description);
                                }
                                String defaultValue = apiPropertyConf.defaultValue();
                                if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                    property.setDefaultValue(defaultValue);
                                }
                                String example = apiPropertyConf.example();
                                if (StringUtils.isNotBlank(example)) {
                                    property.setExample(example);
                                }
                                boolean required = apiPropertyConf.required();
                                if (property.getRequited() == null) {
                                    property.setRequited(required);
                                }
                            }
                        }
                        if (!contain) {
                            continue;
                        }
                        String value = paramGlobalClassDescriptionMap.get(aClass);
                        if (StringUtils.isNotBlank(value)) {
                            docProperty.setDescription(value);
                        }
                    }
                } else {
                    //出参

                    if (returnGlobalClassMap.containsKey(aClass)) {
                        Map<String, ApiProperty> apiPropertyMap = returnGlobalClassMap.get(aClass);
                        Set<String> strings = apiPropertyMap.keySet();
                        //是否包含
                        boolean contain = false;
                        for (String string : strings) {
                            if (string.contains(".")) {
                                if (string.startsWith(methodName + ".")) {
                                    childReturnGlobalApiPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiPropertyMap.get(string));
                                    contain = true;
                                }
                            } else if (string.equals(methodName)) {
                                contain = true;
                                ApiProperty apiPropertyConf = apiPropertyMap.get(methodName);
                                String description = apiPropertyConf.description();
                                if (StringUtils.isNotBlank(description)) {
                                    property.setDescription(description);
                                }
                                String defaultValue = apiPropertyConf.defaultValue();
                                if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                    property.setDefaultValue(defaultValue);
                                }
                                String example = apiPropertyConf.example();
                                if (StringUtils.isNotBlank(example)) {
                                    property.setExample(example);
                                }
                                boolean required = apiPropertyConf.required();
                                if (property.getRequited() == null) {
                                    property.setRequited(required);
                                }
                            }
                        }

                        if (!contain) {
                            continue;
                        }
                        String value = returnGlobalClassDescriptionMap.get(aClass);
                        if (StringUtils.isNotBlank(value)) {
                            docProperty.setDescription(value);
                        }
                    }
                }
                //Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap
                //复制入参Map类型注解信息
                Map<String, ApiParam> childApiMapParamMap = null;
                if (apiMapParamMap != null && !apiMapParamMap.isEmpty()) {
                    childApiMapParamMap = new HashMap<>();
                    Set<String> strings = apiMapParamMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childApiMapParamMap.put(string.replaceFirst(methodName + ".", ""), apiMapParamMap.get(string));
                            }
                        }
                    }
                }
                //复制出参Map类型注解信息
                Map<String, ApiReturn> childApiMapReturnMap = null;
                if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty()) {
                    childApiMapReturnMap = new HashMap<>();
                    Set<String> strings = apiMapReturnMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(methodName + ".")) {
                                childApiMapReturnMap.put(string.replaceFirst(methodName + ".", ""), apiMapReturnMap.get(string));
                            }
                        }
                    }
                }
                //对于map类型的属性
                //方法上的Map注解
                apiMapPropertyMap = new HashMap<>();
                ApiMapProperty[] apiMapProperties = method.getAnnotationsByType(ApiMapProperty.class);
                if (apiMapProperties != null && apiMapProperties.length > 0) {
                    for (ApiMapProperty apiMapProperty : apiMapProperties) {
                        String value = apiMapProperty.name();
                        apiMapPropertyMap.put(value, apiMapProperty);
                    }
                }
                //方法上的注解
                ApiProperty apiPropertyAnnotation = method.getAnnotation(ApiProperty.class);
                //format-data形式的时间格式化注解
                DateTimeFormat dateTimeFormat = method.getAnnotation(DateTimeFormat.class);
                //json形式的时间格式化注解
                JsonFormat jsonFormat = method.getAnnotation(JsonFormat.class);
                //jsonProperty注解
                JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
                if (underscore) {
                    //如果启用下划线命名，则转换为下划线命名
                    property.setName(humpToLine(methodName));
                } else {
                    property.setName(methodName);
                }
                try {
                    Field field = getParentField(aClass, methodName);
                    if (field == null) {
                        throw new NoSuchFieldException();
                    }
                    apiIgnore = field.getAnnotation(ApiIgnore.class);
                    if (apiIgnore != null) {
                        //不处理添加过滤注解的属性
                        continue;
                    }
                    if (apiMapPropertyMap.isEmpty()) {
                        //对于map类型的属性
                        //属性上的Map注解
                        apiMapProperties = field.getAnnotationsByType(ApiMapProperty.class);
                        if (apiMapProperties != null && apiMapProperties.length > 0) {
                            for (ApiMapProperty apiMapProperty : apiMapProperties) {
                                String value = apiMapProperty.name();
                                apiMapPropertyMap.put(value, apiMapProperty);
                            }
                        }
                    }
                    if (apiPropertyAnnotation == null) {
                        //属性上的注解
                        apiPropertyAnnotation = field.getAnnotation(ApiProperty.class);
                    }
                    //format-data形式的时间格式化注解
                    if (dateTimeFormat == null) {
                        //属性上的注解
                        dateTimeFormat = field.getAnnotation(DateTimeFormat.class);
                    }
                    //json形式的时间格式化注解
                    if (jsonFormat == null) {
                        //属性上的注解
                        jsonFormat = field.getAnnotation(JsonFormat.class);
                    }
                    //get/set方法上的注解优先级高
                    if (apiPropertyAnnotation != null) {
                        String value = apiPropertyAnnotation.name();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                        String description = apiPropertyAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            property.setDescription(description);
                        }
                        String defaultValue = apiPropertyAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            property.setDefaultValue(defaultValue);
                        }
                        if (propertyMap.isEmpty()) {
                            boolean required = apiPropertyAnnotation.required();
                            property.setRequited(required);
                        }
                        String example = apiPropertyAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            property.setExample(example);
                        }
                    }
                    //jsonProperty注解
                    if (jsonProperty == null) {
                        jsonProperty = method.getAnnotation(JsonProperty.class);
                    }
                    if (jsonProperty != null) {
                        String value = jsonProperty.value();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                    }
                    if (isParam && method.getName().startsWith("set")) {
                        Parameter[] parameters = method.getParameters();
                        if (!StringUtils.equals(field.getGenericType().getTypeName(), parameters[0].getParameterizedType().getTypeName())) {
                            //在set方法中如果入参类型和属性类型不一致，则不进行处理
                            continue;
                        }
                        //复制重新定义属性信息Map
                        Map<String, ApiParamModelProperty> childApiModelPropertyMap = null;
                        if (apiModelPropertyMap != null && !apiModelPropertyMap.isEmpty()) {
                            childApiModelPropertyMap = new HashMap<>();
                            Set<String> strings = apiModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiParamModelProperty modelProperty = apiModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                    boolean required = modelProperty.required();
                                    property.setRequited(required);
                                    String defaultValue = modelProperty.defaultValue();
                                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                        property.setDefaultValue(defaultValue);
                                    }
                                    String example = modelProperty.example();
                                    if (StringUtils.isNotBlank(example)) {
                                        property.setExample(example);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        DocProperty returnDocProperty;
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Class<?> paramClass = parameters[0].getType();
                            Type realType = realType(parameters[0].getParameterizedType(), aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                paramClass = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                paramClass = (Class<?>) realType;
                            }

                            returnDocProperty = handleModel(property, paramClass, realType, childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            returnDocProperty = handleModel(property, parameters[0].getType(), parameters[0].getParameterizedType(), childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (returnDocProperty.getType().contains("date") || returnDocProperty.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (!isJson && dateTimeFormat != null) {
                                //form-data形式的时间格式
                                String pattern = dateTimeFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (isJson) {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    returnDocProperty.setFormat(this.jsonFormat);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(dateFormat)) {
                                    returnDocProperty.setFormat(dateFormat);
                                }
                            }
                        }
                        apiProperties.add(returnDocProperty);
                    } else if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                        Class<?> returnType = method.getReturnType();
                        Type genericReturnType = method.getGenericReturnType();
                        if (!StringUtils.equals(field.getGenericType().getTypeName(), genericReturnType.getTypeName())) {
                            //在get方法中如果返回参数类型和属性类型不一致，则不进行处理
                            continue;
                        }
                        //复制重新定义属性信息Map
                        Map<String, ApiReturnModelProperty> childApiReturnModelPropertyMap = null;
                        if (apiReturnModelPropertyMap != null && !apiReturnModelPropertyMap.isEmpty()) {
                            childApiReturnModelPropertyMap = new HashMap<>();
                            Set<String> strings = apiReturnModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiReturnModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiReturnModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiReturnModelProperty modelProperty = apiReturnModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Type realType = realType(genericReturnType, aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                returnType = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                returnType = (Class<?>) realType;
                            }
                            property = handleModel(property, returnType, realType, childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            property = handleModel(property, returnType, regenerateType(genericReturnType), childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (property.getType().contains("date") || property.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    property.setFormat(pattern);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    property.setFormat(this.jsonFormat);
                                }
                            }
                        }
                        apiProperties.add(property);
                    }
                } catch (NoSuchFieldException exception) {
                    //get/set方法上的注解优先级高
                    if (apiPropertyAnnotation != null) {
                        String value = apiPropertyAnnotation.name();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                        String description = apiPropertyAnnotation.description();
                        if (StringUtils.isNotBlank(description)) {
                            property.setDescription(description);
                        }
                        String defaultValue = apiPropertyAnnotation.defaultValue();
                        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                            property.setDefaultValue(defaultValue);
                        }
                        if (propertyMap.isEmpty()) {
                            boolean required = apiPropertyAnnotation.required();
                            property.setRequited(required);
                        }
                        String example = apiPropertyAnnotation.example();
                        if (StringUtils.isNotBlank(example)) {
                            property.setExample(example);
                        }
                    }
                    if (jsonProperty != null) {
                        String value = jsonProperty.value();
                        if (StringUtils.isNotBlank(value)) {
                            property.setName(value);
                        }
                    }
                    if (isParam && method.getName().startsWith("set")) {
                        Parameter[] parameters = method.getParameters();
                        //复制重新定义属性信息Map
                        Map<String, ApiParamModelProperty> childApiModelPropertyMap = null;
                        if (apiModelPropertyMap != null && !apiModelPropertyMap.isEmpty()) {
                            childApiModelPropertyMap = new HashMap<>();
                            Set<String> strings = apiModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiParamModelProperty modelProperty = apiModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                    boolean required = modelProperty.required();
                                    property.setRequited(required);
                                    String defaultValue = modelProperty.defaultValue();
                                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                                        property.setDefaultValue(defaultValue);
                                    }
                                    String example = modelProperty.example();
                                    if (StringUtils.isNotBlank(example)) {
                                        property.setExample(example);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        DocProperty returnDocProperty;
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Class<?> paramClass = parameters[0].getType();
                            Type realType = realType(parameters[0].getParameterizedType(), aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                paramClass = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                paramClass = (Class<?>) realType;
                            }
                            returnDocProperty = handleModel(property, paramClass, realType, childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            returnDocProperty = handleModel(property, parameters[0].getType(), parameters[0].getParameterizedType(), childPropertyMap, isParam, isJson, copyRepeats, childApiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (returnDocProperty.getType().contains("date") || returnDocProperty.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (!isJson && dateTimeFormat != null) {
                                //form-data形式的时间格式
                                String pattern = dateTimeFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    returnDocProperty.setFormat(pattern);
                                }
                            } else if (isJson) {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    returnDocProperty.setFormat(this.jsonFormat);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(dateFormat)) {
                                    returnDocProperty.setFormat(dateFormat);
                                }
                            }
                        }
                        apiProperties.add(returnDocProperty);
                    } else if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                        Class<?> returnType = method.getReturnType();
                        Type genericReturnType = method.getGenericReturnType();
                        //复制重新定义属性信息Map
                        Map<String, ApiReturnModelProperty> childApiReturnModelPropertyMap = null;
                        if (apiReturnModelPropertyMap != null && !apiReturnModelPropertyMap.isEmpty()) {
                            childApiReturnModelPropertyMap = new HashMap<>();
                            Set<String> strings = apiReturnModelPropertyMap.keySet();
                            for (String string : strings) {
                                if (string.contains(".")) {
                                    if (string.startsWith(methodName + ".")) {
                                        childApiReturnModelPropertyMap.put(string.replaceFirst(methodName + ".", ""), apiReturnModelPropertyMap.get(string));
                                    }
                                } else if (string.equals(methodName)) {
                                    ApiReturnModelProperty modelProperty = apiReturnModelPropertyMap.get(string);
                                    //重新定义属性信息
                                    String description = modelProperty.description();
                                    if (StringUtils.isNotBlank(description)) {
                                        property.setDescription(description);
                                    }
                                }
                            }
                        }
                        //赋值循环依赖集合
                        Set<Class<?>> copyRepeats = new HashSet<>(repeats);
                        //把自身放进去
                        copyRepeats.add(aClass);
                        //处理类上的泛型
                        if (type instanceof ParameterizedType && aClass.getTypeParameters().length > 0) {
                            Type realType = realType(genericReturnType, aClass.getTypeParameters(), type);
                            if (realType instanceof ParameterizedType) {
                                returnType = (Class<?>) ((ParameterizedType) realType).getRawType();
                            } else if (realType instanceof Class) {
                                returnType = (Class<?>) realType;
                            }
                            property = handleModel(property, returnType, realType, childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        } else {
                            property = handleModel(property, returnType, regenerateType(genericReturnType), childPropertyMap, isParam, isJson, copyRepeats, apiModelPropertyMap, childApiReturnModelPropertyMap, apiMapPropertyMap, childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                        }
                        if (property.getType().contains("date") || property.getType().contains("time")) {
                            if (isJson && jsonFormat != null) {
                                //json形式的时间格式
                                String pattern = jsonFormat.pattern();
                                if (StringUtils.isNotBlank(pattern)) {
                                    property.setFormat(pattern);
                                }
                            } else {
                                //使用全局配置的
                                if (StringUtils.isNotBlank(this.jsonFormat)) {
                                    property.setFormat(this.jsonFormat);
                                }
                            }
                        }
                        apiProperties.add(property);
                    }
                }
            }
            docModel.setApiProperties(apiProperties);
            docProperty.setDocModel(docModel);
            //解析完成后存入到模型集合中
            //指定对象属性的除外，泛型的也除外,重新指定对象属性信息的也除外
            if (!paramGlobalClassMap.containsKey(aClass) && !returnGlobalClassMap.containsKey(aClass) && apiModelPropertyMap != null && apiReturnModelPropertyMap != null && propertyMap.isEmpty() && !(type != null && aClass.getTypeParameters().length > 0)) {
                apiModelMap.put(aClass, docModel);
            }
            return docProperty;
        }
    }

    /**
     * 获取父类中的属性
     *
     * @param clazz     当前类
     * @param fieldName 属性名
     * @return 获取到到属性
     */
    public Field getParentField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        try {
            try {
                //首字符小写先进行获取获取不到用首字母大写在进行获取
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                //首字符小写先进行获取获取不到用首字母大写在进行获取
                return clazz.getDeclaredField(StringUtils.capitalize(fieldName));
            }
        } catch (NoSuchFieldException exception) {
            return getParentField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * 对包含泛型的属性类型进行重新包装
     *
     * @param type           属性类型
     * @param typeParameters 当前类的泛型集合
     * @param genericType    当前类的真实返回类型
     * @return 包装后的真实类型
     */
    public Type realType(Type type, TypeVariable<? extends Class<?>>[] typeParameters, Type genericType) {
        if (type instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) type;
            Type rawType = pType.getRawType();
            List<Type> types = new ArrayList<>();
            for (Type actualTypeArgument : pType.getActualTypeArguments()) {
                types.add(realType(actualTypeArgument, typeParameters, genericType));
            }
            return ParameterizedTypeImpl.make((Class<?>) rawType, types.toArray(new Type[0]), null);
        } else {
            for (int i = 0; i < typeParameters.length; i++) {
                if (type.getTypeName().equals(typeParameters[i].getName())) {
                    return ((ParameterizedType) genericType).getActualTypeArguments()[i];
                }
            }
            return type;
        }
    }

    /**
     * 处理泛型
     *
     * @param docProperty                属性类
     * @param gType                      类型
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param repeats                    循环依赖集合
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          Map类型属性注解集合
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型出参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleGeneric(DocProperty docProperty, Type gType,
                                     Map<String, Boolean> propertyMap, boolean isParam, boolean isJson,
                                     Set<Class<?>> repeats, Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                     Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                     Map<String, ApiMapProperty> apiMapPropertyMap,
                                     Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap, Map<String, ApiProperty> paramGlobalApiPropertyMap, Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        // 如果gType类型是ParameterizedType对象
        if (gType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) gType;
            // 取得泛型类型的泛型参数
            Type[] tArgs = pType.getActualTypeArguments();
            Type tArg = tArgs[0];
            if (tArg instanceof Class && ((Class<?>) tArg).isArray()) {
                //处理数组
                docProperty.setType(docProperty.getType() + "[0]");
                return handleGeneric(docProperty, ((Class<?>) tArg).getComponentType(), propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            } else if (tArg instanceof ParameterizedType && Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) tArg).getRawType())) {
                //处理集合
                docProperty.setType(docProperty.getType() + "[0]");
                return handleGeneric(docProperty, tArg, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            } else {
                if (!(tArg instanceof Class) && !(tArg instanceof ParameterizedType)) {
                    //泛型不为类也不是基本数据类型的不作处理
                    return docProperty;
                }
                //处理类
                Class<?> aClass = (tArg instanceof Class) ? (Class<?>) tArg : (Class<?>) ((ParameterizedType) tArg).getRawType();
                return handleModel(docProperty, aClass, tArg, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
            }
        } else {
            return handleModel(docProperty, (Class<?>) gType, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, apiMapParamMap, apiMapReturnMap, paramGlobalApiPropertyMap, returnGlobalApiPropertyMap);
        }
    }

    /**
     * 重新生成符合泛型形式的Type
     * 用于返回形式的泛型
     *
     * @param returnType 返回的Type
     * @return 符合泛型形式的Type
     */
    public Type regenerateType(Type returnType) {
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
     * 处理Map
     *
     * @param docProperty                属性类
     * @param gType                      类型
     * @param propertyMap                自定义入参对象属性
     * @param isParam                    是否是入参
     * @param isJson                     是否是json
     * @param apiModelPropertyMap        重新配置入参属性信息注解
     * @param apiReturnModelPropertyMap  重新配置出参属性信息注解
     * @param apiMapPropertyMap          类属性为Map的注解
     * @param apiMapParamMap             Map类型入参注解集合
     * @param apiMapReturnMap            Map类型入参注解集合
     * @param paramGlobalApiPropertyMap  全局配置入参类的属性集合
     * @param returnGlobalApiPropertyMap 全局配置出参类的属性集合
     * @return 处理后的模型属性
     */
    public DocProperty handleMap(DocProperty docProperty,
                                 Type gType, Map<String, Boolean> propertyMap, boolean isParam, boolean isJson,
                                 Set<Class<?>> repeats, Map<String, ApiParamModelProperty> apiModelPropertyMap,
                                 Map<String, ApiReturnModelProperty> apiReturnModelPropertyMap,
                                 Map<String, ApiMapProperty> apiMapPropertyMap,
                                 Map<String, ApiParam> apiMapParamMap, Map<String, ApiReturn> apiMapReturnMap, Map<String, ApiProperty> paramGlobalApiPropertyMap, Map<String, ApiProperty> returnGlobalApiPropertyMap) {
        if ((apiMapPropertyMap == null || apiMapPropertyMap.isEmpty()) && (apiMapParamMap == null || apiMapParamMap.isEmpty()) && (apiMapReturnMap == null || apiMapReturnMap.isEmpty())) {
            return docProperty;
        }
        //全局针对多级配置
        Map<String, ApiProperty> childParamGlobalApiPropertyMap = new HashMap<>();
        if (paramGlobalApiPropertyMap != null && !paramGlobalApiPropertyMap.isEmpty()) {
            Set<String> strings = paramGlobalApiPropertyMap.keySet();
            for (String string : strings) {
                if (string.contains(".")) {
                    String[] split = string.split("\\.");
                    childParamGlobalApiPropertyMap.put(string.replaceFirst(split[0] + ".", ""), paramGlobalApiPropertyMap.get(string));

                }
            }
        }
        //全局针对多级配置
        Map<String, ApiProperty> childReturnGlobalApiPropertyMap = new HashMap<>();
        if (returnGlobalApiPropertyMap != null && !returnGlobalApiPropertyMap.isEmpty()) {
            Set<String> strings = returnGlobalApiPropertyMap.keySet();
            for (String string : strings) {
                if (string.contains(".")) {
                    String[] split = string.split("\\.");
                    childReturnGlobalApiPropertyMap.put(string.replaceFirst(split[0] + ".", ""), returnGlobalApiPropertyMap.get(string));
                }
            }
        }
        // 强制类型转换
        ParameterizedType pType = (ParameterizedType) gType;
        // 取得泛型类型的泛型参数
        Type[] tArgs = pType.getActualTypeArguments();
        if (tArgs.length == 0) {
            return docProperty;
        }
        Class<?> aClass;
        if (tArgs[1] instanceof ParameterizedType) {
            //处理类上的泛型
            aClass = (Class<?>) ((ParameterizedType) tArgs[1]).getRawType();
        } else {
            aClass = (Class<?>) tArgs[1];
        }
        DocModel docModel = new DocModel();
        Set<DocProperty> apiProperties = new TreeSet<>();
        Set<String> fieldKeySet = new HashSet<>();
        Map<String, ApiMapProperty> copyApiMapPropertyMap = new HashMap<>();
        if (apiMapPropertyMap != null && !apiMapPropertyMap.isEmpty()) {
            copyApiMapPropertyMap.putAll(apiMapPropertyMap);
            //属性Map的name配置
            fieldKeySet = apiMapPropertyMap.keySet();
        }

        if (apiMapParamMap != null && !apiMapParamMap.isEmpty() && isParam) {
            //入参Map
            Set<String> keySet = apiMapParamMap.keySet();
            Map<String, Map<String, ApiParam>> mapMap = new HashMap<>();
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                for (String fieldKey : fieldKeySet) {
                    //ApiParam配置了则MapModelProperty失效
                    if (split[0].equals(key.split("\\.")[0])) {
                        copyApiMapPropertyMap.remove(fieldKey);
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiParam> childApiMapParamMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapParamMap = mapMap.get(split[0]);
                        childApiMapParamMap.put(key.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(key));
                    } else {
                        childApiMapParamMap = new HashMap<>();
                        childApiMapParamMap.put(key.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(key));
                        mapMap.put(split[0], childApiMapParamMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiParam apiParam = apiMapParamMap.get(key);
                    String description = apiParam.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String defaultValue = apiParam.defaultValue();
                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                        property.setDefaultValue(defaultValue);
                    }
                    String type = apiParam.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiParam.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    String example = apiParam.example();
                    if (StringUtils.isNotBlank(example)) {
                        property.setExample(example);
                    }
                    boolean required = apiParam.required();
                    property.setRequited(required);
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, aClass, gType, propertyMap, true, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, mapMap.get(property.getName()), apiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, aClass, gType, propertyMap, true, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, apiMapPropertyMap, mapMap.get(key), apiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        } else if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty() && !isParam) {
            //出参Map
            Set<String> keySet = apiMapReturnMap.keySet();
            Map<String, Map<String, ApiReturn>> mapMap = new HashMap<>();
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                for (String fieldKey : fieldKeySet) {
                    //ApiParam配置了则MapModelProperty失效
                    if (split[0].equals(key.split("\\.")[0])) {
                        copyApiMapPropertyMap.remove(fieldKey);
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiReturn> childApiMapReturnMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapReturnMap = mapMap.get(split[0]);
                        childApiMapReturnMap.put(key.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(key));
                    } else {
                        childApiMapReturnMap = new HashMap<>();
                        childApiMapReturnMap.put(key.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(key));
                        mapMap.put(split[0], childApiMapReturnMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiReturn apiReturn = apiMapReturnMap.get(key);
                    String description = apiReturn.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String type = apiReturn.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiReturn.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, aClass, gType, propertyMap, false, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, copyApiMapPropertyMap, apiMapParamMap, mapMap.get(property.getName()), childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, aClass, gType, propertyMap, false, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, copyApiMapPropertyMap, apiMapParamMap, mapMap.get(key), childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        }
        if (!copyApiMapPropertyMap.isEmpty()) {
            //属性Map
            Set<String> keySet = copyApiMapPropertyMap.keySet();
            Map<String, Map<String, ApiMapProperty>> mapMap = new HashMap<>();
            //复制入参Map类型注解信息
            Map<String, ApiParam> childApiMapParamMap = null;
            //复制出参Map类型注解信息
            Map<String, ApiReturn> childApiMapReturnMap = null;
            Set<DocProperty> docProperties = new TreeSet<>();
            for (String key : keySet) {
                String[] split = key.split("\\.");
                //复制入参Map类型注解信息
                if (apiMapParamMap != null && !apiMapParamMap.isEmpty() && isParam) {
                    childApiMapParamMap = new HashMap<>();
                    Set<String> strings = apiMapParamMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(split[0] + ".")) {
                                childApiMapParamMap.put(string.replaceFirst(split[0] + ".", ""), apiMapParamMap.get(string));
                            } else {
                                childApiMapParamMap.put(string, apiMapParamMap.get(string));
                            }
                        } else {
                            childApiMapParamMap.put(string, apiMapParamMap.get(string));
                        }
                    }
                }
                //复制出参Map类型注解信息
                if (apiMapReturnMap != null && !apiMapReturnMap.isEmpty() && !isParam) {
                    childApiMapReturnMap = new HashMap<>();
                    Set<String> strings = apiMapReturnMap.keySet();
                    for (String string : strings) {
                        if (string.contains(".")) {
                            if (string.startsWith(split[0] + ".")) {
                                childApiMapReturnMap.put(string.replaceFirst(split[0] + ".", ""), apiMapReturnMap.get(string));
                            } else {
                                childApiMapReturnMap.put(string, apiMapReturnMap.get(string));
                            }
                        } else {
                            childApiMapReturnMap.put(string, apiMapReturnMap.get(string));
                        }
                    }
                }
                if (split.length > 1) {
                    //表示还有下一层
                    Map<String, ApiMapProperty> childApiMapPropertyMap;
                    if (mapMap.containsKey(split[0])) {
                        childApiMapPropertyMap = mapMap.get(split[0]);
                        childApiMapPropertyMap.put(key.replaceFirst(split[0] + ".", ""), copyApiMapPropertyMap.get(key));
                    } else {
                        childApiMapPropertyMap = new HashMap<>();
                        childApiMapPropertyMap.put(key.replaceFirst(split[0] + ".", ""), copyApiMapPropertyMap.get(key));
                        mapMap.put(split[0], childApiMapPropertyMap);
                    }
                } else {
                    DocProperty property = new DocProperty();
                    property.setName(split[0]);
                    ApiMapProperty apiReturn = copyApiMapPropertyMap.get(key);
                    String description = apiReturn.description();
                    if (StringUtils.isNotBlank(description)) {
                        property.setDescription(description);
                    }
                    String defaultValue = apiReturn.defaultValue();
                    if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                        property.setDefaultValue(defaultValue);
                    }
                    String type = apiReturn.type();
                    if (StringUtils.isNotBlank(type)) {
                        property.setType(type);
                    } else {
                        property.setType("");
                    }
                    String format = apiReturn.format();
                    if (StringUtils.isNotBlank(format)) {
                        property.setFormat(format);
                    }
                    String example = apiReturn.example();
                    if (StringUtils.isNotBlank(example)) {
                        property.setExample(example);
                    }
                    boolean required = apiReturn.required();
                    property.setRequited(required);
                    docProperties.add(property);
                }
            }
            if (!docProperties.isEmpty()) {
                for (DocProperty property : docProperties) {
                    String type = property.getType();
                    property.setType("");
                    property = handleModel(property, aClass, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, copyApiMapPropertyMap, null, null, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    if (StringUtils.isNotBlank(type) && StringUtils.isBlank(property.getType())) {
                        property.setType(type);
                    }
                    apiProperties.add(property);
                }
            }
            //继续向下执行
            if (!mapMap.isEmpty()) {
                Set<String> childKeySet = mapMap.keySet();
                for (String key : childKeySet) {
                    DocProperty childProperty = new DocProperty();
                    childProperty.setName(key);
                    childProperty = handleModel(childProperty, aClass, gType, propertyMap, isParam, isJson, repeats, apiModelPropertyMap, apiReturnModelPropertyMap, mapMap.get(key), childApiMapParamMap, childApiMapReturnMap, childParamGlobalApiPropertyMap, childReturnGlobalApiPropertyMap);
                    apiProperties.add(childProperty);
                }
            }
        }
        docModel.setApiProperties(apiProperties);
        docProperty.setDocModel(docModel);
        return docProperty;
    }

    /**
     * 驼峰转下划线
     *
     * @param str 原始字符串
     * @return 下滑线格式的字符串
     */
    public String humpToLine(String str) {
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

