package com.pandaroid.springframework.beans.support;

import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidService;
import com.pandaroid.springframework.beans.config.PandaroidBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 负责读取配置文件（根据 configLocation），扫描 scanPackage 下的类，根据配置的注解解析，封装 BeanDefinition
 * @author pandaroid
 */
public class PandaroidBeanDefinitionReader {

    public PandaroidBeanDefinitionReader(String[] configLocations) {
        // 根据 configLocations[0] 加载配置文件
        doLoadConfig(configLocations[0]);
        // 扫描配置文件中 scanPackage 下的类
        String scanPackage = contextConfig.getProperty("scanPackage");
        System.out.println("[PandaroidBeanDefinitionReader PandaroidBeanDefinitionReader] 扫描 scan-package 下相关的类。scanPackage：" + scanPackage);
        doScanPackageClasses(scanPackage);
    }

    public List<PandaroidBeanDefinition> loadBeanDefinitions() {
        List<PandaroidBeanDefinition> beanDefinitions = new ArrayList<>();
        for (String beanClassName : registryBeanClasses) {
            try {
                Class<?> beanClass = Class.forName(beanClassName);
                // 这里还是要按之前的思路，检查注解
                // PandaroidController 注解或 PandaroidService 注解才加载 BeanDefinition
                if(!beanClass.isAnnotationPresent(PandaroidController.class) && !beanClass.isAnnotationPresent(PandaroidService.class)) {
                    continue;
                }
                // 保存类对应的 beanClassName（全限定类名）、beanName
                // 后面 getBean 的时候，就可以通过 beanName 得到 BeanDefinition ，获取到 className 去反射实例化 Bean
                // className 就是 beanClassName
                // PandaroidController 直接放 SimpleName 首字母小写做 beanName
                String beanName = toLowerFirstCase(beanClass.getSimpleName());
                System.out.println("[PandaroidBeanDefinitionReader loadBeanDefinitions] beanName: " + beanName);
                System.out.println("[PandaroidBeanDefinitionReader loadBeanDefinitions] beanClassName: " + beanClassName);
                System.out.println("[PandaroidBeanDefinitionReader loadBeanDefinitions] beanClass.getName(): " + beanClass.getName());
                if(beanClass.isAnnotationPresent(PandaroidController.class)) {
                    beanDefinitions.add(doCreateBeanDefinition(beanName, beanClassName));
                    continue;
                }
                // PandaroidService 分三种情况（SimpleName 首字母小写、自定义、接口）
                // beanName 分以下三种情况：
                // 1. 如果用户于注解 PandaroidService 自定义了 beanName ，则以用户自定义的为 beanName
                //    解决在不同 package 下出现相同的类名。让用户可以自己定义一个全局唯一的名字
                String customeBeanName = beanClass.getAnnotation(PandaroidService.class).value();
                // 如果 customeBeanName 不为空，则用 customeBeanName 做 beanName
                if(null != customeBeanName && !("".equals(customeBeanName.trim()))) {
                    beanName = customeBeanName.trim();
                }
                // 2. 默认是以简单类名 Class getSimpleName() 的首字母小写作为 beanName
                beanDefinitions.add(doCreateBeanDefinition(beanName, beanClassName));
                // 3. 如果是接口，则以接口的全限定名作为其唯一实现类的 Bean 实例的一个 beanName（Spring 5 中可以通过注解 @Primary 、@Qualifier 、@Resource 等调整优先级，当多个 Bean 冲突的情况下）
                for (Class<?> beanClassInterface : beanClass.getInterfaces()) {
                    beanDefinitions.add(doCreateBeanDefinition(beanClassInterface.getName(), beanClassName));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return beanDefinitions;
    }

    private PandaroidBeanDefinition doCreateBeanDefinition(String beanName, String beanClassName) {
        PandaroidBeanDefinition beanDefinition = new PandaroidBeanDefinition();
        beanDefinition.setFactoryBeanName(beanName);
        beanDefinition.setBeanClassName(beanClassName);
        return beanDefinition;
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

    private Properties contextConfig = new Properties();
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:", ""));
        System.out.println("[PandaroidBeanDefinitionReader doLoadConfig] is: " + is);
        try {
            contextConfig.load(is);
            System.out.println("[PandaroidBeanDefinitionReader doLoadConfig] contextConfig: " + contextConfig);
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

    private List<String> registryBeanClasses = new ArrayList<String>();
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
            registryBeanClasses.add(className);
        }
    }
}
