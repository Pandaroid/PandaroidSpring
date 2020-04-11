package com.pandaroid.springframework.webmvc.servlet;

import com.pandaroid.springframework.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

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
     * @param req
     * @param resp
     * @throws IOException
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 从 req 中取出 url
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("[PandaroidDispatcherServlet doDispatch] url: " + url);
        System.out.println("[PandaroidDispatcherServlet doDispatch] contextPath: " + contextPath);
        String mvcUrl = url.replace(contextPath, "").replaceAll("/+", "/");
        System.out.println("[PandaroidDispatcherServlet doDispatch] mvcUrl: " + mvcUrl);
        // url 跟 handlerMapping 中进行匹配
        // 如果没有配置，那么 404
        if(!handlerMapping.containsKey(mvcUrl)) {
            resp.getWriter().write("404 Not Found!!!");
            return ;
        }
        Method beanMethod = handlerMapping.get(mvcUrl);
        // 匹配到的 beanMethod invoke
        // 这里还需要对 Parameters 的注解 PandaroidRequestParam 进行处理
        // 获取 Parameters beanMethod.getParameters() ，然后获取带注解的参数，根据注解进行参数填充
        Map<String, String[]> reqParameterMap = req.getParameterMap();
        Parameter[] beanMethodParameters = beanMethod.getParameters();
        // 首先组织 Parameters
        ArrayList<Object> beanMethodInvokeParameters = new ArrayList<>(beanMethodParameters.length);
        // Object[] beanMethodInvokeParameters = new Object[]{req, resp};
        beanMethodInvokeParameters.add(req);
        beanMethodInvokeParameters.add(resp);
        // 迭代 beanMethodParameters ，看是否注解 PandaroidRequestParam
        for (Parameter beanMethodParameter : beanMethodParameters) {
            // 没有 PandaroidRequestParam 注解，不用处理
            if(!beanMethodParameter.isAnnotationPresent(PandaroidRequestParam.class)) {
                continue;
            }
            // 有 PandaroidRequestParam 注解，取出 value 作为请求参数名（即 reqParameterMap 的 key），从 reqParameterMap 中获取请求参数值
            String reqParamName = beanMethodParameter.getAnnotation(PandaroidRequestParam.class).value();
            String[] reqParamValues = reqParameterMap.get(reqParamName);
            // 将 reqParamValue 作为 args Object[] 数组的下一个值
            // 这里暂时只处理单个参数，不处理 Array 类型参数
            String reqParamStrValue = reqParamValues[0];
            Object reqParamValue = reqParamStrValue;
            System.out.println("[PandaroidDispatcherServlet doDispatch] beanMethodParameter.getType(): " + beanMethodParameter.getType());
            System.out.println("[PandaroidDispatcherServlet doDispatch] beanMethodParameter.getType().equals(Integer.class): " + beanMethodParameter.getType().equals(Integer.class));
            // 简单的对类型进行转化
            if(beanMethodParameter.getType().equals(Integer.class)) {
                reqParamValue = Integer.valueOf(reqParamStrValue);
            }
            // 存入参数数组
            beanMethodInvokeParameters.add(reqParamValue);
        }
        // 暴力访问调用 beanMethod.invoke()
        beanMethod.setAccessible(true);
        // 这里获取 beanName ，通过当前 beanMethod 找到它被声明的 Class ，然后获取到 SimpleName（与初始化 IoC 的时候 PandaroidController 的 key 一致）
        String beanName = toLowerFirstCase(beanMethod.getDeclaringClass().getSimpleName());
        Object[] beanMethodInvokeParametersObjects = beanMethodInvokeParameters.toArray();
        System.out.println("[PandaroidDispatcherServlet doDispatch] beanName: " + beanName);
        System.out.println("[PandaroidDispatcherServlet doDispatch] beanMethodInvokeParametersObjects: " + Arrays.toString(beanMethodInvokeParametersObjects));
        try {
            beanMethod.invoke(ioc.get(beanName), beanMethodInvokeParametersObjects);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        // resp 返回结果
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1. 加载配置文件
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        System.out.println("[PandaroidDispatcherServlet init] 1. 加载配置文件 contextConfigLocation：" + contextConfigLocation);
        logger.info("[PandaroidDispatcherServlet init] 1. 加载配置文件 contextConfigLocation：{}", contextConfigLocation);
        doLoadConfig(contextConfigLocation);
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
        doDiAutowired();
        // 5. 初始化 MVC HandlerMappting
        System.out.println("[PandaroidDispatcherServlet init] 5. 初始化 MVC HandlerMapping");
        doInitHandlerMapping();
        System.out.println("[PandaroidDispatcherServlet init] init 完毕。Started Pandaroid Spring");
    }

    /**
     * 实际 Spring 是以 ArrayList 保存 handlerMapping
     * 原因：涉及到正则匹配 url ，用 Map 就没有优势了，还是用 ArrayList 更合适
     * 我们也可以看到其他如 Golang 、Node.js 的 Web 框架中，对于 router 也有类似的涉及：按顺序正则匹配第一个的
     */
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();
    private void doInitHandlerMapping() {
        if(ioc.isEmpty()) {
            return ;
        }
        // 迭代 IoC 容器中的 Bean ，处理 PandaroidController 注解的 Action 其中的 PandaroidRequestMapping 注解
        for (Map.Entry<String, Object> iocEntry : ioc.entrySet()) {
            Class<?> iocBeanClazz = iocEntry.getValue().getClass();
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
            if(null == beanMethods || beanMethods.length == 0) {
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
                if(null == beanMethodUrl || "".equals(beanMethodUrl.trim())) {
                    continue;
                }
                // beanMethodUrl 不为空，则进行完整 url 拼接
                String handlerMappingUrl = "/" + beanClazzUrl + "/" + beanMethodUrl;
                // 这里正则处理，将 handlerMappingUrl 中多个 / 处理为单个 /
                handlerMappingUrl = handlerMappingUrl.replaceAll("/+", "/");
                // 以 handlerMappingUrl 为 key ，以 beanMethod 为 value ，存入 HandlerMapping
                // 处理的时候，根据请求 url 取出对应的 beanMethod ，进行 invoke
                handlerMapping.put(handlerMappingUrl, beanMethod);
                // 打印
                System.out.println("[PandaroidDispatcherServlet doInitHandlerMapping] handlerMapping.put(handlerMappingUrl, beanMethod) handlerMappingUrl: " + handlerMappingUrl);
                System.out.println("[PandaroidDispatcherServlet doInitHandlerMapping] handlerMapping.put(handlerMappingUrl, beanMethod) beanMethod: " + beanMethod);
            }
        }
    }

    private void doDiAutowired() {
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
    }

    /**
     * ioc : IoC 容器，默认 key 是类名首字母小写 beanName ，value 是对应的实例对象 instance
     */
    private Map<String, Object> ioc = new HashMap<String, Object>();
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
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        char aUpper = 'A';
        char zUpper = 'Z';
        char aLower = 'a';
        // 首字母在 A 和 Z 之间的大写字母，进行首字母小写转换
        if(chars[0] >= aUpper && chars[0] <= zUpper) {
            // 首字母 +（小写 - 大写）差值，就可以从大写字母转为小写字母
            chars[0] += (aLower - aUpper);
        }
        // 转回 String
        return String.valueOf(chars);
    }

    private List<String> classNames = new ArrayList<String>();
    private void doScanPackageClasses(String scanPackage) {
        String scanPackageFilePath = File.separator + scanPackage.replaceAll("\\.", File.separator);
        System.out.println("[PandaroidDispatcherServlet doScanPackageClasses] scanPackageFilePath: " + scanPackageFilePath);
        URL url = this.getClass().getClassLoader().getResource(scanPackageFilePath);
        String urlFile = url.getFile();
        System.out.println("[PandaroidDispatcherServlet doScanPackageClasses] urlFile: " + urlFile);
        File classPath = new File(urlFile);
        for(File file : classPath.listFiles()) {
            // 下一级目录，递归
            if(file.isDirectory()) {
                doScanPackageClasses(scanPackage + "." + file.getName());
                continue;
            }
            // 非目录，普通文件，如果是非 .class 文件？应该忽略吧
            if(!file.getName().endsWith(".class")) {
                continue;
            }
            // 非目录，并且是 .class 文件，进行缓存
            String className = file.getName().replace(".class", "");
            // 前面加上 scanPackage ，避免 className 重复
            className = scanPackage + "." + className;
            System.out.println("[PandaroidDispatcherServlet doScanPackageClasses] className: " + className);
            // 可以通过 Class.forName(className) 反射出一个 Class 的实例
            // 在初始化 IoC 容器的时候进行实例化，所以这里先缓存
            classNames.add(className);
        }
    }

    private Properties contextConfig = new Properties();
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:", ""));
        System.out.println("[PandaroidDispatcherServlet doLoadConfig] is: " + is);
        try {
            contextConfig.load(is);
            System.out.println("[PandaroidDispatcherServlet doLoadConfig] contextConfig: " + contextConfig);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
