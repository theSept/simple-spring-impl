package com.liuxu.springframework.beans;

import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.ComponentScan;
import com.liuxu.springframework.beans.annotion.Primary;
import com.liuxu.springframework.beans.annotion.Priority;
import com.liuxu.springframework.beans.annotion.Qualifier;
import com.liuxu.springframework.beans.autowirecapable.AbstractAutowireCapableBeanFactory;
import com.liuxu.springframework.beans.beandefinition.GenericBeanDefinition;
import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;
import com.liuxu.springframework.beans.config.DependencyDescriptor;
import com.liuxu.springframework.beans.destroy.DisposableBean;
import com.liuxu.springframework.beans.destroy.DisposableBeanAdapter;
import com.liuxu.springframework.beans.interfaces.Aware;
import com.liuxu.springframework.beans.interfaces.BeanDefinition;
import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistry;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactoryAware;
import com.liuxu.springframework.beans.interfaces.BeanNameAware;
import com.liuxu.springframework.beans.interfaces.BeanPostProcessor;
import com.liuxu.springframework.beans.interfaces.DestructionAwareBeanPostProcessor;
import com.liuxu.springframework.beans.interfaces.InitializingBean;
import com.liuxu.springframework.beans.interfaces.InstantiationAwareBeanPostProcessor;
import com.liuxu.springframework.beans.interfaces.MergedBeanDefinitionPostProcessor;
import com.liuxu.springframework.beans.interfaces.ObjectFactory;
import com.liuxu.springframework.beans.interfaces.SmartInitializingSingleton;
import com.liuxu.springframework.beans.postprocessor.AutowiredAnnotationBeanPostProcessor;
import com.liuxu.springframework.beans.postprocessor.CommonAnnotationBeanPostProcessor;
import com.liuxu.springframework.utils.BeanFactoryUtils;
import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.OrderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @date: 2025-06-20
 * @author: liuxu
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultListableBeanFactory.class);

    /** (一级缓存：成品) 单例对象的缓存：bean 名称 - bean 实例。 */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    /** (二级缓存：半成品) 创建实例后没注入属性对象的缓存：bean 名称 - bean 实例 */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    /** (三级缓存：获取bean实例的工厂[可能返回的是AOP代理]) 单例工厂缓存：Bean 名称 - ObjectFactory */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);


    /** beanName -> Bean定义 */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(15);

    /** beanName -> 合并后的Bean定义 */
    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

    /** Type类型 -> beanName */
    private final Map<Class<?>, List<String>> allBeanNamesByType = new ConcurrentHashMap<>(64);

    /** 别名-> beanName */
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);

    /** 已注册的bean实例名称 */
    private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

    /** BeanPostProcessors to apply. */
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    /** beanDefinition Name list */
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    /** Names of beans that are currently in creation. 当前正在创建的 bean 的名称。 */
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    /** 创建的实例缓存 beanName -> 实例对象 */
    private final ConcurrentMap<String, Object> factoryBeanInstanceCache = new ConcurrentHashMap<>();

    /** 预过滤后处理器的缓存 */
    private BeanPostProcessorCache beanPostProcessorCache;

    /** 标记当前工厂是否在销毁单例中 */
    private boolean singletonsCurrentlyInDestruction = false;

    /** beanName -> 处理该bean的销毁实例 */
    private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();


    public DefaultListableBeanFactory(Class<?> configClass) {
        registerAnnotationConfigProcessors(this, null);
        refresh(configClass);
    }

    public void refresh(Class<?> configClass) {
        // 1.加载所有的 beanDefinition
        ScannerConfigLoadBeanDefinition(configClass);

        // 2.注册BeanPostProcessor
        registerBeanPostProcessors();

        // 3.初始化所有非懒加载的单例实例
        finishBeanFactoryInitialization();

    }

    public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
            "com.liuxu.springframework.beans.postprocessor.AutowiredAnnotationBeanPostProcessor";

    public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
            "com.liuxu.springframework.beans.postprocessor.CommonAnnotationBeanPostProcessor";


    /**
     * 注册相关注解配置的处理器
     *
     * @param beanFactory bean工厂
     * @param resource    资源
     */
    private void registerAnnotationConfigProcessors(BeanFactory beanFactory, Object resource) {
        DefaultListableBeanFactory defaultBeanFactory = (DefaultListableBeanFactory) beanFactory;

        // 注册自动依赖注入的处理器
        if (!beanFactory.containsBean(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition rbd = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
            defaultBeanFactory.registryBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, rbd);
        }

        // 注册常用注解的后处理器
        if (!beanFactory.containsBean(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition rbd = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
            defaultBeanFactory.registryBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, rbd);
        }


    }

    /**
     * 加载配置类上的组件扫描注解 指定的路径下所有使用 Component 注解的类
     *
     * @param configClass 配置类
     */
    public void ScannerConfigLoadBeanDefinition(Class<?> configClass) {
        ComponentScan annotation = configClass.getAnnotation(ComponentScan.class);
        String path = annotation.value();
        if (path.isBlank()) { // 如果没有指定扫描的包路径则扫描当前类所在包路径
            path = configClass.getPackageName();
        }
        // 包及其子包下所有类
        List<Class<?>> classes = ClassUtils.reflectionsFindClassByPath(path);

        ArrayList<String> beanPostProcessNames = new ArrayList<>(classes.size() / 2 + 1);
        for (Class<?> aClass : classes) {
            Component component;
            if (aClass.isAnnotationPresent(Component.class) && (component = aClass.getAnnotation(Component.class)) != null) {
                String beanName = BeanFactoryUtils.generateBeanName(aClass);
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition(aClass);
                beanDefinition.setLazyInit(component.lazyInit());

                if (aClass.isAnnotationPresent(Primary.class)) {
                    beanDefinition.setPrimary(true);
                }

                // BeanPostProcessor 接口实现类
                if (BeanPostProcessor.class.isAssignableFrom(aClass)) {
                    beanPostProcessNames.add(beanName);
                }

                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);
                registerAlias(aClass.getName(), beanName);
            }
        }

        // 类型映射的BeanName
        allBeanNamesByType.put(BeanPostProcessor.class, beanPostProcessNames);
        log.info(">>>>>>> init loading beanDefinition done...");
    }

    /**
     * 注册别名  别名-> beanName
     *
     * @param alias    别名
     * @param beanName beanName
     */
    private void registerAlias(String alias, String beanName) {
        synchronized (this.aliasMap) {
            this.aliasMap.remove(alias);
            this.aliasMap.put(alias, beanName);
        }
    }

    /**
     * 注册BeanPostProcessor（后处理器）
     */
    public void registerBeanPostProcessors() {
        String[] beanPostProcessorNames = getBeanNamesForType(BeanPostProcessor.class);
        // List<String> beanPostProcessorNames = allBeanNamesByType.get(BeanPostProcessor.class);
        log.info("BeanPostProcessor size ={}", beanPostProcessorNames.length);

        ArrayList<BeanPostProcessor> postProcessors = new ArrayList<>(beanPostProcessorNames.length);
        for (String beanPostProcessorName : beanPostProcessorNames) {
            BeanPostProcessor bean = getBean(beanPostProcessorName, BeanPostProcessor.class);
            postProcessors.add(bean);
        }

        postProcessors.forEach(this::addBeanPostProcessor);
        log.info(">>>>>>> registry beanPostProcessor done...");
    }


    /**
     * 初始化所有非懒加载的单例bean
     */
    public void finishBeanFactoryInitialization() {
        // 预先实例化单例bean
        preInstantiateSingletons();
    }

    /**
     * 预先实例化单例bean
     */
    public void preInstantiateSingletons() {
        log.info(">>>>>>>>>>>>>> Pre-instantiating singletons in {}", this);
        List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd.isSingleton() && !bd.isLazyInit()) {
                getBean(beanName);
            }
        }
        log.info(">>>>>>>>>>>>>> Pre-instantiating singletons end");

        // 触发所有 SmartInitializingSingleton bean 的初始化后回调
        for (String beanName : beanNames) {
            Object singletonInstance = getSingleton(beanName, true);
            if (singletonInstance instanceof SmartInitializingSingleton smartInitializingSingleton) {
                smartInitializingSingleton.afterSingletonsInstantiated();
            }
        }
        log.info(">>>>>>>>>>>>>> Pre-instantiating callback method end");
    }


    protected <T> T doGetBean(
            String name, Class<T> requiredType, Object[] args, boolean typeCheckOnly) {
        Object beanInstance;

        String beanName = transformedBeanName(name);
        // 检查bean实例是否已存入缓存
        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null) {// 已存在bean实例

            beanInstance = getObjectForBeanInstance(sharedInstance, beanName, beanName);

        } else {
            /* 创建bean实例*/

            // 获取最终合并的BeanDefinition
            RootBeanDefinition mdb = getMergedLocalBeanDefinition(beanName);

            if (mdb.isSingleton()) {
                // 单例对象
                sharedInstance = getSingleton(beanName, () -> {
                    // 创建实例
                    return createBean(beanName, mdb, args);
                });
                beanInstance = getObjectForBeanInstance(sharedInstance, beanName, beanName);
            } else if (!mdb.isSingleton()) {
                // 处理其他类型的对象
                beanInstance = getObjectForBeanInstance(sharedInstance, beanName, beanName);
            } else {
                beanInstance = getObjectForBeanInstance(sharedInstance, beanName, beanName);
            }

        }

        return adaptBeanInstance(beanName, beanInstance, requiredType);
    }

    /**
     * 合并最终bean定义（spring中会有父子bean定义，子继承夫属性，子覆盖父级属性）
     *
     * @param beanName beanName
     * @return RootBeanDefinition 最终bean定义
     */
    private RootBeanDefinition getMergedLocalBeanDefinition(String beanName) {
        if (!containsBeanDefinition(beanName)) {
            // 不存在 beanName 的 bean 定义
            log.error("beanName {} 不存在 BeanDefinition ", beanName);
            throw new RuntimeException("beanName " + beanName + " 不存在 BeanDefinition");
        }

        // 缓存中获取
        if (this.mergedBeanDefinitions.containsKey(beanName)) {
            return this.mergedBeanDefinitions.get(beanName);
        }

        return getMergedBeanDefinition(beanName, this.beanDefinitionMap.get(beanName));
    }


    /**
     * 合并bean定义
     *
     * @param beanName       beanName
     * @param beanDefinition bean定义
     * @return RootBeanDefinition 最终bean定义
     */
    private RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        // 执行合并 bean 定义
        synchronized (this.mergedBeanDefinitions) {
            // double check
            if (this.mergedBeanDefinitions.containsKey(beanName)) {
                return this.mergedBeanDefinitions.get(beanName);
            }

            RootBeanDefinition mbd = null;

            // RootBeanDefinition 直接可以赋值不必再合并处理
            if (beanDefinition instanceof RootBeanDefinition rbd) {
                mbd = rbd.cloneBeanDefinition();
            }

            // 合并 bean 定义
            if (beanDefinition instanceof GenericBeanDefinition bd) {
                mbd = new RootBeanDefinition(bd);

            }

            if (mbd == null) {
                log.error("beanName {} getMergedLocalBeanDefinition fail.", beanName);
                throw new RuntimeException("beanName " + beanName + " 获取最终beanDefinition失败");
            }

            this.mergedBeanDefinitions.put(beanName, mbd);
            return mbd;
        }

    }

    /**
     * 将Bean实例转换为真正所需的类型
     *
     * @param name         beanName
     * @param bean         实例对象
     * @param requiredType 需转换的类型
     * @param <T>          泛型
     * @return 实际对象
     */
    @SuppressWarnings("all")
    private <T> T adaptBeanInstance(String name, Object bean, Class<?> requiredType) {
        if (requiredType != null && !requiredType.isInstance(bean)) {
            log.error("[adaptBeanInstance] bean 类型是 {} 无法转换 requiredType:{} ", bean.getClass().getName(), requiredType.getName());
            throw new RuntimeException("[adaptBeanInstance] bean 类型是 " + bean.getClass().getName() + " 无法转换 requiredType:" + requiredType.getName());
        }
        return (T) bean;
    }


    /**
     * 获取最终的实例对象
     * 处理的场景：实现了FactoryBean的对象，真正的bean实例类型取决于getObject()方法的返回值。
     *
     * @param beanInstance bean实例对象
     * @param name         用户传入的bean名称
     * @param beanName     容器中beanName
     * @return 最终的bean的实例
     */
    private Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName) {
        // TODO 后续可兼容 FactoryBean 接口，通过 getObject 返回真正的实例；
        return beanInstance;
    }


    private Object getSingleton(String beanName) {
        return getSingleton(beanName, true);
    }

    /**
     * 获取单例bean实例
     *
     * @param beanName            beanName
     * @param allowEarlyReference 是否允许提前引用
     * @return bean实例
     */
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {
            // 从二级缓存中获取 提前暴露的bean
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                synchronized (this.singletonObjects) {
                    // double check
                    singletonObject = this.singletonObjects.get(beanName);
                    if (singletonObject == null) {
                        singletonObject = this.earlySingletonObjects.get(beanName);
                        if (singletonObject == null) {
                            // 从三级缓存中获取bean工厂，创建半成品bean，将其存入二级缓存
                            ObjectFactory<?> objectFactory = this.singletonFactories.get(beanName);
                            if (objectFactory != null) {
                                singletonObject = objectFactory.getObject();
                                this.earlySingletonObjects.put(beanName, singletonObject);
                                this.singletonFactories.remove(beanName);
                            }
                        }
                    }
                }

            }

        }
        return singletonObject;
    }


    /**
     * 添加Bean后处理器
     * - 每添加后处理器时，清除提前过滤的后处理器缓存，确保缓存状态一致性
     *
     * @param beanPostProcessor 后处理器
     */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        synchronized (this.beanPostProcessors) {
            this.beanPostProcessors.remove(beanPostProcessor);
            this.beanPostProcessors.add(beanPostProcessor);
            this.beanPostProcessorCache = null;// 清除缓存
        }

    }

    /**
     * 添加一级缓存
     *
     * @param beanName beanName
     * @param bean     bean实例
     */
    public void addSingleton(String beanName, Object bean) {
        synchronized (this.singletonObjects) {
            this.singletonFactories.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
            this.singletonObjects.put(beanName, bean);
            this.registeredSingletons.add(beanName);
        }
    }

    /**
     * 添加三级缓存（提交暴露bean工厂）
     *
     * @param beanName         beanName
     * @param singletonFactory bean工厂
     */
    protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
        synchronized (this.singletonObjects) {
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
                this.registeredSingletons.add(beanName);
            }
        }
    }

    /**
     * 移除指定名称的单例缓存
     */
    protected void removeSingleton(String beanName) {
        synchronized (this.singletonObjects) {
            if (this.singletonObjects.containsKey(beanName)) {
                this.singletonObjects.remove(beanName);
                this.earlySingletonObjects.remove(beanName);
                this.singletonFactories.remove(beanName);
                this.registeredSingletons.remove(beanName);
            }
        }
    }


    /**
     * 从工厂创建bean实例
     *
     * @param beanName         beanName
     * @param singletonFactory 单例工厂
     * @return bean实例
     */
    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        synchronized (this.singletonObjects) {
            Object singletonObject = this.singletonObjects.get(beanName);

            if (singletonObject == null) {
                if (this.singletonsCurrentlyInDestruction) {
                    log.warn("[getSingleton] beanName: {}. 当前工厂的单例处于销毁状态，不允许创建单例 Bean（不要在执行销毁方法是从 BeanFactory 请求 bean!", beanName);
                    throw new RuntimeException("[getSingleton] beanName: " + beanName + ". 当前工厂的单例处于销毁状态，不允许创建单例 Bean（不要在执行销毁方法是从 BeanFactory 请求 bean!");
                }

                // 创建单例对象前 - 将beanName加入正在创建单例中
                beforeSingletonCreation(beanName);
                boolean newSingleton = false;
                try {
                    // 获取实例
                    singletonObject = singletonFactory.getObject();
                    newSingleton = true;
                } catch (Exception e) {
                    newSingleton = false;
                    throw new RuntimeException("[getSingleton] 创建 bean 创建异常 beanName " + beanName, e);
                } finally {
                    // 创建单例实例后 - 将beanName从正在创建中移除
                    afterSingletonCreation(beanName);
                }
                if (newSingleton) {
                    addSingleton(beanName, singletonObject);
                }

            }

            return singletonObject;
        }
    }


    public Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) {
        RootBeanDefinition mbdToUse = mbd;

        // 1. 尝试获取代理对象
        try {
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                log.info("{} 创建的代理对象：{}", beanName, bean);
                return bean;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 2. 没有代理对象，则创建实例
        try {
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            log.info("最终创建的bean实例： {}", beanInstance);
            return beanInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
        // 1.创建实例
        Object beanInstance = this.factoryBeanInstanceCache.get(beanName);
        if (beanInstance == null) {
            beanInstance = createBeanInstance(beanName, mbd, args);
        }

        Class<?> beanType = beanInstance.getClass();
        // 2.允许后处理器修改合并的 bean 定义。缓存需要注入的属性, 修改beanDefinition中记录的属性
        synchronized (mbd.postProcessingLock) {
            if (!mbd.isPostProcessed()) {
                // 执行后处理器，确定初始化、销毁方法
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            mbd.markAsPostProcessed();
        }


        // 3.提前暴露引用（解决循环依赖）
        boolean earlySingletonExposure = (mbd.isSingleton() && isSingletonCurrentlyInCreation(beanName));
        if (earlySingletonExposure) {
            // 加入三级缓存
            Object earlyBeanInstance = beanInstance;
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, earlyBeanInstance));
        }


        Object exposedObject = beanInstance;
        try {
            // 4.注入属性
            populateBean(beanName, mbd, exposedObject);
            // 5.初始化bean
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        } catch (Exception e) {
            log.error("注入属性 and 执行初始化方法 出现错误..");
            throw new RuntimeException(e);
        }

        // 6.处理提前暴露的引用
        if (earlySingletonExposure) {
            // 查询二级缓存中，当前bean是否已经存在
            Object earlySingleton = getSingleton(beanName, false);
            if (earlySingleton != null) { // 表示bean被其他对象引用了
                if (exposedObject == beanInstance) { // 在给bean注入依赖和调用初始化时，并没有创建新的实例，就以暴露给其他对象的引用作为当前Bean的最终引用
                    exposedObject = earlySingleton;
                } else {
                    log.error("BeanName {} 被作为循环引用的一部分注入到其他 bean 中，但 {} 已被包装新对象，意味着其他对象引用的并不是最终版本的 Bean ", beanName, beanName);
                    throw new RuntimeException("循环依赖的 bean 不是最终版本 bean");
                }
            }
        }

        // 7.注册销毁方法
        registerDisposableBeanIfNecessary(beanName, exposedObject, mbd);

        return exposedObject;
    }


    /**
     * 创建bean实例
     *
     * @param beanName beanName
     * @param mbd      bean定义
     * @param args     参数
     * @return bean实例对象
     */
    private Object createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
        Class<?> beanType = mbd.getBeanType();
        Object beanInstance = null;
        try {
            for (Constructor<?> constructor : beanType.getConstructors()) {
                // 调用无参构造函数创建实例
                if (constructor.getParameterCount() == 0) {
                    beanInstance = constructor.newInstance();
                    break;
                }
            }
            if (beanInstance == null) {
                throw new RuntimeException("创建bean实例，没有无参构造，创建实例失败...");
            }
            // 加入缓存
            factoryBeanInstanceCache.put(beanName, beanType);
            return beanInstance;
        } catch (InstantiationException e) {
            log.error("创建bean实例，实例化异常： {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            log.error("创建bean实例，非法访问异常： {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            log.error("创建bean实例，调用目标方法异常： {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行合并Bean定义后处理器 （用于缓存bean中依赖的属性信息）
     *
     * @param mbd      bean定义
     * @param beanType beanClass类型
     * @param beanName beanName
     */
    protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
        for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
            processor.postProcessMergedBeanDefinition(mbd, beanType, beanName);
        }
    }


    /**
     * 给Bean注入属性
     *
     * @param beanName beanName
     * @param mbd      bean定义
     * @param bean     实例
     */
    private void populateBean(String beanName, RootBeanDefinition mbd, Object bean) {
        if (bean == null) {
            throw new RuntimeException(beanName + "：无法将属性值应用于 null 实例");
        }


        // 1.在 Bean 被实例化之后、属性填充之前回调，（返回false可终止注入属性）
        if (!getBeanPostProcessorCache().instantiationAware.isEmpty()) {
            for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
                if (!bp.postProcessAfterInstantiation(bean, beanName)) {
                    return;
                }
            }
        }

        // 后处理 -进行属性依赖注入
        if (!getBeanPostProcessorCache().instantiationAware.isEmpty()) {
            for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
                bp.postProcessProperties(bean, beanName);
            }
        }
    }


    /**
     * 初始化
     *
     * @param beanName beanName
     * @param bean     实例
     * @param mbd      bean定义
     * @return 初始化后的bean实例
     */
    private Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd) {
        // liuxu: 1.执行 BeanNameAware、BeanFactoryAware 等一些内置功能
        invokeAwareMethods(beanName, bean);

        Object wrappedBean = bean;

        // liuxu: 2.执行 BeanPostProcessor 初始化之前回调后处理器
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        try {
            // liuxu: 3.执行 初始化方法
            invokeInitMethods(beanName, bean, mbd);
        } catch (Exception e) {
            throw new RuntimeException("执行初始化方法出现异常：{}", e);
        }

        // liuxu: 4.执行 BeanPostProcessor 初始化之后回调后处理器
        wrappedBean = applyBeanPostProcessorsAfterInitialization(bean, beanName);

        return wrappedBean;

    }

    /**
     * 注册 bean 的销毁方法
     *
     * @param beanName bean 名称
     * @param bean     bean 对象
     * @param mbd      bean 定义
     */
    private void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
        log.info(">>>>>>>>>>>> invoke disposable method");
        if (mbd.isSingleton() && requiresDestruction(bean, mbd)) {
            registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, mbd,
                    getBeanPostProcessorCache().destructionAware));
        } else {
            // 其他的作用域...
        }

    }

    /**
     * 判断是否需要销毁
     * 如下任意场景都会返回true：
     * - 实现了 DisposableBean 接口
     * - 显示配置或推断出了销毁方法
     * - 该 bean 有匹配的销毁后处理器
     */
    protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
        return ((DisposableBeanAdapter.hasDestroyMethod(bean, mbd) ||
                (hasDestructionAwareBeanPostProcessors() && DisposableBeanAdapter.hasApplicableProcessors(
                        bean, getBeanPostProcessorCache().destructionAware))));
    }

    /**
     * 判断是否存在销毁后处理器
     */
    private boolean hasDestructionAwareBeanPostProcessors() {
        return !getBeanPostProcessorCache().destructionAware.isEmpty();
    }

    /**
     * 注册 bean 对应的销毁方法
     *
     * @param beanName beanName
     * @param bean     销毁 bean 的实例
     */
    public void registerDisposableBean(String beanName, DisposableBean bean) {
        synchronized (this.disposableBeans) {
            this.disposableBeans.put(beanName, bean);
        }
    }


    /**
     * 执行 Aware方法 实现内置功能
     *
     * @param beanName beanName
     * @param bean     实例
     */
    private void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof Aware) {
            if (bean instanceof BeanNameAware beanNameAware) {
                beanNameAware.setBeanName(beanName);
            }

            if (bean instanceof BeanFactoryAware beanFactoryAware) {
                beanFactoryAware.setBeanFactory(this);
            }
        }
    }

    /**
     * 初始化之前 -执行后处理器，返回的实例可能是新实例
     *
     * @param existingBean 创建的实例
     * @param beanName     实例名称
     * @return 创建的实例
     */
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) {
        Object result = existingBean;

        for (BeanPostProcessor beanPostProcessor : this.beanPostProcessors) {
            Object current = beanPostProcessor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {// 没有新的实例返回
                return result;
            }
            result = current;
        }
        return result;
    }

    /**
     * 调用初始化方法
     *
     * @param beanName beanName
     * @param bean     bean实例
     * @param mbd      bean定义
     */
    private void invokeInitMethods(String beanName, Object bean, RootBeanDefinition mbd) throws Exception {

        // 3. 调用属性设置后的回调，实现了initializingBean的bean
        if (bean instanceof InitializingBean initializingBean) {
            log.info("在名为 {} 的 bean 上调用 afterPropertiesSet() ", beanName);
            initializingBean.afterPropertiesSet();
        }

        // 4.调用初始化方法
        if (mbd != null) {
            // TODO init-method 方法
            log.info("=====[TODO] invoke init-method ");
        }

    }


    /**
     * 初始化之后 -执行后处理器，返回的实例可能是新实例
     *
     * @param existingBean 实例
     * @param beanName     beanName
     * @return 实例
     */
    protected Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        for (BeanPostProcessor processor : this.beanPostProcessors) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) { // 没有返回新实例
                return result;
            }
            result = current;
        }
        return result;
    }


    /**
     * 实例化之前 - 执行后处理器 如果返回实例将不会走后面默认的实例化逻辑
     *
     * @param beanClass beanClass
     * @param beanName  beanName
     * @return 实例
     */
    private Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            Object result = bp.postProcessBeforeInstantiation(beanClass, beanName);
            // 实例化前回调如果返回了实例对象，会将其作为代理对象直接返回，不再往后走实例化逻辑。
            if (result != null) {
                return result;
            }
        }
        return null;
    }


    private BeanPostProcessorCache getBeanPostProcessorCache() {
        synchronized (this.beanPostProcessors) {
            BeanPostProcessorCache bppCache = this.beanPostProcessorCache;
            if (bppCache == null) {
                // 创建各种后处理器的缓存
                bppCache = new BeanPostProcessorCache();
                for (BeanPostProcessor beanPostProcessor : this.beanPostProcessors) {
                    if (beanPostProcessor instanceof MergedBeanDefinitionPostProcessor mdbPostProcessor) {
                        bppCache.mergedDefinition.add(mdbPostProcessor);
                    }

                    if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor iaBpp) {
                        bppCache.instantiationAware.add(iaBpp);
                    }

                    if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor daBpp) {
                        bppCache.destructionAware.add(daBpp);
                    }
                }
                this.beanPostProcessorCache = bppCache;
            }

            return bppCache;
        }
    }

    /**
     * 获取提前暴露的Bean引用（三级缓存的Bean工厂方法）
     *
     * @param beanName beanName
     * @param mbd      bean定义
     * @param bean     bean对于的实例
     * @return 返回暴露的bean对象
     */
    protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
        // TODO 可进行代理，返回代理对象
        return bean;
    }


    /**
     * 在实例化之前解决 - 可获取对应的代理对象
     *
     * @param beanName bean名称
     * @param mbd      bean定义
     * @return 返回bean代理对象
     */
    private Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        //  执行实例化之前的后处理器，也可进行代理，返回代理对象
        Object bean = null;
        if (mbd != null) {
            if (!getBeanPostProcessorCache().instantiationAware.isEmpty()) {
                // 后处理器- 实例化之前
                bean = applyBeanPostProcessorsBeforeInstantiation(mbd.getBeanType(), beanName);
                if (bean != null) {
                    // 后处理器 - 初始化之后
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        return bean;
    }


    /**
     * 默认创建单例前 -将bean加入正在创建单例中
     *
     * @param beanName beanName
     */
    public void beforeSingletonCreation(String beanName) {
        if (!this.singletonsCurrentlyInCreation.add(beanName)) {
            throw new RuntimeException("[beforeSingletonCreation]创建bean失败，当前bean已经在注册中..");
        }
    }

    /**
     * 默认创建单例后的回调
     * 将bean从正在创建中移除
     *
     * @param beanName beanName
     */
    protected void afterSingletonCreation(String beanName) {
        if (!this.singletonsCurrentlyInCreation.remove(beanName)) {
            throw new RuntimeException("[afterSingletonCreation]创建bean失败，创建中的beanName移除失败..");
        }
    }

    /**
     * 当前的单例正在创建中
     *
     * @param beanName beanName
     * @return boolean
     */
    public boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }

    /**
     * 尝试转变beanName (如果传入的是别名的话)
     *
     * @param name beanName
     * @return beanName
     */
    private String transformedBeanName(String name) {
        String beanName;
        if ((beanName = this.aliasMap.get(name)) == null) {
            beanName = name;
        }
        return beanName;
    }

    public boolean containsSingleton(String beanName) {
        return this.singletonObjects.containsKey(beanName);
    }

    @Override
    public boolean containsBean(String name) {
        String beanName = transformedBeanName(name);
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
            return true;
        }

        return false;
    }

    @Override
    public Object getBean(String name) {
        return doGetBean(name, null, null, false);
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        return doGetBean(name, requiredType, null, false);
    }

    @Override
    public Class<?> getType(String name) {
        return getType(name, false);
    }

    @Override
    public Class<?> getType(String name, boolean allowFactoryBeanInit) {
        String beanName = transformedBeanName(name);

        // 从单例缓存中获取，检查到二级缓存
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            return beanInstance.getClass();
        }

        // 检查是否存在 Bean 定义
        if (containsBeanDefinition(beanName)) {
            // 合并Bean定义，拿它的Class
            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            return mbd.getBeanType();
        }

        return null;
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        String beanName = transformedBeanName(name);
        // 检查Bean定义
        if (!containsBeanDefinition(beanName)) {
            return false;
        }

        RootBeanDefinition rbd = getMergedLocalBeanDefinition(beanName);
        // 检查类型匹配
        if (!typeToMatch.isAssignableFrom(rbd.getBeanType())) {
            return false;
        }

        return true;
    }

    @Override
    public void registryBeanDefinition(String beanName, Class<?> beanClass) {


    }

    @Override
    public void registryBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
        if (existingDefinition != null) {
            log.info("bean {} 已存在 bean 定义, 无需重复注册", beanName);
        } else {
            synchronized (this.beanDefinitionMap) {
                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);
            }
        }
    }

    /**
     * Internal cache of pre-filtered post-processors.
     * 预过滤后处理器的内部缓存
     *
     * @since 5.3
     */
    static class BeanPostProcessorCache {

        /* 创建bean实例时感知接口 环绕在bean实例化的整个声明周期 */
        final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();

        // final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();
        /* 感知bean销毁的后处理器 */
        final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();

        /* 合并 bean 定义之后，执行的后处理器 */
        final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
    }


    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName,
                                    Set<String> autowiredBeanNames) {

        /** ① 检查缓存，直接获取对象如果存在缓存就会走此 */
        Object shortcut = descriptor.resolveShortcut(this);
        if (shortcut != null) {
            return shortcut;
        }


        // 根据类型查询对应的bean实例映射 key:BeanName value:Bean实例或者Bean的Class
        Map<String, Object> autowireCandidates = findAutowireCandidates(requestingBeanName, descriptor.getDependencyType(), descriptor);
        if (autowireCandidates.isEmpty()) {
            if (descriptor.isRequired()) {
                log.error("给 beanName：{} 的 field:{} 进行依赖注入失败, 预计至少有 1 个 bean 符合自动装配候选条件,类型：{}", requestingBeanName, descriptor.getFieldName(), descriptor.getDependencyType());
                throw new RuntimeException("预计至少有 1 个 bean 符合自动装配候选条件。Dependency annotations: " + Arrays.toString(descriptor.getAnnotations()));
            }
        }

        String autowiredBeanName;
        Object instanceCandidate;
        if (autowireCandidates.size() > 1) { // 存在多个候选对象的场景
            // 匹配最终适合的自动注入候选者
            autowiredBeanName = determineAutowireCandidate(autowireCandidates, descriptor);
            if (autowiredBeanName == null) {
                if (descriptor.isRequired()) {
                    log.error("beanName {} field {}  没有匹配到合适的自动注入 Bean. 类型 {} ", requestingBeanName, descriptor.getFieldName(), descriptor.getDependencyType());
                    throw new RuntimeException("beanName" + requestingBeanName + " field " + descriptor.getFieldName() + "  没有匹配到合适的自动注入 Bean. 类型 " + descriptor.getDependencyType());
                } else {
                    // 自动注入没有匹配到合适的 Bean, 但不是必须的，可不进行处理.
                    return null;
                }
            }
            // 取合适的候选 Bean 实例
            instanceCandidate = autowireCandidates.get(autowiredBeanName);
        } else {
            Map.Entry<String, Object> entry = autowireCandidates.entrySet().iterator().next();
            autowiredBeanName = entry.getKey();
            instanceCandidate = entry.getValue();
        }

        // 记录自动注入的 BeanName
        if (autowiredBeanNames != null) {
            autowiredBeanNames.add(autowiredBeanName);
        }

        // 如果是 Class 类型，说明未创建对象，根据 BeanName 初始化实例对象
        if (instanceCandidate instanceof Class<?> clazz) {
            instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, clazz, this);
        }

        return instanceCandidate;
    }

    /**
     * 根据类型查询对应的bean映射
     *
     * @param beanName     等给属性或参数注入依赖的 beanName
     * @param requiredType 属性或参数需要注入的依赖类型
     * @param descriptor   依赖描述符
     * @return beanName-> bean实例（bean Class）
     */
    private Map<String, Object> findAutowireCandidates(String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
        // 获取所有候选依赖的Bean名称
        String[] candidateName = getBeanNamesForType(requiredType);
        HashMap<String, Object> result = new HashMap<>(candidateName.length);

        for (String candidate : candidateName) {
            // 排除自引用 && 检查是否符合自动装配条件（@Qualifier、泛型等）
            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
                // 加入候选 Map 中
                addCandidateEntry(result, candidate, descriptor, requiredType);
            }
        }

        if (result.isEmpty()) {
            // Spring 中对此降低了匹配策略，再次查询候选的 bean 对象
        }

        return result;
    }


    /**
     * 检查是否自引用
     *
     * @param beanName      当前beanName
     * @param candidateName 候选beanName
     * @return 是否自引用
     */
    private boolean isSelfReference(String beanName, String candidateName) {
        return (beanName != null && candidateName != null &&
                beanName.equals(candidateName));
    }


    /**
     * 检查 BeanName 是否符合依赖描述符的自动装配条件
     *
     * @param beanName   BeanName
     * @param descriptor 依赖描述符
     * @return true:符合依赖描述符的自动装配提交 false:不符合
     */
    private boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor) {
        // 检查Bean定义
        if (!this.beanDefinitionNames.contains(beanName)) {
            return false;
        }

        RootBeanDefinition rbd = getMergedLocalBeanDefinition(beanName);
        // 检查类型匹配
        if (!descriptor.getDependencyType().isAssignableFrom(rbd.getBeanType())) {
            return false;
        }

        // 检查 Qualifier 注解
        for (Annotation fieldAnnotation : descriptor.getAnnotations()) {
            if (fieldAnnotation instanceof Qualifier qualifier) {
                if (!(qualifier.value().equals(beanName) ||
                        // 注解的value可能填写的名称bean1，容器存储的是包名+类名，转换别名再次比较
                        beanName.equals(transformedBeanName(qualifier.value())))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 存储依赖注入的候选对象
     *
     * @param candidates    存储候选对象的 Map
     * @param candidateName 候选对象名称
     * @param descriptor    依赖描述符
     * @param requiredType  依赖类型
     */
    private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
                                   DependencyDescriptor descriptor, Class<?> requiredType) {

        // 检查是否存在完成初始化的单例对象
        if (containsSingleton(candidateName)) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            candidates.put(candidateName, beanInstance);
        } else {
            // 将bean的Class存入
            candidates.put(candidateName, getType(candidateName));
        }

    }


    /**
     * 根据类型获获取对应的BeanName
     *
     * @param type Class
     * @return BeanName[]
     */
    private String[] getBeanNamesForType(Class<?> type) {
        Map<Class<?>, List<String>> cache = this.allBeanNamesByType;
        // 1.缓存获取
        List<String> dbNames = cache.get(type);
        if (dbNames != null && !dbNames.isEmpty()) {
            return dbNames.toArray(new String[]{});
        }

        // 2.缓存未命中，查询所有的beanName
        List<String> result = new ArrayList<>();
        for (String beanName : this.beanDefinitionNames) {
            RootBeanDefinition rbd = getMergedLocalBeanDefinition(beanName);
            if (isTypeMatch(beanName, type)) {
                result.add(beanName);
            }
        }

        // 3.加入缓存
        cache.put(type, result);
        return result.toArray(new String[]{});
    }


    protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        // 1.校验 @Primary 注解,其优先级最高
        String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }

        // 2.校验 @Priority 注解, 按优先级取,值越小优先级越高
        String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
        if (priorityCandidate != null) {
            return priorityCandidate;
        }

        return null;
    }

    /**
     * 获取主要的bean {@link Primary}
     *
     * @param candidates   候选的bean
     * @param requiredType 需要的bean类型
     * @return 主要的beanName 或 null(如果不存在)，存在多个会抛出异常.
     */
    protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            String beanInstance = entry.getKey();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) { // 存在多个主要类(@Primary)
                    boolean candidateBeanLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateBeanLocal && primaryLocal) {
                        log.error("在候选 bean 中找到不止一个 'primary' bean: {}", candidates.keySet());
                        throw new RuntimeException("在候选 bean 中找到不止一个 'primary' bean ：" + candidates.keySet());
                    }
                } else {
                    primaryBeanName = candidateBeanName;
                }
            }

        }
        return primaryBeanName;

    }

    /**
     * 获取候选 bean 中 {@link Priority} 优先级最高的 bean
     *
     * @param candidates   候选 bean
     * @param requiredType 候选 bean 的类型
     * @return 优先级最高的 bean 或者 null(如果不存在) ，如果存在多个，则抛出异常
     */
    protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String highestPriorityBeanName = null;
        Integer highestPriority = null;

        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object candidateInstance = entry.getValue();

            Integer candidatePriority =
                    Optional.ofNullable(candidateInstance)
                            .map(this::getPriority)
                            .orElse(null);

            if (candidatePriority != null) {
                if (highestPriority != null) {
                    if (candidatePriority.equals(highestPriority)) {
                        log.error("在候选 bean 中获取存在 'Priority' value 相同的类 bean: {} ", candidates.keySet());
                        throw new RuntimeException("在候选 bean 中获取存在 'Priority' value 相同的类 bean: {} " + candidates.keySet());
                    } else if (candidatePriority < highestPriority) { // 值越小优先级越高
                        highestPriority = candidatePriority;
                        highestPriorityBeanName = candidateBeanName;
                    }
                } else {
                    highestPriority = candidatePriority;
                    highestPriorityBeanName = candidateBeanName;
                }
            }

            // spring源码写法
            /*if (candidateInstance != null) {
                Integer candidatePriority = getPriority(candidateInstance);
                if (candidatePriority != null) {
                    if (highestPriority != null) {
                        if (highestPriority.equals(candidatePriority)) {
                            log.error("在候选 bean 中获取存在 'Priority' value 相同的类 bean: {} ", candidates.keySet());
                            throw new RuntimeException("在候选 bean 中获取存在 'Priority' value 相同的类 bean: {} " + candidates.keySet());
                        } else if (candidatePriority < highestPriority) {
                            highestPriority = candidatePriority;
                            highestPriorityBeanName = candidateBeanName;
                        }
                    } else {
                        highestPriority = candidatePriority;
                        highestPriorityBeanName = candidateBeanName;
                    }
                }
            }*/

        }
        return highestPriorityBeanName;

    }

    /**
     * 校验是否是主要的类
     * -类是否有 {@link Primary} 注
     *
     * @param beanName     beanName
     * @param beanInstance 实例
     * @return true:是主要的类 false:不是
     */
    protected boolean isPrimary(String beanName, Object beanInstance) {
        String transformedBeanName = transformedBeanName(beanName);
        if (containsBeanDefinition(transformedBeanName)) {
            return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
        }
        return false;
    }


    /**
     * 校验指定 beanName 是否存在 bean定义
     *
     * @param beanName beanName
     * @return true:存在 false:不存在
     */
    private boolean containsBeanDefinition(String beanName) {
        return this.beanDefinitionMap.containsKey(beanName);
    }

    /**
     * 获取Bean的优先级
     *
     * @param beanInstance Bean实例（或者Bean的Class对象）
     * @return 优先级
     */
    protected Integer getPriority(Object beanInstance) {
        if (beanInstance instanceof Class<?> clazz) {
            return OrderUtils.getPriority(clazz);
        }
        return OrderUtils.getPriority(beanInstance.getClass());
    }


    public void close() {
        // spring 中关闭会做很多善后动作.

        // 销毁全部单例
        destroySingletons();
    }


    /**
     * 销毁全部单例
     */
    public void destroySingletons() {
        // 标记执行销毁中
        synchronized (this.singletonObjects) {
            this.singletonsCurrentlyInDestruction = true;
        }

        // 拿到所有存在销毁适配实例的 beanName
        String[] disposableBeanNames = {};
        synchronized (this.disposableBeans) {
            disposableBeanNames = (!(this.disposableBeans.keySet().isEmpty()) ?
                    disposableBeans.keySet().toArray(disposableBeanNames) : disposableBeanNames);
        }

        // 倒序执行销毁
        for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
            destroySingleton(disposableBeanNames[i]);
        }

    }

    /**
     * 销毁指定单例
     *
     * @param beanName beanName
     */
    public void destroySingleton(String beanName) {
        // 移除相关单例缓存
        removeSingleton(beanName);

        DisposableBean disposableBean;
        // 移除销毁缓存
        synchronized (this.disposableBeans) {
            disposableBean = this.disposableBeans.remove(beanName);
        }

        // 销毁bean
        destroyBean(beanName, disposableBean);

    }


    /**
     * 销毁指定 bean
     *
     * @param beanName       beanName
     * @param disposableBean 执行所有销毁方法的适配器实例
     */
    protected void destroyBean(String beanName, DisposableBean disposableBean) {
        // spring 源码中会销毁该 Bean 所有依赖的 Bean
        // 例如：A 依赖于 B ,B依赖于C，销毁顺序为：C -> B -> A
        // 这里就不处理那么细腻了，只销毁自己
        if (disposableBean != null) {
            try {

                disposableBean.destroy();
            } catch (Exception e) {
                log.error("销毁 beanName: {} 出现错误...", beanName);
            }
        }


    }

}
