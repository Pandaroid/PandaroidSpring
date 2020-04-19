package com.pandaroid.springframework.context;

import com.pandaroid.springframework.annotation.PandaroidAutowired;
import com.pandaroid.springframework.annotation.PandaroidController;
import com.pandaroid.springframework.annotation.PandaroidService;
import com.pandaroid.springframework.beans.PandaroidBeanWrapper;
import com.pandaroid.springframework.beans.config.PandaroidBeanDefinition;
import com.pandaroid.springframework.beans.support.PandaroidBeanDefinitionReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 职责：IoC（完成 Bean 创建）、DI
 * @author pandaroid
 */
public class PandaroidApplicationContext {
    /**
     * 方便通过 beanName 找到 BeanDefinition ，然后在 getBean 中根据 BeanDefinition 去创建 Bean
     */
    private Map<String, PandaroidBeanDefinition> beanDefinitionMap = new HashMap<>();
    /**
     * 读取配置文件（properties 、xml 、xml）
     * 扫描相关的类，将类上注解的配置以 BeanDefinition 保存到内存中
     */
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
        try {
            doRegistBeanDefinitions(beanDefinitions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 4. 附加：如果有非 Lazy 延时加载的 Bean ，需要在这里进行 DI
        // 其他 Lazy 延时加载的 Bean ，则在 getBean 的时候进行 DI
        // 根据配置来进行，这里进行非延时加载的 Bean 的 DI ，延时加载的 Bean 在 getBean 中实例化后 DI（不调则不实例化也不 DI）
        doInstantAutowired();
    }

    private void doInstantAutowired() {
        System.out.println("[PandaroidApplicationContext doInstantAutowired] 非 Lazy 延时加载的 Bean ，需要在这里进行 DI");
        // 这里还处在配置阶段，beanDefinitionMap 是配置文件和扫描类注解后，配置文件保存在内存中的形式
        for (Map.Entry<String, PandaroidBeanDefinition> beanDefinitionMapEntry : beanDefinitionMap.entrySet()) {
            // 这个 beanDefinitionMapKey 实际对应的是 getBean() 中的参数 beanName
            String beanDefinitionMapKey = beanDefinitionMapEntry.getKey();
            // 调用 getBean() 进行 DI ，这一步才进行真正的实例化
            getBean(beanDefinitionMapKey);
        }
    }

    private void doRegistBeanDefinitions(List<PandaroidBeanDefinition> beanDefinitions) throws Exception {
        for (PandaroidBeanDefinition beanDefinition : beanDefinitions) {
            // 检查：如果 beanDefinitionMap 中已经存在了，则抛出异常
            String factoryBeanName = beanDefinition.getFactoryBeanName();
            if(beanDefinitionMap.containsKey(factoryBeanName)) {
                throw new Exception("[PandaroidApplicationContext doRegistBeanDefinitions] Duplicated key exists!!! factoryBeanName: " + factoryBeanName);
            }
            // 每个 beanDefinition 用于 getBean 中 IoC 和 DI 时，可以通过 beanName（自定义、首字母小写 SimpleName 、接口全限定类名）找到 beanDefinition
            beanDefinitionMap.put(factoryBeanName, beanDefinition);
            // 检查：如果 beanDefinitionMap 中已经存在了，则抛出异常
            String beanClassName = beanDefinition.getBeanClassName();
            if(beanDefinitionMap.containsKey(beanClassName)) {
                // 这里 continue 是因为如果是接口对应的实现类的，是多个 beanName 对应同一个 beanDefinition
                // 上面 beanName 不会重复，正常情况下，但是这里是同样的 Bean 实例的 BeanClassName ，就会发生重复，所以 continue 忽略即可，或者放行覆盖（没有覆盖的必要，都是一样的效果）
                continue;
                // throw new Exception("[PandaroidApplicationContext doRegistBeanDefinitions] Duplicated key exists!!! beanClassName: " + beanClassName);
            }
            // 也可以通过 Bean 的全限定类名找到 beanDefinition
            beanDefinitionMap.put(beanClassName, beanDefinition);
            // 以上，多个 key 可以索引到同一个单例的 BeanDefinition
        }
    }

    /**
     * Spring 保存 BeanWrapper 对象的容器
     */
    private Map<String, PandaroidBeanWrapper> factoryBeanInstanceCache = new HashMap<>();
    /**
     * Spring 保存原生对象的容器，为了保证安全
     */
    private Map<String, Object> factoryBeanObjectCache = new HashMap<>();
    /**
     * Bean 的实例化、DI 都是从这个方法开始的
     * 初始化 IoC 容器 factoryBeanInstanceCache
     * 并且实例化 Bean 的 instance ，封装到 BeanWrapper 中，并保存到 IoC 容器中
     * 完成 DI 注入，解决循环依赖问题
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
        // 检查 factoryBeanInstanceCache 中是否已经有了 beanWrapper ，如果有了，则直接取出返回即可
        if(factoryBeanInstanceCache.containsKey(beanName)) {
            return factoryBeanInstanceCache.get(beanName).getWrappedInstance();
        }
        // 1. 先拿到配置信息： beanName 对应的 BeanDefinition
        PandaroidBeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        // 2. 反射实例化
        Object instance = instantiateBean(beanName, beanDefinition);
        // 3. 封装成 BeanWrapper
        PandaroidBeanWrapper beanWrapper = new PandaroidBeanWrapper(instance);
        // 4. 保存到 IoC 容器中
        factoryBeanInstanceCache.put(beanName, beanWrapper);
        // 注意，这里还没有 DI ，必须完成 DI 后再 return 出去
        // 5. 执行依赖注入 DI
        populateBean(beanName, beanDefinition, beanWrapper);
        return instance;
    }

    /**
     * DI 依赖注入，可能涉及到循环依赖
     * @param beanName
     * @param beanDefinition
     * @param beanWrapper
     */
    private void populateBean(String beanName, PandaroidBeanDefinition beanDefinition, PandaroidBeanWrapper beanWrapper) {
        // DI 可能涉及到循环依赖，如何解决？
        // 用两个缓存，循环两次
        // 1. 把第一次读取结果为空的 BeanDefinition 保存到第一个缓存 (如果读取不为空则直接赋值)
        // 2. 等第一次循环之后，第二次循环，再检查第一个缓存中读取结果为空的，迭代，再进行获取和赋值
        // 对于上面方案的理解：如果有两个对象 A {B b} 和 B {A a} ，第一次循环，A DI 不了 B ，因为 B 还未初始化（未 getBean(B)），此时将 A 先放入缓存
        // 而 B 肯定能初始化，因为 A 此时已经存在于缓存中了，所以第一次循环 B 完成了初始化（DI A 也完成了）
        // 然后第二次循环，把缓存的未成功初始化的 A 取出，再重新执行一遍 getBean ，此时 DI B 是能成功的了
        // 因为 B 中 DI 的 A 是单例（现在只考虑默认的单例的情况，因为单例我们用的最多，而 prototype 在 Spring 中也无法很好的解决循环依赖（其实也可以通过缓存解决））
        // 以上是参考《Spring 5 核心原理之 30 个类手写 Spring》的标准解决方案（当前解决不了就增加层次来解决），以上方案对于环形依赖也是能解决的，
        // 因为用到了 Spring 中的一些思想，比如先提供未初始化完成的对象的引用、利用容器缓存当前正在初始化的对象，所以第一次循环所有对象都要么跟 B 一样完成了初始化，
        // 要么跟 A 一样现在还未完成初始化但已经实例化了可以被引用到，于是在第二次循环中，只要配置的 BeanDefinition 正确能找到，就一定能将跟 A 一样的对象完全 DI 初始化。
        // 在下面 DI (autowiredBeanWrapper == null) 时，我有写下我猜想的另一种思路，也是基于当前代码的可用解决方案，之后更进一步阅读 Spring 源码后，进行验证或推翻
        // 现在细想，其实于两个缓存循环两次是一样的，类似它用栈我用递归，思路是一致的，区别可能在于编码复杂度和运行时的可控程度和性能等方面
        // 如果用户恶意要造成 StackOverFlow ，双缓存双循环不会出现问题，永远是可控的多一个缓存和多一次循环
        Object instance = beanWrapper.getWrappedInstance();
        Class<?> beanClass = beanWrapper.getWrappedClass();
        // 必须要注解 PandaroidController 或者 PandaroidService 才能 DI
        // 在 Spring 中是 Component 注解，Controller 和 Service 都是它的子类
        if(!beanClass.isAnnotationPresent(PandaroidController.class) && !beanClass.isAnnotationPresent(PandaroidService.class)) {
            return ;
        }
        for (Field iocBeanField : beanClass.getDeclaredFields()) {
            // 如果没有注解 PandaroidAutowired ，则不需要注入，直接跳过看下一个 iocBeanField
            if(!iocBeanField.isAnnotationPresent(PandaroidAutowired.class)) {
                continue;
            }
            // 有注解 PandaroidAutowired ，需要得到应当 DI 的 autowiredBeanName
            // 先看注解 PandaroidAutowired 是否指定了 autowiredBeanName ，如自定义的 autowiredBeanName（PandaroidService 自定义 autowiredBeanName）
            String autowiredBeanName = iocBeanField.getAnnotation(PandaroidAutowired.class).value();
            // 若注解 PandaroidAutowired 未指定 autowiredBeanName ，则取当前 iocBeanField 类型 Type（Class 名（或 Interface 名））做 autowiredBeanName
            if(null == autowiredBeanName || "".equals(autowiredBeanName.trim())) {
                autowiredBeanName = iocBeanField.getType().getName();
            }
            // trim 一下，这里 autowiredBeanName 已经可以确定不为 null
            autowiredBeanName = autowiredBeanName.trim();
            System.out.println("[PandaroidDispatcherServlet populateBean] autowiredBeanName: " + autowiredBeanName);
            // DI 注入 Bean
            iocBeanField.setAccessible(true);
            try {
                // 如果获取到的 BeanWrapper 是 null ，说明找不到对应的，则不用注入
                PandaroidBeanWrapper autowiredBeanWrapper = factoryBeanInstanceCache.get(autowiredBeanName);
                if(autowiredBeanWrapper == null) {
                    // 这里应该可以再 getBean 一次？看是否能获取到 autowiredBeanName 对应的 Bean ？
                    // 这里 getBean 的话，也是要注意循环依赖问题，解决方案：
                    // 这里增加一个容器保存当前正在进行 DI 的 instance ，每次进入 getBean 之前检查是否已经存在于该容器中了，如果有循环，无法解决，只有抛出异常
                    // 如果正常 getBean 得到了 autowiredBeanName 对应的 autowiredBeanWrapper ，则继续下面的注入
                    // 在这里其实可以自然解决循环依赖问题，为什么呢？
                    // 因为如果 A {B b} 和 B {A a} ，getBean(A) 的时候，A 实例化并封装 BeanWrapper 、加入 factoryBeanInstanceCache 了，populateBean DI 时，
                    // 发现从 factoryBeanInstanceCache 获取 B 为 null ，于是在这里进行 getBean(B)
                    // 于是，B 进入 getBean ，B 实例化并封装 BeanWrapper 、加入 factoryBeanInstanceCache 了，populateBean DI 时，
                    // 发现从 factoryBeanInstanceCache 获取 A 是获取得到的（因为 A 虽然没 DI 完成，但是 Bean 对象已经存在且加入 factoryBeanInstanceCache 了）
                    // 于是 B 成功的注入了 A a 属性，B 的 populateBean 结束后，getBean(B) 方法出口结束，再次执行 autowiredBeanWrapper = factoryBeanInstanceCache.get(autowiredBeanName)
                    // 就能获取到 autowiredBeanWrapper 了，这是正常情况
                    // 异常情况 getBean(B) 执行后没能成功（可能找不到 BeanDefinition 、无法实例化等），得到 autowiredBeanWrapper 为 null ，那就还是跳过当前 field 并打印 err
                    // 需要检查配置是否有问题，名字写错了，Bean named "xxx" not found
                    // 是否会出现死循环 stackOverFlow 等问题？现在看不会，即使是多重环依赖，从起点 A 开始，最终到终点 X 的时候（X 依赖 A），环上的所有元素的引用都加入 factoryBeanInstanceCache 了，
                    // （只是所有 Bean 此时都递归 getBean() 等待循环依赖的元素 DI 完成），X factoryBeanInstanceCache.get(A) 是没有问题的，所以 X 成功 DI 了 A（A 此时还等待 getBean(B) 结果以便 DI）
                    // 然后 X DI 完成开始返回，一直返回到 A ，A 此时 factoryBeanInstanceCache.get(B) 也成功得到了 autowiredBeanWrapper ，成功 DI
                    // 如果我的这个思路之后验证有问题，再使用 Spring 源码中的多级缓存进行解决（猜想和个人设计难免有误，学无止境）
                    // 以上都是基于 Spring 默认的单例 scope
                    System.out.println("[PandaroidDispatcherServlet populateBean] autowiredBeanWrapper is null!!! Do getBean(autowiredBeanName) to retry!!! autowiredBeanName: " + autowiredBeanName);
                    getBean(autowiredBeanName);
                    autowiredBeanWrapper = factoryBeanInstanceCache.get(autowiredBeanName);
                    if(autowiredBeanWrapper == null) {
                        System.err.println("[PandaroidDispatcherServlet populateBean] autowiredBeanWrapper is null!!! The autowiredBeanName not found for DI!!! Skip to next field!!! autowiredBeanName: " + autowiredBeanName);
                        continue;
                    }
                }
                // ioc.get(autowiredBeanName) 获取到 IoC 容器中 autowiredBeanName 对应的 Bean 实例
                // 如果 autowiredBeanName 是接口的全限定名，则 IoC 容器中在 doInitIoc() 中也是以接口的全限定名作为 key 放入的具体实例，所以这里能拿出来
                iocBeanField.set(instance, autowiredBeanWrapper.getWrappedInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建真正的原生实例
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private Object instantiateBean(String beanName, PandaroidBeanDefinition beanDefinition) {
        // 如果已经初始化了单例的 Bean 实例，则直接取出 return
        if(factoryBeanObjectCache.containsKey(beanName)) {
            return factoryBeanObjectCache.get(beanName);
        }
        try {
            Class<?> beanClass = Class.forName(beanDefinition.getBeanClassName());
            Object instance = beanClass.newInstance();
            factoryBeanObjectCache.put(beanName, instance);
            return instance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object getBean(Class beanClass) {
        return getBean(beanClass.getName());
    }

    public String toLowerFirstCase(String simpleName) {
        return beanDefinitionReader.toLowerFirstCase(simpleName);
    }

    public int getBeanDefinitionCount() {
        return beanDefinitionMap.size();
    }

    public String[] getBeanDefinitionNames() {
        return beanDefinitionMap.keySet().toArray(new String[getBeanDefinitionCount()]);
    }
}
