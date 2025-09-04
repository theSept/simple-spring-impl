package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.TargetSource;
import com.liuxu.springframework.aop.framework.AdvisedSupport;
import com.liuxu.springframework.aop.framework.ReflectiveMethodInvocation;
import com.liuxu.springframework.aop.utils.AopUtils;
import com.liuxu.springframework.utils.ClassUtils;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cglib代理
 * 开启Cglib代理因为JDK9模块化，需要添加jvm参数才行：--add-opens java.base/java.lang=ALL-UNNAMED
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class CglibAopProxy implements AopProxy {

    private static final Logger log = LoggerFactory.getLogger(CglibAopProxy.class);


    private static final int AOP_PROXY = 0; // AOP动态查询对应的方法和拦截器
    private static final int INVOKE_TARGET = 1; // 没有拦截链，反射动态调用目标对象方法即可

    private static final int NOOP_INVOKE = 2; // 不需要处理的方法
    private static final int EQUALS_INVOKE = 3;
    private static final int HASHCODE_INVOKE = 4;


    private AdvisedSupport advised;

    /**
     * 静态（不会动态变更的）目标对象，优化后每个方法对应的固定的拦截映射，
     * key:方法  value:方法对应的Callback在Callback数组中的索引位置
     */
    private Map<Method, Integer> fixedInterceptorMap;

    /**
     * 方法对应的Callback在 Callback 数组中存储的起始索引位置，因为是存在一块的。
     * 一般是在主要的几个拦截器后面
     */
    private int fixedInterceptorOffset;


    /** The CGLIB class separator: {@code "$$"}. */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";


    public CglibAopProxy(AdvisedSupport advisedSupport) {
        this.advised = advisedSupport;
    }

    @Override
    public Object getProxy() {
        return buildProxy(ClassUtils.getDefaultClassLoader(), false);
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        return buildProxy(classLoader, false);
    }

    @Override
    public Class<?> getProxyClass(ClassLoader classLoader) {
        return (Class<?>) buildProxy(classLoader, true);
    }

    private Object buildProxy(ClassLoader classLoader, boolean classOnly) {
        log.info("创建 CGLIB 代理");

        try {
            Class<?> rootClass = this.advised.getTargetSource().getTargetClass();

            // 代理父类
            Class<?> proxySuperClass = rootClass;
            // 如果是CGlib创建的代理类,拿到真正的目标类型,并添加实现的接口
            if (rootClass.getName().contains(CGLIB_CLASS_SEPARATOR)) {
                proxySuperClass = rootClass.getSuperclass();
                Class<?>[] interfaces = rootClass.getInterfaces();
                for (Class<?> additionalInterface : interfaces) {
                    this.advised.addInterfaces(additionalInterface);
                }
            }

            // 配置 CGLIB Enhancer
            Enhancer enhancer = new Enhancer();

            // 代理的父类是谁（要继承哪个类）。
            enhancer.setSuperclass(proxySuperClass);

            if (classLoader != null) {
                enhancer.setClassLoader(classLoader);
            }

            // 代理实现哪些接口。
            enhancer.setInterfaces(AopUtils.completeProxiedInterfaces(this.advised));
            // 类名生成规则（避免冲突）。
            // enhancer.setNamingPolicy();
            // 让 CGLIB 在生成新类前，尝试用类加载器加载是否已有缓存的类，避免重复生成。
            enhancer.setAttemptLoad(true);
            // 如何生成字节码。
            // enhancer.setStrategy();

            Callback[] callbacks = getCallbacks(rootClass);
            Class<?>[] types = new Class[callbacks.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = callbacks[i].getClass();
            }

            // 使用过滤器来决定目标对象的每个方法由哪个Callback负责
            // CallbackFilter.accept(Method) 只在 代理类生成时执行一次，用来决定每个方法走哪个 Callback
            // 调用时：JVM 执行的就是已经生成好的字节码，
            ProxyCallbackFilter proxyCallbackFilter = new ProxyCallbackFilter(this.advised, this.fixedInterceptorMap, this.fixedInterceptorOffset);

            enhancer.setCallbackTypes(types);
            enhancer.setCallbackFilter(proxyCallbackFilter);

            return (classOnly ? createProxyClass(enhancer) : createProxyClassAndInstance(enhancer, callbacks));

        } catch (Exception e) {
            throw new RuntimeException("创建CGLIB代理出现错误：", e);
        }


    }

    /**
     * 创建代理类的类型
     *
     * @param enhancer 创建代理类
     * @return 代理类类型
     */
    protected Class<?> createProxyClass(Enhancer enhancer) {
        // 是否在构建过程中设置拦截机制
        enhancer.setInterceptDuringConstruction(false);
        return enhancer.createClass();
    }


    /**
     * 创建代理类实例
     *
     * @param enhancer  创建代理类
     * @param callbacks 回调
     * @return 代理类实例
     */
    protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
        // 是否在构建过程中设置拦截机制
        enhancer.setInterceptDuringConstruction(false);
        enhancer.setCallbacks(callbacks);
        return enhancer.create();
    }

    protected Enhancer createEnhancer() {
        return new Enhancer();
    }


    /**
     * 获取创建代理需要配置的回调
     *
     * @param rootClass 目标对象
     * @return 回调
     */
    protected Callback[] getCallbacks(Class<?> rootClass) throws Exception {
        boolean isStatic = this.advised.getTargetSource().isStatic();

        boolean isFrozen = this.advised.isFrozen();

        // AOP拦截器，用于切面拦截链的调用
        Callback aopInterceptor = new DynamicAdvisedInterceptor(this.advised);

        Callback[] mainCallbacks = new Callback[]{
                aopInterceptor, // 负责AOP拦截通知链的方法的拦截器
                new DynamicUnadvisedInterceptor(this.advised.getTargetSource()), // 负责动态调用没有拦截通知链的方法的拦截器
                new SerializableNoOp(), // 不处理的方法拦截器
                new EqualsInterceptor(this.advised),
                new HashcodeInterceptor(this.advised)
        };


        Callback[] callbacks;

        if (isStatic && isFrozen) {
            // 静态的目标对象，并且冻结了Advised配置，进行优化处理，
            // 将类中的每个方法的调用都定义 Callback，每次调用不需要在查找计算方法。直接拿对应索引的 Callback 执行
            Object target = this.advised.getTargetSource().getTarget();
            Method[] methods = rootClass.getMethods();
            Callback[] fixedCallbacks = new Callback[methods.length];
            this.fixedInterceptorMap = new HashMap<>(methods.length);

            for (int i = 0; i < fixedCallbacks.length; i++) {
                Method method = methods[i];
                List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, rootClass);
                fixedCallbacks[i] = new FixedChainStaticTargetInterceptor(chain,
                        target, this.advised.getTargetSource().getTargetClass());
                this.fixedInterceptorMap.put(method, i);
            }

            // 将主要的Callback和静态优化后方法的Callback组合在一块
            callbacks = new Callback[mainCallbacks.length + fixedCallbacks.length];
            System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
            System.arraycopy(fixedCallbacks, 0, callbacks, mainCallbacks.length, fixedCallbacks.length);

            // 记录方法对应的固定Callback在数组的索引起始位置
            this.fixedInterceptorOffset = mainCallbacks.length;

        } else {
            callbacks = mainCallbacks;
        }

        return callbacks;
    }


    /**
     * 处理目标方法和拦截链执行完毕后的返回值
     *
     * @param target 目标对象
     * @param proxy  代理对象
     * @param method 执行的方法
     * @param retVal 方法的返回值
     * @return 处理后的返回值
     */
    public static Object processReturnType(Object target, Object proxy, Method method, Object retVal) {
        if (retVal != null && retVal == target &&
                target.getClass().isInstance(proxy)) {
            // 4.1 返回值如果是this,需要用代理对象替代目标对象返回
            retVal = proxy;
        }

        Class<?> returnType = method.getReturnType();
        if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
            // 4.2 返回值是null, 如果原始方法的返回类型如果是 primitive（基本类型），那就绝对不能返回 null
            throw new RuntimeException("由于执行通知部分没有返回值，而方法返回类型又与原始返回类型不匹配，所以出现了错误。方法:" + method);
        }

        return retVal;
    }


    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof CglibAopProxy that &&
                AopUtils.equalsInProxy(this.advised, that.advised)));
    }

    @Override
    public int hashCode() {
        return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
    }


    /**
     * 这个拦截器负责动态反射调用目标对象的方法，当方法没有拦截通知的时候才使用该拦截器（已知这个方法没有拦截通知）
     */
    private static class DynamicUnadvisedInterceptor implements MethodInterceptor, Serializable {

        private final TargetSource targetSource;

        private DynamicUnadvisedInterceptor(TargetSource targetSource) {
            this.targetSource = targetSource;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object target = this.targetSource.getTarget();
            try {
                Object resVal = AopUtils.invokeJoinpointUsingReflection(method, target, args);
                return processReturnType(target, proxy, method, resVal);
            } finally {
                if (target != null) {
                    this.targetSource.releaseTarget(target);
                }
            }
        }
    }


    /**
     * equals() 方法拦截器. 负责处理代理对象执行equals()方法
     */
    private static class EqualsInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        private EqualsInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            // equals() 第一个参数就是要对比的对象
            Object other = args[0];
            if (other == proxy) {
                return true;
            }

            if (other instanceof Factory factory) {
                Callback callback = factory.getCallback(EQUALS_INVOKE);
                // 如果传入的参数是 Factory ,那就拿到它的 EqualsInterceptor Callback, 对比两个对象里面的 切面配置
                return (callback instanceof EqualsInterceptor otherEqualsInterceptor &&
                        AopUtils.equalsInProxy(this.advised, otherEqualsInterceptor.advised));
            }
            return false;
        }
    }


    /***
     * hashCode() 方法拦截器. 负责处理代理对象执行hashCode()方法
     */
    private static class HashcodeInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        private HashcodeInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
        }
    }

    /**
     * 当代理配置冻结后，并且代理的目标对象属于静态资源(不会运行时变更的)，就会用该拦截器来处理目标对象的方法.
     * 这个拦截中已经记录该方法的拦截链
     */
    private static class FixedChainStaticTargetInterceptor implements MethodInterceptor, Serializable {

        private final List<Object> adviceChain;

        private final Object target;

        private final Class<?> targetClass;

        private FixedChainStaticTargetInterceptor(List<Object> adviceChain, Object target, Class<?> targetClass) {
            this.adviceChain = adviceChain;
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            CglibMethodInvocation invocation = new CglibMethodInvocation(method, args, this.target, proxy, this.targetClass, this.adviceChain);
            Object resVal = invocation.proceed();
            return processReturnType(this.target, proxy, method, resVal);
        }
    }


    /**
     * 通用 AOP 回调。当目标是动态的或代理未冻结时使用
     */
    private static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        public DynamicAdvisedInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

            Object target = null;
            TargetSource targetSource = this.advised.getTargetSource();


            try {
                // 是否要线程暴露代理对象,省略不处理...

                target = targetSource.getTarget();
                Class<?> targetClass = (target != null ? target.getClass() : null);

                // 获取匹配的拦截方法链
                List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

                // 调用方法,拿到返回值
                Object retVal;
                if (chain.isEmpty()) {
                    // 没有拦截增强方法,直接调用
                    retVal = AopUtils.invokeJoinpointUsingReflection(method, target, args);
                } else {
                    // 代理有拦截通知
                    retVal = new CglibMethodInvocation(method, args, target, proxy, targetClass, chain).proceed();
                }

                // 校验返回值是否正确
                return processReturnType(target, proxy, method, retVal);
            } finally {
                if (target != null && !targetSource.isStatic()) {
                    targetSource.releaseTarget(target);
                }
            }

        }
    }


    /**
     * 此类负责调用AOP代理的有拦截链的方法
     */
    private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

        public CglibMethodInvocation(Method method, Object[] arguments, Object target, Object proxy, Class<?> targetClass, List<?> interceptorsAndDynamicMethodMatchers) {
            super(method, arguments, target, proxy, targetClass, interceptorsAndDynamicMethodMatchers);
        }

        @Override
        public Object proceed() throws Throwable {
            return super.proceed();
        }
    }


    /**
     * 当前拦截器拦截用作不处理的方法调用该对象
     * 比如: finalize方法,该方法是对象被回收时执行的,不处理
     */
    public static class SerializableNoOp implements NoOp, Serializable {
    }


    /**
     * 代理回调过滤器,作用:决定处理当前方法的 callback 对象
     * 代理对象内存储了所有 callback方法的一个数组, 通过过滤器返回的数值作为索引, 拿到 callback数组对应的 callback 对象用来处理当前的方法.
     * <p>
     * 重点：CallbackFilter.accept(Method) 只在 代理类生成时执行一次，用来决定每个方法走哪个 Callback。
     * 运行时：JVM 执行的就是已经生成好的字节码，
     */
    private static class ProxyCallbackFilter implements CallbackFilter {

        private final AdvisedSupport advised;

        private final Map<Method, Integer> fixedInterceptorMap;

        private final int fixedInterceptorOffset;

        private ProxyCallbackFilter(AdvisedSupport advised, Map<Method, Integer> fixedInterceptorMap, int fixedInterceptorOffset) {
            this.advised = advised;
            this.fixedInterceptorMap = fixedInterceptorMap;
            this.fixedInterceptorOffset = fixedInterceptorOffset;
        }


        @Override
        public int accept(Method method) {
            if (AopUtils.isFinalizeMethod(method)) {
                return NOOP_INVOKE;
            }

            if (AopUtils.isEqualsMethod(method)) {
                return EQUALS_INVOKE;
            }

            if (AopUtils.isHashCodeMethod(method)) {
                return HASHCODE_INVOKE;
            }

            Class<?> targetClass = this.advised.getTargetSource().getTargetClass();
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
            boolean isAdvice = !chain.isEmpty();
            boolean isStatic = this.advised.getTargetSource().isStatic();
            boolean isFrozen = this.advised.isFrozen();

            // 存在拦截链，或者没有冻结配置
            if (isAdvice || !isFrozen) {
                if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(method)) {
                    Integer index = this.fixedInterceptorMap.get(method);
                    // 从Callback存储拦截方法的起始索引开始，计算拿到这个方法在Callback数组中正确的索引位置。
                    return (index + this.fixedInterceptorOffset);
                } else {
                    // 交给AOP代理进行动态查找方法调用
                    return AOP_PROXY;
                }
            } else {
                // 当没有拦截链（Advice） 并且 冻结了配置，就走这个逻辑，直接调用目标方法，
                return INVOKE_TARGET;
            }
        }
    }

}

