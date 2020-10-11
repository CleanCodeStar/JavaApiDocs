# javaApiDocs文档

[TOC]

## 访问ApiDocs页面方式

ip:端口/项目名/citrsw/index.html

## 示例

1.  [JavaApiDocs源码地址](https://github.com/15706058532/JavaApiDocs)
2. [使用文档](https://api.citrsw.com)
3. [演示页面](https://example.citrsw.com/citrsw/index.html)
4. [演示后端java代码](https://github.com/15706058532/api-example)

## 辅助工具

[集成JavaApiDocs的代码生成器]("https://github.com/15706058532/MyBatisPlusGenerator")

## maven配置

```xml
<dependency>
    <groupId>com.citrsw</groupId>
    <artifactId>java-api-docs</artifactId>
    <version>1.2.0-beta</version>
</dependency>
```

## 拦截器配置

如果使用了拦截器则必须放行JavaApiDocs的请求

1. ```java
   registry.addResourceHandler("/citrsw/**").addResourceLocations("classpath:/citrsw/");
   ```

2. ```java
   .excludePathPatterns("/citrsw/**")
   ```

   

```java
/**
 * 拦截器配置
 *
 * @author Zhenfeng Li
 * @date 2020-01-07 15:15:11
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public InterceptorConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 放行API
     *
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/citrsw/**").addResourceLocations("classpath:/citrsw/");
    }

    /**
     * 配置拦截器执行顺序
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 多个拦截器组成一个拦截器链
        // 默认全部拦截
        // addPathPatterns 添加拦截
        // excludePathPatterns 排除拦截
        //登录拦截器
        registry.addInterceptor(loginInterceptor)
                //需要拦截的uri
                .addPathPatterns("/**")
                //需要跳过的uri
                .excludePathPatterns("/citrsw/**", "/login", "/logout")
                //拦截器的执行顺序
                .order(1);
    }
}
```



## 入门用法

### 注解使用说明

#### @ApiIgnore

1. **适用范围**

   适用于Controller类、Controller的方法上、实体类属性上、入参参数上

2. **使用示例**

   ```java
   //Controller类
   @RestController
   @Slf4j
   @ApiClass(value = "管理员")
   @ApiIgnore
   public class UserController {
   
   }
   //Controller类方法
   @ApiMethod("根据条件分页查询")
   @GetMapping("/user/page/all")
   @ApiIgnore
   public Result<Page<User>> pageAll(User user, Page<User> page) {
       LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
   	return Result.buildQueryOk(userService.page(page, wrapper));
   }
   //实体类属性
   /**
    * 主键Id
    */
   @ApiProperty(description = "主键Id")
   @ApiIgnore
   private Long id;
   
   ```


#### @ApiEnable

1. **适用范围**

   Spring-Boot 启动类上

2. **参数说明**

   | 参数名     | 说明               |
   | ---------- | ------------------ |
   | name       | 项目名称           |
   | underscore | 是否使用下划线名称 |

3. **使用示例**

   ```java
   @ApiEnable(name = "中国IT资源分享网站", underscore = true)
   @SpringBootApplication
   public class ApiExampleApplication {
       public static void main(String[] args) {
           SpringApplication.run(ApiExampleApplication.class, args);
       }
   
   }
   ```

#### @ApiClass

1. **适用范围**

   Controller类上

2. **参数说明**

   | 参数名 | 说明             |
   | ------ | ---------------- |
   | value  | Controller类描述 |

3. **使用示例**

   ```java
   @ApiClass(value = "学生")
   @RestController
   public class StudentController {
   }
   ```

#### @ApiMethod

1. **适用范围**

   Controller类的public方法上

2. **参数说明**

   | 参数名 | 说明                     |
   | ------ | ------------------------ |
   | value  | Controller类中方法的描述 |

3. **使用示例**

   ```java
   @ApiMethod("根据条件分页查询")
   @GetMapping("/user/page/all")
   public Result<Page<User>> pageAll(User user, Page<User> page) {
       LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
   	return Result.buildQueryOk(userService.page(page, wrapper));
   }
   ```

#### @ApiModel

1. **适用范围**

   实体类上

2. **参数说明**

   | 参数名 | 说明       |
   | ------ | ---------- |
   | value  | 实体类描述 |

3. **使用示例**

   ```java
   @TableName("tb_user")
   @Accessors(chain = true)
   @Data
   @ApiModel("管理员")
   public class User implements Serializable {
   }
   ```

#### @ApiProperty

1. **适用范围**

   实体类的属性上

   实体类属性的get/set方法上

2. **参数说明**

   | 参数名       | 说明     |
   | ------------ | -------- |
   | description  | 属性描述 |
   | name         | 属性别名 |
   | required     | 是否必须 |
   | example      | 示例     |
   | defaultValue | 默认值   |

3. **使用示例**

   ```java
   /**
    * 主键Id
    */
    @ApiProperty(description = "主键Id")
    private Long id;
   /**
    * 登录名
    */
    @ApiProperty(description = "登录名", name = "login_name", required = true, example = "admin")
    private String loginName;
   ```

#### @ApiParam（初级用法）

1. **适用范围**

   Controller类public方法上的入参参数上

2. **参数说明**

   | 参数名       | 说明                                                         |
   | ------------ | ------------------------------------------------------------ |
   | name         | 别名                                                         |
   | description  | 描述                                                         |
   | required     | 是否必须                                                     |
   | type         | 数据类型[int,int[],long,long[],date,date[],string,string[],double,double[]] |
   | format       | 数据格式(一般用于日期)[例如：yyyy-MM-dd HH:mm:ss]            |
   | defaultValue | 默认值                                                       |
   | example      | 示例                                                         |

3. **使用示例**

   ```java
   @ApiMethod("根据条件分页查询")
   @GetMapping("/user/page/all")
   public Result<Page<User>> pageAll(User user, @RequestParam(defaultValue = "1")@ApiParam(description = "当前页", defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") @ApiParam(description = "每页数据条数", defaultValue = "10") Integer pageSize) {
       LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
       return Result.buildQueryOk(userService.page(new Page<>(page, pageSize), wrapper));
   }
   ```

## 中级用法

### 注解使用说明

#### @ApiBasicReturn

1. **适用范围**

   Controller类的public方法上

   仅仅适用于响应为基本数据类型的场景

2. **参数说明**

   | 参数名      | 说明 |
   | ----------- | ---- |
   | description | 描述 |

3. **使用示例**

   ```java
   @ApiMethod("保存学生信息")
   @PostMapping("/student")
   @ApiBasicReturn(description = "保存结果")
   public Boolean saveStudent(@RequestBody Student student) {
       return studentService.save(student);
   }
   ```

#### @ApiMapProperty

1. **适用范围**

   实体类属性

   实体类属性的get/set方法上

   主要用于响应类型为Map或包含Map属性的类

2. **参数说明**

   | 参数名       | 说明                                                    |
   | ------------ | ------------------------------------------------------- |
   | name         | Map类型的key名称（支持多级配置，例如：user.info.level） |
   | description  | 描述                                                    |
   | required     | 是否必须                                                |
   | type         | 类型（在实际类型为Object时生效）                        |
   | format       | 数据格式(一般用于日期)[例如：yyyy-MM-dd HH:mm:ss]       |
   | defaultValue | 默认值                                                  |
   | example      | 示例                                                    |

3. **使用示例**

   ```java
   @ApiMapProperty(name = "level", description = "用户等级", type = "int")
   @ApiMapProperty(name = "integral", description = "用户积分", type = "int")
   private Map<String,Integer> userInfo;
   ```

#### @ApiAppointParam

1. **适用范围**

   Controller类的public方法上

   仅仅适用于入参类型

   主要用于不需要全部实体属性的场景

2. **参数说明**

   | 参数名     | 说明                     |
   | ---------- | ------------------------ |
   | require    | 必须传入的属性名称数组   |
   | nonRequire | 非必须传入的属性名称数组 |

3. **使用示例**

   ```java
   @ApiMethod("入参Student类仅显示指定属性")
   @GetMapping("/appoint/student")
   @ApiAppointParam(require = {"age", "sex"}, nonRequire = {"name"})
   public Student countNumByClazzId(Student student) {
       return student;
   }
   //对应的实体类
   /**
   * 学生
   *
   * @author Zhenfeng Li
   * @version 1.0.0
   * @date 2020-10-02 08:59:51
   */
   @TableName("tb_student")
   @Accessors(chain = true)
   @Data
   @ApiModel("学生")
   public class Student implements Serializable {
   
       private static final long serialVersionUID = 1L;
   
       /**
       * 主键Id
       */
       @ApiProperty(description = "主键Id")
       private Long id;
   
       /**
       * 学号
       */
       @ApiProperty(description = "学号")
       private String no;
   
       /**
       * 姓名
       */
       @ApiProperty(description = "姓名")
       private String name;
   
       /**
       * 年龄
       */
       @ApiProperty(description = "年龄")
       private Integer age;
   
       /**
        * 性别 0：女，1：男，3：保密
        */
       @ApiProperty(description = "性别 0：女，1：男，3：保密")
       private Integer sex;
   
       /**
       * 班级Id
       */
       @ApiProperty(description = "班级Id")
       private Long clazzId;
   
       /**
       * 班主任Id
       */
       @ApiProperty(description = "班主任Id")
       private Long teacherId;
   }
   ```

#### @ApiGlobalClass

1. **适用范围**

   Spring-Boot 启动类上

   可配置多个

   主要用于使用第三方jar时，对请求或响应的第三方实体进行全局统一配置

2. **参数说明**

   | 参数名      | 说明                                               |
   | ----------- | -------------------------------------------------- |
   | name        | 类名                                               |
   | description | 类描述                                             |
   | type        | 类型（入参：TypeEnum.PARAM,响应：TypeEnum.RETURN） |
   | properties  | ApiProperty数组（属性注解集合）                    |

3. **使用示例**

   ```java
   //入参全局配置
   @SpringBootApplication
   @ApiEnable(name = "中国IT资源分享网站", underscore = true)
   @ApiGlobalClass(name = Page.class, type = TypeEnum.PARAM, properties = {
           @ApiProperty(name = "current", description = "当前页", defaultValue = "1"),
           @ApiProperty(name = "size", description = "每页显示条数", defaultValue = "10"),
           @ApiProperty(name = "orders.asc", description = "排序方式"),
           @ApiProperty(name = "orders.column", description = "排序字段")
   })
   public class ApiExampleApplication {
       public static void main(String[] args) {
           SpringApplication.run(ApiExampleApplication.class, args);
       }
   }
   //响应全局配置
   @SpringBootApplication
   @ApiEnable(name = "中国IT资源分享网站", underscore = true)
   @ApiGlobalClass(name = Page.class, description = "分页", type = TypeEnum.RETURN, properties = {
           @ApiProperty(name = "current", description = "当前页"),
           @ApiProperty(name = "size", description = "每页显示条数"),
           @ApiProperty(name = "records"),
           @ApiProperty(name = "previous", description = "是否有上一页"),
           @ApiProperty(name = "next", description = "是否有下一页"),
           @ApiProperty(name = "total", description = "总条数"),
           @ApiProperty(name = "pages", description = "总页数"),
   })
   public class ApiExampleApplication {
       public static void main(String[] args) {
           SpringApplication.run(ApiExampleApplication.class, args);
       }
   }
   ```

#### @ApiGlobalCode

1. **适用范围**

   Spring-Boot 启动类上

   可配置多个

   主要用于全局配置响应状态码

2. **参数说明**

   | 参数名      | 说明   |
   | ----------- | ------ |
   | name        | 参数名 |
   | value       | 值     |
   | description | 描述   |

3. **使用示例**

   ```java
   @SpringBootApplication
   @ApiEnable(name = "中国IT资源分享网站", underscore = true)
   @ApiGlobalCode(name = "code", value = "200", description = "成功")
   @ApiGlobalCode(name = "code", value = "300", description = "失败")
   @ApiGlobalCode(name = "code", value = "400", description = "token失效")
   @ApiGlobalCode(name = "code", value = "500", description = "系统内部异常")
   public class ApiExampleApplication {
       public static void main(String[] args) {
           SpringApplication.run(ApiExampleApplication.class, args);
       }
   }
   ```

#### @ApiCode

1. **适用范围**

   Controller类的public方法上

   可配置多个

   主要针对当前方法覆盖全局配置的状态码或追加当前接口的状态码

2. **参数说明**

   | 参数名      | 说明   |
   | ----------- | ------ |
   | name        | 参数名 |
   | value       | 值     |
   | description | 描述   |

3. **使用示例**

   ```java
   @ApiMethod("增加状态码/覆盖全局状态码")
   @GetMapping("/add/code")
   @ApiCode(name = "code", value = "200", description = "用户保存成功")
   @ApiCode(name = "code", value = "300", description = "用户保存失败")
   @ApiCode(name = "code", value = "310", description = "用户名或密码为空")
   public Student save(Student student) {
       return student;
   }
   ```

## 高级用法

### 注解使用说明

#### @ApiParamModelProperty

1. **适用范围**

   Controller类的public方法上

   仅仅适用于入参的实体

   主要用于覆盖实体类中属性上@ApiProperty注解的配置

2. **参数说明**

   | 参数名       | 说明                                            |
   | ------------ | ----------------------------------------------- |
   | name         | 属性名称（支持多级配置，例如：user.info.level） |
   | description  | 描述                                            |
   | required     | 是否必须                                        |
   | defaultValue | 默认值                                          |
   | example      | 示例                                            |

3. **使用示例**

   ```java
   /**
    * 保存
    */
   @ApiMethod("保存")
   @PostMapping("/user")
   @ApiParamModelProperty(name = "username",description = "账号",required = true)
   @ApiParamModelProperty(name = "phone",description = "固定电话号码",required = true)
   public Result<Boolean> save(@RequestBody User user, HttpSession session) {
       boolean save = userService.save(user);
       return Result.buildSaveOk(save);
   }
   //对应的实体类
   /**
    * 管理员
    *
    * @author Zhenfeng Li
    * @version 1.0.0
    * @date 2020-10-02 08:59:51
    */
   @TableName("tb_user")
   @Accessors(chain = true)
   @Data
   @ApiModel("管理员")
   public class User implements Serializable {
   
       private static final long serialVersionUID = 1L;
   
       /**
        * 主键Id
        */
       @ApiProperty(description = "主键Id")
       private Long id;
   
       /**
        * 用户名
        */
       @ApiProperty(description = "用户名")
       private String username;
   
       /**
        * 登录名
        */
       @ApiProperty(description = "登录名", name = "loginName", required = true, example = "admin")
       private String loginName;
   
       /**
        * 密码
        */
       @ApiProperty(description = "密码")
       private String password;
   
       /**
        * 手机号
        */
       @ApiProperty(description = "手机号")
       private String phone;
   
       /**
        * 用户扩展信息
        */
       @ApiMapProperty(name = "level", description = "用户等级", type = "int")
       @ApiMapProperty(name = "integral", description = "用户积分", type = "int")
       private Map<String, Integer> userInfo;
   }
   ```


#### @ApiMapParam

1. **适用范围**

   Controller类的public方法上

   主要用于入参类型为Map或包含Map

2. **参数说明**

   | 参数名 | 说明         |
   | ------ | ------------ |
   | value  | ApiParam数组 |

3. **使用示例**

   ```java
   @ApiMethod("入参为Map<String,Object>类型")
   @GetMapping("/map/object")
   @ApiMapParam({
       @ApiParam(name = "age", description = "年龄", type = "int", required = true),
       @ApiParam(name = "sex", description = "性别", type = "int", required = true)
   })
   public Map<String, Object> mapObject(Map<String, Object> param) {
       Integer age = (Integer) param.get("age");
       Integer sex = (Integer) param.get("sex");
       return param;
   }
   ```

   

#### @ApiParam（高级用法）

1. **适用范围**

   @ApiMapParam的value参数 或 Controller类public方法上的入参参数

2. **参数说明**

   | 参数名       | 说明                                                    |
   | ------------ | ------------------------------------------------------- |
   | name         | Map类型的key名称（支持多级配置，例如：user.info.level） |
   | description  | 描述                                                    |
   | required     | 是否必须                                                |
   | type         | 类型（在实际类型为Object时生效）                        |
   | format       | 数据格式(一般用于日期)[例如：yyyy-MM-dd HH:mm:ss]       |
   | defaultValue | 默认值                                                  |
   | example      | 示例                                                    |

3. **使用示例**

   ```java
   //入门：Controller类public方法上的入参参数
   @ApiMethod("根据条件分页查询")
   @GetMapping("/user/page/all")
   public Result<Page<User>> pageAll(User user, @RequestParam(defaultValue = "1")@ApiParam(description = "当前页", defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") @ApiParam(description = "每页数据条数", defaultValue = "10") Integer pageSize) {
       LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
       return Result.buildQueryOk(userService.page(new Page<>(page, pageSize), wrapper));
   }
   //高级：@ApiMapParam的value参数
   @ApiMethod("入参为Map<String,  Map<String,Object>>类型")
   @GetMapping("/map/map/object")
   @ApiMapParam({
       @ApiParam(name = "info.level", description = "用户等级", type = "int", required = true),
       @ApiParam(name = "info.integral", description = "用户积分", type = "int", required = true),
       @ApiParam(name = "username.", description = "用户名", type = "string", required = true)
   })
   public Map<String, Map<String,Object>> mapObject(Map<String,  Map<String,Object>> param) {
       return param;
   }
   ```

#### @ApiReturnModelProperty

1. **适用范围**

   Controller类的public方法上

   仅仅适用于响应的实体

   主要用于覆盖实体类中属性上@ApiProperty注解的配置

2. **参数说明**

   | 参数名      | 说明                                            |
   | ----------- | ----------------------------------------------- |
   | name        | 属性名称（支持多级配置，例如：user.info.level） |
   | description | 描述                                            |

3. **使用示例**

   ```java
   /**
    * 根据条件分页查询
    */
   @ApiMethod("根据条件分页查询")
   @GetMapping("/return/user/page/all")
   @ApiReturnModelProperty(name = "data.records.username",description = "账号")
   @ApiReturnModelProperty(name = "data.records.phone",description = "固定电话号码")
   public Result<Page<User>> pageAll(User user, Page<User> page) {
       LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
       return Result.buildQueryOk(userService.page(page, wrapper));
   }
   //Result实体类
   @Data
   public class Result<T> implements Serializable {
       private static final long serialVersionUID = -2492072809889519824L;
   
       /**
        * 响应编码
        */
       private Integer code;
   
       /**
        * 响应消息
        */
       private String msg;
   
       /**
        * 响应数据
        */
       private T data;
   }
   //Page实体类
   public class Page<T> implements IPage<T> {
   
       private static final long serialVersionUID = 8545996863226528798L;
   
       /**
        * 查询数据列表
        */
       private List<T> records = Collections.emptyList();
   }
   //User实体类
   @TableName("tb_user")
   @Accessors(chain = true)
   @Data
   @ApiModel("管理员")
   public class User implements Serializable {
   
       private static final long serialVersionUID = 1L;
   
       /**
        * 主键Id
        */
       @ApiProperty(description = "主键Id")
       private Long id;
   
       /**
        * 用户名
        */
       @ApiProperty(description = "用户名")
       private String username;
   
       /**
        * 登录名
        */
       @ApiProperty(description = "登录名", name = "loginName", required = true, example = "admin")
       private String loginName;
   
       /**
        * 密码
        */
       @ApiProperty(description = "密码")
       private String password;
   
       /**
        * 手机号
        */
       @ApiProperty(description = "手机号")
       private String phone;
   }
   ```

#### @ApiMapReturn

1. **适用范围**

   Controller类的public方法上

   主要用于响应类型为Map或包含Map

2. **参数说明**

   | 参数名 | 说明          |
   | ------ | ------------- |
   | value  | ApiReturn数组 |

3. **使用示例**

   ```java
   @ApiMethod("出参为Map<String,Object>类型")
   @GetMapping("/map/list/student")
   @ApiMapReturn({
       @ApiReturn(name = "username", type = "string", description = "用户名"),
       @ApiReturn(name = "password", type = "string", description = "密码")
   })
   public Map<String, List<String>> mapListStudent() {
       return null;
   }
   ```

#### @ApiReturn

1. **适用范围**

   @ApiMapReturn的value参数

   主要用于响应类型为Map或包含Map

2. **参数说明**

   | 参数名      | 说明                                                    |
   | ----------- | ------------------------------------------------------- |
   | name        | Map类型的key名称（支持多级配置，例如：user.info.level） |
   | description | 描述                                                    |
   | type        | 类型（在实际类型为Object时生效）                        |
   | format      | 数据格式(一般用于日期)[例如：yyyy-MM-dd HH:mm:ss]       |

3. **使用示例**

   ```java
   //响应为Map<String,  Map<String,Object>>类型
   @ApiMethod("响应为Map<String,  Map<String,Object>>类型")
   @GetMapping("/map/map/object")
   @ApiMapReturn({
       @ApiReturn(name = "info.level", description = "用户等级", type = "int"),
       @ApiReturn(name = "info.integral", description = "用户积分", type = "int"),
       @ApiReturn(name = "username.", description = "用户名", type = "string")
   })
   public Map<String, Map<String,Object>> mapObject() {
       return null;
   }
   //响应为List<Map<String, Map<String,Object>>>类型
   @ApiMethod("响应为List<Map<String, Map<String,Object>>>类型")
   @GetMapping("/list/map/map/object")
   @ApiMapReturn({
       @ApiReturn(name = "info.level", description = "用户等级", type = "int"),
       @ApiReturn(name = "info.integral", description = "用户积分", type = "int"),
       @ApiReturn(name = "username.", description = "用户名", type = "int")
   })
   public List<Map<String, Map<String,Object>>> mapObject() {
       return null;
   }
   ```

## 覆盖规则

**针对同一个属性进行多个配置则按以下方式，前面的配置覆盖后面的配置**

1. 实体注解配置：get()/set()方法上的注解  **>**  对应属性上的注解

2. ApiProperty.description【描述】 **>**  ApiModel.value【描述】

3. ApiModelProperty  **>**  ApiParam  **=**  ApiMapParam  **>** ApiProperty **=** ApiMapProperty

4. ApiReturnModelProperty  **>**  ApiReturn  **=**  ApiMapReturn  **>**  ApiProperty  **=**  ApiMapProperty

5. ApiCode  **>**  ApiGlobalCode

**注意**

1. ApiModelProperty 和ApiReturnModelProperty不支持Map类型

2. ApiMapParam  **=**  ApiMapReturn 可对实体类的属性为Map类型的进行覆盖

**从总体进行分类**

1. 方法上的注解  **>**  属性上的注解  **>**  全局注解

