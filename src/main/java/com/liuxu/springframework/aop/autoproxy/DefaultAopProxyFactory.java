package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.framework.AdvisedSupport;
import com.liuxu.springframework.utils.ClassUtils;

import java.lang.reflect.Proxy;

/**
 * 默认的AopProxyFactory实现
 * 可定制自己的实现来决定创建代理的方式
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class DefaultAopProxyFactory implements AopProxyFactory {

    // 单例实现这个类
    public static final DefaultAopProxyFactory INSTANCE = new DefaultAopProxyFactory();

    @Override
    public AopProxy createAopProxy(AdvisedSupport config) {
        // 指定使用CGLIB代理 或 没有使用任何代理接口
        if (config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
            Class<?> targetClass = config.getTargetSource().getTargetClass();
            if (targetClass == null) {
                // 抛出未知异常
                throw new RuntimeException("TargetSource 无法确定目标类：创建代理需要接口或目标");
            }

            // 如果目标类型是代理接口 | JDK生成的代理类 | lambda表达式生成的类型
            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
                return new JdkDynamicAopProxy(config);
            }

            // cglib代理
            return new CglibAopProxy(config);

        } else {
            return new JdkDynamicAopProxy(config);
        }
    }


    /**
     * 判断指定的 {@link AdvisedSupport} 是否没有使用任何接口
     *
     * @param config advisedSupport
     * @return true: 没有使用任何接口
     */
    private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
        Class<?>[] ifcs = config.getProxiedInterfaces();
        return (ifcs.length == 0);
    }

}
