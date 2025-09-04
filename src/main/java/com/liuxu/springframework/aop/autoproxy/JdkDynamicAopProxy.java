package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.TargetSource;
import com.liuxu.springframework.aop.framework.AdvisedSupport;
import com.liuxu.springframework.aop.framework.ReflectiveMethodInvocation;
import com.liuxu.springframework.aop.utils.AopUtils;
import com.liuxu.springframework.utils.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK动态代理 (同时也作为JDK代理对象的方法调用处理器实现类)
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public final class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    // 日志
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JdkDynamicAopProxy.class);

    /** 用于配置此代理的配置 */
    private AdvisedSupport advised;

    /** 代理的接口 */
    private final Class<?>[] proxiedInterfaces;

    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
        this.proxiedInterfaces = AopUtils.completeProxiedInterfaces(advised);
    }

    @Override
    public Object getProxy() {
        return getProxy(ClassUtils.getDefaultClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        log.info("创建 JDK 代理");
        return Proxy.newProxyInstance(classLoader, this.proxiedInterfaces, this);
    }

    @Override
    public Class<?> getProxyClass(ClassLoader classLoader) {
        return null;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 1.准备目标对象
        TargetSource targetSource = this.advised.getTargetSource();
        Object target = null;

        // 如果有必要,就将上下文暴露代理对象

        try {

            // 对equals / hashCode : 默认是不会调用目标对象的equals()方法, 这里拦截后会对比两个对象是否代理了同一个目标对象,如果是的就作为true返回
            // / toString 做了特殊处理. 避免类似日志打印对象会调用toString() 防止执行invoke将拦截链执行一遍
            if (AopUtils.isEqualsMethod(method)) {
                return equals(args[0]);
            } else if (AopUtils.isHashCodeMethod(method)) {
                return hashCode();
            }


            target = targetSource.getTarget();
            Class<?> targetClass = target != null ? target.getClass() : null;
            // 2. 拿到明确的拦截链和动态匹配拦截记录
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

            Object resVal;
            // 3.1 如果拦截链是空,则直接执行目标方法
            if (chain.isEmpty()) {
                resVal = AopUtils.invokeJoinpointUsingReflection(method, target, args);
            } else {
                // 3.2 有拦截链条,创建 ReflectiveMethodInvocation对象,调用所有拦截链
                ReflectiveMethodInvocation rmi = new ReflectiveMethodInvocation(method, args, target, proxy, targetClass, chain);
                resVal = rmi.proceed();
            }

            // 4. 处理返回值
            Class<?> returnType = method.getReturnType();
            if (resVal != null && resVal == target &&
                    returnType != Object.class && returnType.isInstance(proxy)) {
                // 4.1 返回值如果是this,需要用代理对象替代目标对象返回
                resVal = proxy;
            } else if (resVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
                // 4.2 返回值是null, 如果原始方法的返回类型如果是 primitive（基本类型），那就绝对不能返回 null
                throw new RuntimeException("由于执行通知部分没有返回值，而方法返回类型又与原始返回类型不匹配，所以出现了错误。方法:" + method);
            }

            // 5. 返回...
            return resVal;
        } finally {
            // 目标对象源处理,
            if (target != null && !targetSource.isStatic()) {
                targetSource.releaseTarget(target);
            }
            // AOP上下文处理 此处忽略...
        }
    }


    /**
     * 检查代理对象是否是同一个,通过advised,接口,advisor 来判断是否匹配.不会调用目标对象的 equals() 方法
     *
     * @param other
     * @return
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other == null) {
            return false;
        }

        JdkDynamicAopProxy otherProxy;
        if (other instanceof JdkDynamicAopProxy jdkDynamicAopProxy) {
            otherProxy = jdkDynamicAopProxy;
        } else if (Proxy.isProxyClass(other.getClass())) {
            InvocationHandler ih = Proxy.getInvocationHandler(other);
            if (!(ih instanceof JdkDynamicAopProxy jdkDynamicAopProxy)) {
                return false;
            }
            otherProxy = jdkDynamicAopProxy;
        } else {
            // Not a valid comparison...
            return false;
        }


        return AopUtils.equalsInProxy(this.advised, otherProxy.advised);
    }

    @Override
    public int hashCode() {
        return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
    }
}

