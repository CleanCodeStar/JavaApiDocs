package com.citrsw.core;

import com.citrsw.annatation.ApiClass;
import com.citrsw.annatation.ApiIgnore;
import com.citrsw.definition.DocClass;
import com.citrsw.definition.DocMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 处理Controller类
 *
 * @author 李振峰
 * @version 1.0
 * @date 2021-08-04 20:23
 */
@Component
@Slf4j
public class ControllerHandle {

    private final MethodHandle handleMethod;

    public ControllerHandle(MethodHandle handleMethod) {
        this.handleMethod = handleMethod;
    }

    /**
     * 获取需要扫描的包
     */
    public List<String> takePackages(Class<?> mainApplicationClass) {
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
    public void scanner(String packageName, Set<Class<?>> classes) {
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
            Set<DocMethod> docMethods = handleMethod.handleMethod(methods, requestMapping);
            docClass.setDocMethods(docMethods);
            docClasses.add(docClass);
        }
        return docClasses;
    }


}
