package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.TargetSource;
import com.liuxu.springframework.aop.framework.ProxyConfig;
import com.liuxu.springframework.aop.framework.ProxyFactory;
import com.liuxu.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import com.liuxu.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import com.liuxu.springframework.aop.target.SingletonTargetSource;
import com.liuxu.springframework.aop.utils.AopUtils;
import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.destroy.DisposableBean;
import com.liuxu.springframework.beans.interfaces.Aware;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactoryAware;
import com.liuxu.springframework.beans.interfaces.InitializingBean;
import com.liuxu.springframework.beans.interfaces.SmartInstantiationAwareBeanPostProcessor;
import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 继承代理配置{@link ProxyConfig},子类也可以访问和设置代理的属性
 *
 * @date: 2025-08-13
 * @author: liuxu
 */
public abstract class AbstractAutoProxyCreator extends ProxyConfig implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {


    private static final Logger log = LoggerFactory.getLogger(AbstractAutoProxyCreator.class);
    private DefaultListableBeanFactory beanFactory;


    // 记录已通过 TargetSource 创建代理的 bean 的名称，防止重复代理。
    private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    // 哪些 bean 已经被 Spring 判断过是否需要 AOP 代理。 key:bean 的唯一标识 value:true-需要 创建 AOP 代理
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);

    // 缓存代理对象的类型 key:bean 的唯一标识  value:代理对象的Class<?> 类型，不是目标类，而是最终生成的代理类
    private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

    // 记录提前暴露的代理引用。
    private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

    // 缓存 Advisor Bean的名称
    private volatile String[] cachedAdvisorBeanNames;

    /**
     * AdvisorAdapterRegistry 适配器
     */
    private AdvisorAdapterRegistry advisorAdapterRegistry = new DefaultAdvisorAdapterRegistry();


    /**
     * 子类的便利常量：“不代理”的返回值。
     *
     * @see #getAdvicesAndAdvisorsForBean
     */
    protected static final Object[] DO_NOT_PROXY = null;

    /**
     * 子类的便利常量：“代理，没有额外的拦截器，只有常见的拦截器”的返回值。
     *
     * @see #getAdvicesAndAdvisorsForBean
     */
    protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


    /**
     * 后处理操作 - 在实例化之前 可返回自定义的实例对象作为该beanName的实例
     * 检查是否有为该bean自定义了创建实例源, 如果有为该bean创建目标对象, 就马上给bean创建代理对象, 禁止再走后面默认的实例化过程.
     *
     * @param beanClass 实例化的类
     * @param beanName  实例化的名称
     * @return 自定义的实例对象
     */
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        Object cacheKey = getCacheKey(beanName, beanClass);

        if (!StringUtils.isBlank(beanName) || !this.targetSourcedBeans.contains(beanName)) {
            // 检查Spring是否已经判断过此Bean
            if (this.advisedBeans.containsKey(cacheKey)) {
                return null;
            }
            // 检查是否是AOP基础类
            if (isInfrastructureClass(beanClass)) {
                return null;
            }
        }


        // 如果我们有一个自定义 TargetSource，请在此处创建代理。
        // 禁止目标 Bean 的不必要地默认实例化：TargetSource 将以自定义方式处理目标实例。

        // 检查是否有自定义的方式创建目标对象，如果有的话就在此创建代理，不要再执行后续的默认实例化
        TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
        if (targetSource != null) {
            if (StringUtils.isNotBlank(beanName)) {
                this.targetSourcedBeans.add(beanName);
            }

            // 找到该 Bean 适用的拦截器
            Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
            // 创建代理对象
            Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
            // 缓存代理对象的类型
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        }

        return null;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new RuntimeException("beanFactory 类型不是 DefaultListableBeanFactory ,错误的类型：" + beanFactory.getClass().getName());
        }
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }


    /**
     * 后处理操作- 初始化完成之后
     * <p>
     * 初始化完成后,尝试检查bean是否有满足代理,如果满足将会返回bean的代理对象
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @return bean对象 或者 bean的代理对象
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        log.info("后处理器 执行 AbstractAutoProxyCreator.postProcessAfterInitialization()");
        if (bean != null) {
            Object cacheKey = getCacheKey(beanName, bean.getClass());
            if (this.earlyProxyReferences.remove(cacheKey) != bean) { // 如果是循环依赖，依赖这个bean的对象在获取这个bean时已经缓存并尝试创建代理了，不用重新再尝试创建代理
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }

        return bean;
    }


    /**
     * 获取早期暴露的bean引用
     * 该方法会尝试创建bean的代理对象,如果不需要代理,则直接返回bean源对象
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @return bean对象或者bean代理对象
     * @throws Exception 创建代理对象时抛出的异常
     */
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        Object cacheKey = getCacheKey(beanName, bean.getClass());
        this.earlyProxyReferences.put(cacheKey, bean); // 记录早期暴露的bean引用(非代理对象) 表明这个对象还未完全实例，被循环依赖尝试创建代理。
        // 尝试获取代理对象
        return wrapIfNecessary(bean, beanName, cacheKey);
    }

    @Override
    public void postProcessProperties(Object bean, String beanName) {
        // ...
    }

    // 判断是否是AOP模块继承的类
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        return Advice.class.isAssignableFrom(beanClass) ||
                Pointcut.class.isAssignableFrom(beanClass) ||
                Advisor.class.isAssignableFrom(beanClass);
    }

    protected Object getCacheKey(String beanName, Class<?> beanClass) {
        return (StringUtils.isNotBlank(beanName) ? beanName : beanClass);
    }


    /**
     * 如有必要，包装给定的 bean, 指它符合代理条件,将返回bean的代理对象。
     * <p>
     * 如果有适用的拦截器,会将bean对象包装成 {@link SingletonTargetSource} 创建代理
     *
     * @param bean     bean实例
     * @param beanName bean名称
     * @param cacheKey 缓存key
     * @return bean的代理对象, 或者bean原对象
     */
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // log.info("wrapIfNecessary 尝试创建代理");
        // 1. 检查是否已在初始化之前拿到targetSourcedBeans处理过代理
        if (StringUtils.isNotBlank(beanName) && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }

        // 2. 检查该bean是否需要创建代理
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }


        // 获取所有适用该Bean的切面拦截列表
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        if (specificInterceptors != DO_NOT_PROXY) {
            log.info("wrapIfNecessary 需要创建代理 ");
            // 记录已经判断并且赋值 TRUE:标识需要AOP代理
            this.advisedBeans.put(cacheKey, Boolean.TRUE);

            // 将bean实例包装单例目标源, 创建代理对象
            Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            this.proxyTypes.put(cacheKey, proxy.getClass());
            log.info("wrapIfNecessary {} 代理对象创建成功，类型：{}", beanName, proxy.getClass());
            return proxy;
        }

        // 记录已经判断并且赋值 TRUE:标识不需要AOP代理
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }


    /**
     * 获取指定bean适用的切面和通知数组
     *
     * @param beanClass          bean类
     * @param beanName           bean名称
     * @param customTargetSource 自定义目标源
     * @return 切面数组
     */
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
                                                    TargetSource customTargetSource) {
        // 查询所有符合条件的切面
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        if (advisors.isEmpty()) {
            log.info("beanName:{} 没有符合条件的切面", beanName);
            return DO_NOT_PROXY;
        }
        log.info("beanName:{} 有{}个切面通知", beanName, advisors.size());
        return advisors.toArray();
    }


    /**
     * 查找指定 beanClass 匹配的切面
     */
    private List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        // 1. 查询所有候选的切面方法
        List<Advisor> candidateAdvisors = findCandidateAdvisors();

        // 2. 搜索符合指定 bean 的切面
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);

        // 3. 调用钩子：尝试扩展切面列表
        extendAdvisors(eligibleAdvisors);

        // 4. 将切面方法进行排序
        if (!eligibleAdvisors.isEmpty()) {
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }

        return eligibleAdvisors;
    }

    /**
     * 查询匹配的切面方法 ，子类实现。
     * 当前方法是处理容器中手动维护的低级切面类，具体注解实现的高级切面在子类实现
     *
     * @return
     */
    protected List<Advisor> findCandidateAdvisors() {
        // 去看子类
        return findAdvisorBeans();
    }

    /**
     * 搜索给定的候选切面，查找可应用于指定 Bean 的所有切面
     *
     * @param candidateAdvisors 候选切面
     * @param beanClass         bean类型
     * @param beanName          beanName
     * @return 可用的切面
     */
    protected List<Advisor> findAdvisorsThatCanApply(
            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        } finally {
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }

    }


    /**
     * 保留的一个钩子，在返回候选切面前保留一个可修改的构造
     *
     * @param candidateAdvisors 候选切面
     */
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    }


    /**
     * 将切面列表排序后返回
     *
     * @param advisors 切面列表
     * @return 排序后的切面
     */
    protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
        // TODO 后面可考虑每个切面方法排序
        return advisors;
    }


    /**
     * 从容器中获取手动维护的低级切面的 Bean 名称（可忽略此方法）
     *
     * @return 低级切面 Bean 名称
     */
    protected List<Advisor> findAdvisorBeans() {
        // 忽略，这是里是从容器中获取手动维护的低级切面类，只实现简单的注解切面。
        return new ArrayList<>();
    }

    protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
        // 暂时不实现，详细可见Spring源码
        return null;
    }


    // ============================ 代理相关 ==============================


    protected Object createProxy(Class<?> beanClass, String beanName,
                                 Object[] specificInterceptors, TargetSource targetSource) {
        return buildProxy(beanClass, beanName, specificInterceptors, targetSource, false);
    }

    private Object buildProxy(Class<?> beanClass, String beanName,
                              Object[] specificInterceptors, TargetSource targetSource, boolean classOnly) {
        ProxyFactory proxyFactory = new ProxyFactory();
        log.info("buildProxy() beanName:{} 构建代理", beanName);
        // proxyTargetClass: 不管目标类有没有接口，Spring 都要考虑用类代理（CGLIB）。
        if (proxyFactory.isProxyTargetClass()) {

            // 如果目标类本身已经是 JDK 代理类，或者是 lambda（匿名类），还是会走JDK代理
            if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
                for (Class<?> anInterface : beanClass.getInterfaces()) {
                    proxyFactory.addInterfaces(anInterface);
                }
            }

        } else {
            // 没有指定类（CGLIB）代理，进行默认检查

            // 检查目标类实现了的接口，有接口的话Spring 默认会走 JDK 动态代理
            evaluateProxyInterfaces(beanClass, proxyFactory);
        }


        // 设置切面链
        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
        proxyFactory.setAdvisors(advisors);
        proxyFactory.setTargetSource(targetSource); // TODO targetSource，只支持单例，要看在哪儿创建对象

        proxyFactory.setFrozen(false); // 框架内部级别不会冻结配置，保持动态修改
        if (advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);// 已经提前处理
        }


        ClassLoader proxyClassLoader = getProxyClassLoader();

        // 根据 classOnly 参数决定返回代理类还是代理对象。
        return (classOnly ? proxyFactory.getProxyClass(proxyClassLoader) : proxyFactory.getProxy(proxyClassLoader));

    }

    protected ClassLoader getProxyClassLoader() {
        return this.beanFactory.getBeanClassLoader();
    }

    /**
     * 评估代理接口，判断走CGLIB还是JDK代理
     *
     * @param beanClass    目标类
     * @param proxyFactory 代理工厂
     */
    private void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(beanClass, this.beanFactory.getBeanClassLoader());
        boolean flag = false;
        for (Class<?> ifc : interfaces) {
            if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
                    ifc.getMethods().length > 0) {
                flag = true;
                break;
            }

        }
        if (flag) {
            // 走JDK代理
            for (Class<?> anInterface : interfaces) {
                proxyFactory.addInterfaces(anInterface);
            }
        } else {
            // 走目标类代理（CGLIB代理）
            proxyFactory.setProxyTargetClass(true);
        }

    }

    /**
     * 是否是一些配置回调接口
     *
     * @param ifc 接口类
     * @return true/false
     */
    protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
        return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
                AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
    }

    /**
     * 确定给定接口是否是众所周知的内部语言接口，因此不被视为合理的代理接口。
     *
     * @param ifc 接口
     * @return 如果给定接口是内部语言接口，则为 true
     */
    protected boolean isInternalLanguageInterface(Class<?> ifc) {
        return (ifc.getName().equals("groovy.lang.GroovyObject") ||
                ifc.getName().endsWith(".cglib.proxy.Factory") ||
                ifc.getName().endsWith(".bytebuddy.MockAccess"));
    }

    /**
     * 构建Advisor数组
     *
     * @param beanName             bean名称
     * @param specificInterceptors 当前bean明确适用的拦截器
     * @return Advisor数组
     */

    protected Advisor[] buildAdvisors(String beanName, Object[] specificInterceptors) {
        // 在Spring中会有配置全局拦截器名称的处理，将当前bean明确的拦截和全局拦截器组装在一起。
        // 再将组装后的拦截器转换成 Advisor 。


        // 省略全局拦截器的处理....

        // 将所有的拦截器或通知或切面适配成对应的切面
        Advisor[] advisors = new Advisor[specificInterceptors.length];
        for (int i = 0; i < specificInterceptors.length; i++) {
            advisors[i] = this.advisorAdapterRegistry.wrap(specificInterceptors[i]);
        }

        return advisors;
    }

    /**
     * 切面是否已经预先进行了类级别匹配
     *
     * @return 在当前类始终是false，因为没有进行类级别匹配
     */
    protected boolean advisorsPreFiltered() {
        return false;
    }


}


