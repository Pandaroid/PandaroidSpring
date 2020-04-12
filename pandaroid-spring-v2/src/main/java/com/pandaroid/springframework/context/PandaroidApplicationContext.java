package com.pandaroid.springframework.context;

import com.pandaroid.springframework.beans.config.PandaroidBeanDefinition;
import com.pandaroid.springframework.beans.support.PandaroidBeanDefinitionReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 职责：IoC（完成 Bean 创建）、DI
 * @author pandaroid
 */
public class PandaroidApplicationContext {
    /**
     * 方便通过 beanName 找到 BeanDefinition ，然后在 getBean 中根据 BeanDefinition 去创建 Bean
     */
    private Map<String, PandaroidBeanDefinition> beanDefinitionMap = new HashMap<>();
    private PandaroidBeanDefinitionReader beanDefinitionReader;

    /**
     * ApplicationContext 初始化构造函数
     * @param configLocations 配置文件位置，可以有多个可拆分的配置文件
     */
    public PandaroidApplicationContext(String... configLocations) {
        // 1. 加载配置文件。不是 ApplicationContext 的职责，应该交给 BeanDefinitionReader
        beanDefinitionReader = new PandaroidBeanDefinitionReader(configLocations);
        // 2. 解析配置文件，封装成 BeanDefinition
        List<PandaroidBeanDefinition> beanDefinitions = beanDefinitionReader.loadBeanDefinitions();
        // 3. 享元模式缓存 BeanDefinition ：供之后 getBean 使用。先缓存在 beanDefinitionMap 中
        doRegistBeanDefinitions(beanDefinitions);
        // 4. 附加：如果有非 Lazy 延时加载的 Bean ，需要在这里进行 DI
        // 其他 Lazy 延时加载的 Bean ，则在 getBean 的时候进行 DI
        // 根据配置来进行，这里进行非延时加载的 Bean 的 DI ，延时加载的 Bean 在 getBean 中实例化后 DI（不调则不实例化也不 DI）
        doInstantAutowired();
    }

    private void doInstantAutowired() {
        System.out.println("[PandaroidApplicationContext doInstantAutowired] 非 Lazy 延时加载的 Bean ，需要在这里进行 DI");
    }

    private void doRegistBeanDefinitions(List<PandaroidBeanDefinition> beanDefinitions) {

    }

    public Object getBean(String beanName) {
        return null;
    }

    public Object getBean(Class beanClass) {
        return getBean(beanClass.getName());
    }
}
