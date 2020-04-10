package com.pandaroid.springframework.webmvc.servlet;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[PandaroidDispatcherServlet doPost] Web 容器接收请求，进行委派 doDispatch");
        // 根据 url 从 handlerMapping 中找到一个对应的 method 并通过 resp 返回
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {

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
        System.out.println("[PandaroidDispatcherServlet init] 4. 依赖注入 DI");
        doDiAutowired();
        System.out.println("[PandaroidDispatcherServlet init] 5. 初始化 MVC HandlerMapping");
        doInitHandlerMapping();
        System.out.println("[PandaroidDispatcherServlet init] init 完毕。Started Pandaroid Spring");
    }

    private void doInitHandlerMapping() {

    }

    private void doDiAutowired() {

    }

    private void doInitIoc() {
        // 如果 classNames 中什么都没有，则不做 IoC
        if(classNames.isEmpty()) {
            return ;
        }
        // 迭代 classNames ，反射实例化，分 beanName 、className 、interfaceName 存入 IoC 容器中
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
