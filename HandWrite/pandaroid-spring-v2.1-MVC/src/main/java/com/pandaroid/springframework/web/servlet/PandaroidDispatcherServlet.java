package com.pandaroid.springframework.web.servlet;

import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidRequestMapping;
import com.pandaroid.springframework.context.PandaroidApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 职责：通过委派模式，负责任务调度、请求分发
 * @author pandaroid
 */
public class PandaroidDispatcherServlet extends HttpServlet {
    /**
     * System.out.println 实际不用于输出日志，一般用 slf4j 、logback 输出到文件、定制格式和日志等级、异步输出日志等，以达到更佳的性能
     */
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        System.out.println("[PandaroidDispatcherServlet doPost] Web 容器接收请求，进行委派 doDispatch");
        // 根据 url 从 handlerMapping 中找到一个对应的 method 并通过 resp 返回
        try {
            doDispatch(req, resp);
        } catch (IOException e) {
            e.printStackTrace();
            resp.getWriter().write("500 Internal Server Error!!! Exception Detail: " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 委派处理请求，进行响应
     * http://localhost/web/add*.json?name=wangpei&addr=Pandaroid
     * http://localhost/web/query.json?name=wangpei
     * http://localhost/web/edit.json?name=wangpei&id=333
     * http://localhost/web/remove.json?id=222
     * 增加 HandlerMapping 正则匹配以后（/web* 和 /add* 两段都可以正则匹配）：
     * http://localhost/web/addPandaroid.json?name=wangpei&addr=Pandaroid
     * http://localhost/web3344/addPandaroid.json?name=wangpei&addr=Pandaroid
     * @param req
     * @param resp
     * @throws IOException
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. 先获取到 req 对应的 HandlerMapping
        PandaroidHandlerMapping handlerMapping = doGetHandlerMapping(req);
        // 如果没有配置，那么 404
        if(null == handlerMapping) {
            // 先统一封装处理返回结果的 processDispatchResult ：根据 ModelAndView 来处理
            processDispatchResult(req, resp, new PandaroidModelAndView("404"));
            return ;
        }
        // 增加 HandlerAdapter ，与 HandlerMapping 一一对应？具体做法：
        // 2. 根据一个 HandlerMapping 去获得一个 HandlerAdapter
        // HandlerAdapter 动态解析 HandlerMapping 的参数？
        // 适配器模式：用于亡羊补牢。没有提前设计好，所以使用适配器
        // 这里 HandlerAdapter 解析形参和返回结果
        PandaroidHandlerAdapter handlerAdapter = doGetHandlerAdapter(handlerMapping);
        // 3. 解析某一个方法的形参和返回值之后，统一封装为 ModelAndView 对象
        if(null == handlerAdapter) {
            processDispatchResult(req, resp, new PandaroidModelAndView("500"));
            return ;
        }
        PandaroidModelAndView modelAndView = handlerAdapter.handler(req, resp, handlerMapping);
        // 暂时先统一处理 ModelAndView ，把 ModelAndView 通过 processDispatchResult 处理成 ViewResolver ，再通过 ViewResolver 渲染具体的页面、字符串等
        processDispatchResult(req, resp, modelAndView);
    }

    private final Map<PandaroidHandlerMapping, PandaroidHandlerAdapter> handlerMappings2AdaptersMap = new HashMap<>();
    private PandaroidHandlerAdapter doGetHandlerAdapter(PandaroidHandlerMapping handlerMapping) {
        if(null == handlerMapping) {
            return null;
        }
        return handlerMappings2AdaptersMap.get(handlerMapping);
    }

    /**
     * 结果可能是页面、对象、字符串、json 、空等等
     * 如果 modelAndView 为空，则直接通过 resp 进行输出即可
     * 如果不为空，则渲染相应的 Template 页面
     * @param req
     * @param resp
     * @param modelAndView
     */
    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, PandaroidModelAndView modelAndView) {
        if(null == modelAndView) {
            return ;
        }
        switch (modelAndView.getViewName()) {
            case "404":
                try {
                    resp.getWriter().write("404 Not Found!!!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "500":
                break;
            default:
        }
    }

    private PandaroidHandlerMapping doGetHandlerMapping(HttpServletRequest req) {
        if(handlerMappings.isEmpty()) {
            return null;
        }
        // 不为空，匹配
        // 从 req 中取出 url
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("[PandaroidDispatcherServlet doDispatch] url: " + url);
        System.out.println("[PandaroidDispatcherServlet doDispatch] contextPath: " + contextPath);
        String mvcUrl = url.replace(contextPath, "").replaceAll("/+", "/");
        System.out.println("[PandaroidDispatcherServlet doDispatch] mvcUrl: " + mvcUrl);
        // url 跟 handlerMappings 中进行匹配
        for (PandaroidHandlerMapping handlerMapping : handlerMappings) {
            Matcher matcher = handlerMapping.getPattern().matcher(mvcUrl);
            if(matcher.matches()) {
                return handlerMapping;
            }
        }
        return null;
    }

    private PandaroidApplicationContext applicationContext;
    @Override
    public void init(ServletConfig config) {
        // 1. 加载配置文件
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        System.out.println("[PandaroidDispatcherServlet init] 1. 加载配置文件 contextConfigLocation：" + contextConfigLocation);
        logger.info("[PandaroidDispatcherServlet init] 1. 加载配置文件 contextConfigLocation：{}", contextConfigLocation);
        // 初始化 PandaroidApplicationContext ，Spring 的核心 IoC 容器，Bean 工厂
        // 其构造方法应该有几个参数？
        applicationContext = new PandaroidApplicationContext(contextConfigLocation);
        /*doLoadConfig(contextConfigLocation);
        // 2. 扫描 scanPackage
        String scanPackage = contextConfig.getProperty("scanPackage");
        System.out.println("[PandaroidDispatcherServlet init] 2. 扫描 scan-package 下相关的类。scanPackage：" + scanPackage);
        doScanPackageClasses(scanPackage);
        // 3. 反射实例化扫描到的类，初始化 IoC 容器
        System.out.println("[PandaroidDispatcherServlet init] 3. 初始化 IoC 容器：将 2 中扫描到的类实例化，并放入 IoC 容器中");
        doInitIoc();
        // AOP 应该在 DI 之前
        // 因为 AOP 以后是生成新的代理对象，然后进行 DI 全新的代理对象，而不是原本的实例对象
        // 这里 DI 注入给的目标对象是代理对象，被 DI 注入给目标对象属性的 IoC 中的对象也应是代理对象
        // 4. 依赖注入 DI
        System.out.println("[PandaroidDispatcherServlet init] 4. 依赖注入 DI");
        doDiAutowired();*/
        // 完成 IoC 、DI 与之前非委派（逻辑都揉在本 DispatcherServlet 里）MVC 的对接
        // 5. 初始化 MVC HandlerMappting
        // 模仿 Spring 的 DispatcherServlet 初始化 MVC 九大组件，下面的 initHandlerMappings() 也包含在其中
        System.out.println("[PandaroidDispatcherServlet init] 5. 初始化 MVC HandlerMapping");
        initStrategies(applicationContext);
        // initHandlerMappings();
        System.out.println("[PandaroidDispatcherServlet init] init 完毕。Started Pandaroid Spring");
    }

    /**
     * 九大组件的初始化是有顺序的
     * @param applicationContext
     */
    private void initStrategies(PandaroidApplicationContext applicationContext) {
        // 多文件上传组件
        // initMultipartResolver(applicationContext);
        // 初始化本地语言环境：根据客户端 locale 切换不同语言的展示
        // initLocaleResolver(applicationContext);
        // 初始化主题模板处理器
        // initThemeResolver(applicationContext);
        // 初始化 HandlerMapping
        initHandlerMappings(applicationContext);
        // 初始化 handler 适配器：解析 HandlerMapping 参数和返回 ModelAndView
        initHandlerAdapters(applicationContext);
        // 初始化异常拦截器
        // initHandlerExceptionResolvers(applicationContext);
        // 初始化视图预处理器
        // initRequestToViewNameTranslator(applicationContext);
        // 初始化视图转换器
        initViewResolvers(applicationContext);
        // FlashMap 管理器
        // initFlashMapManager(applicationContext);
    }

    private void initViewResolvers(PandaroidApplicationContext applicationContext) {

    }

    private void initHandlerAdapters(PandaroidApplicationContext applicationContext) {
        for (PandaroidHandlerMapping handlerMapping : handlerMappings) {
            handlerMappings2AdaptersMap.put(handlerMapping, new PandaroidHandlerAdapter());
        }
    }

    /**
     * 实际 Spring 是以 ArrayList 保存 handlerMapping
     * 原因：涉及到正则匹配 url ，用 Map 就没有优势了，还是用 ArrayList 更合适
     * 我们也可以看到其他如 Golang 、Node.js 的 Web 框架中，对于 router 也有类似的涉及：按顺序正则匹配第一个的
     * // private Map<String, Method> handlerMapping = new HashMap<String, Method>();
     */
    private final List<PandaroidHandlerMapping> handlerMappings = new ArrayList<>();
    private void initHandlerMappings(PandaroidApplicationContext applicationContext) {
        // applicationContext.getBeanDefinitionCount() 获得 IoC 容器中已经初始化的 BeanDefinition 的个数
        if(this.applicationContext.getBeanDefinitionCount() == 0) {
            return ;
        }
        // 迭代 IoC 容器中的 Bean ，处理 PandaroidController 注解的 Action 其中的 PandaroidRequestMapping 注解
        for (String beanDefinitionName : this.applicationContext.getBeanDefinitionNames()) {
            Object beanInstance = this.applicationContext.getBean(beanDefinitionName);
            Class<?> iocBeanClazz = beanInstance.getClass();
            // 如果当前 Bean 不是 PandaroidController ，则不用处理
            if(!iocBeanClazz.isAnnotationPresent(PandaroidController.class)) {
                continue;
            }
            // 当前 Bean 是 PandaroidController ，处理 PandaroidRequestMapping
            // 先处理定义在 Class 上的 PandaroidRequestMapping
            String beanClazzUrl = "";
            if(iocBeanClazz.isAnnotationPresent(PandaroidRequestMapping.class)) {
                beanClazzUrl = iocBeanClazz.getAnnotation(PandaroidRequestMapping.class).value();
            }
            // 再处理定义在 method 上的 PandaroidRequestMapping
            // getMethods @return ：the array of {@code Method} objects representing the public methods of this class
            // 这里使用 getMethods ，只获取 public 的方法来进行处理
            Method[] beanMethods = iocBeanClazz.getMethods();
            // 没有 beanMethods ，则相当于没有定义具体的 handler ，则没有 handlerMapping
            if(beanMethods.length == 0) {
                continue;
            }
            // 有 beanMethods ，迭代处理带 PandaroidRequestMapping 的项
            for (Method beanMethod : beanMethods) {
                // 没有注解 PandaroidRequestMapping 的方法，不用处理
                if(!beanMethod.isAnnotationPresent(PandaroidRequestMapping.class)) {
                    continue;
                }
                // 注解了 PandaroidRequestMapping 的方法，取 value 看 url ，跟前面的 beanClazzUrl 拼接完整的 url
                String beanMethodUrl = beanMethod.getAnnotation(PandaroidRequestMapping.class).value();
                // 如果 beanMethodUrl 为空，则没有具体的 url 对应处理，跳过（或者抛出异常？）
                if("".equals(beanMethodUrl.trim())) {
                    continue;
                }
                // beanMethodUrl 不为空，则进行完整 url 拼接
                String handlerMappingUrlRegex = ("/" + beanClazzUrl + "/" + beanMethodUrl).replaceAll("\\*", ".*");
                // 这里正则处理，将 handlerMappingUrl 中多个 / 处理为单个 /
                handlerMappingUrlRegex = handlerMappingUrlRegex.replaceAll("/+", "/");
                // 以 handlerMappingUrl 为 key ，以 beanMethod 为 value ，存入 HandlerMapping
                // 处理的时候，根据请求 url 取出对应的 beanMethod ，进行 invoke
                // 原来的处理：handlerMapping.put(handlerMappingUrl, beanMethod);
                Pattern handlerMappingUrlRegexPattern = Pattern.compile(handlerMappingUrlRegex);
                handlerMappings.add(new PandaroidHandlerMapping(handlerMappingUrlRegexPattern, beanInstance, beanMethod));
                // 打印
                System.out.println("[PandaroidDispatcherServlet initHandlerMappings] handlerMappings.add handlerMappingUrlRegex: " + handlerMappingUrlRegex);
                System.out.println("[PandaroidDispatcherServlet initHandlerMappings] handlerMappings.add handlerMappingUrlRegexPattern: " + handlerMappingUrlRegexPattern);
                System.out.println("[PandaroidDispatcherServlet initHandlerMappings] handlerMappings.add beanInstance: " + beanInstance);
                System.out.println("[PandaroidDispatcherServlet initHandlerMappings] handlerMappings.add beanMethod: " + beanMethod);
            }
        }
    }

    /*private void doDiAutowired() {
        if(ioc.isEmpty()) {
            return ;
        }
        // 从 IoC 容器中取出每一个 Bean（iocEntry.getValue()），然后看每一个 Bean 的 Field（iocBeanField）是否注解了 PandaroidAutowired ，
        // 根据该注解从 IoC 容器中获取相应的 Bean 进行注入
        for (Map.Entry<String, Object> iocEntry : ioc.entrySet()) {
            for (Field iocBeanField : iocEntry.getValue().getClass().getDeclaredFields()) {
                // 如果没有注解 PandaroidAutowired ，则不需要注入，直接跳过看下一个 iocBeanField
                if(!iocBeanField.isAnnotationPresent(PandaroidAutowired.class)) {
                    continue;
                }
                // 有注解 PandaroidAutowired ，需要得到应当 DI 的 beanName
                // 先看注解 PandaroidAutowired 是否指定了 beanName ，如自定义的 beanName（PandaroidService 自定义 beanName）
                String beanName = iocBeanField.getAnnotation(PandaroidAutowired.class).value();
                // 若注解 PandaroidAutowired 未指定 beanName ，则取当前 iocBeanField 类型 Type（Class 名（或 Interface 名））做 beanName
                if(null == beanName || "".equals(beanName.trim())) {
                    beanName = iocBeanField.getType().getName();
                }
                // trim 一下，这里 beanName 已经可以确定不为 null
                beanName = beanName.trim();
                System.out.println("[PandaroidDispatcherServlet doDiAutowired] beanName: " + beanName);
                // DI 注入 Bean
                iocBeanField.setAccessible(true);
                try {
                    // ioc.get(beanName) 获取到 IoC 容器中 beanName 对应的 Bean 实例
                    // 如果 beanName 是接口的全限定名，则 IoC 容器中在 doInitIoc() 中也是以接口的全限定名作为 key 放入的具体实例，所以这里能拿出来
                    iocBeanField.set(iocEntry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    /**
     * ioc : IoC 容器，默认 key 是类名首字母小写 beanName ，value 是对应的实例对象 instance
     */
    /*private Map<String, Object> ioc = new HashMap<String, Object>();
    private void doInitIoc() {
        // 如果 classNames 中什么都没有，则不做 IoC
        if(classNames.isEmpty()) {
            return ;
        }
        // 迭代 classNames ，反射实例化，分 PandaroidController 、PandaroidService 存入 IoC 容器中
        for(String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                System.out.println("[PandaroidDispatcherServlet doInitIoc] clazz: " + clazz);
                // 如果没有标记为 PandaroidController 或 PandaroidService ，则不需要 IoC 反射实例化
                if(!clazz.isAnnotationPresent(PandaroidController.class) && !clazz.isAnnotationPresent(PandaroidService.class)) {
                    continue;
                }
                // PandaroidController 和 PandaroidService 在下面进行 IoC 实例化，放入 IoC 容器 ioc 中
                String beanName = toLowerFirstCase(clazz.getSimpleName());
                System.out.println("[PandaroidDispatcherServlet doInitIoc] beanName: " + beanName);
                Object instance = clazz.newInstance();
                if(clazz.isAnnotationPresent(PandaroidController.class)) {
                    // 因为 PandaroidController 不用 DI 到其他 Bean 中，所以这里直接实例化保存入 IoC 容器 ioc 中即可
                    ioc.put(beanName, instance);
                    continue;
                }
                if(clazz.isAnnotationPresent(PandaroidService.class)) {
                    // PandaroidService 需要 DI 到其他 Bean 中，如：其他 PandaroidController 或 PandaroidService
                    // 所以，PandaroidService 需要分三种情况处理：
                    // 1. 用户自定义的 PandaroidService 名称：解决在不同 package 下出现相同的类名。让用户可以自己定义一个全局唯一的名字
                    String customeBeanName = clazz.getAnnotation(PandaroidService.class).value();
                    // 如果 customeBeanName 不为空，则用 customeBeanName 做 beanName
                    if(null != customeBeanName && !("".equals(customeBeanName.trim()))) {
                        beanName = customeBeanName.trim();
                    }
                    // 2. 默认的类名首字母小写
                    ioc.put(beanName, instance);
                    // 3. 该 Service 的 Interface 名：一个接口如果有多个实现类呢？注入哪一个？如果只有一个就默认选择那一个，如果有多个则抛异常
                    // 首先获取 clazz 的所有接口，进行反射
                    for(Class<?> clazzInterface : clazz.getInterfaces()) {
                        if(ioc.containsKey(clazzInterface.getName())) {
                            throw new Exception("The IoC key [" + clazzInterface.getName() + "] already exists! Please check the duplicated one(s)!");
                        }
                        ioc.put(clazzInterface.getName(), instance);
                    }
                    // continue;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

}
