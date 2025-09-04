package com.liuxu.springframework.aop.autoproxy;

/**
 * AOP代理（顶级接口）
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface AopProxy {

    /**
     * 获取代理对象
     *
     * @return 代理对象
     */
    Object getProxy();

    /**
     * 获取代理对象，根据传入的类加载器类,获取代理对象
     *
     * @param classLoader 类加载器
     * @return 代理对象
     */
    Object getProxy(ClassLoader classLoader);

    /**
     * 获取代理类,根据传入的类加载器类,获取代理类型
     *
     * @param classLoader 类加载器
     * @return 代理类
     */
    Class<?> getProxyClass(ClassLoader classLoader);

}
