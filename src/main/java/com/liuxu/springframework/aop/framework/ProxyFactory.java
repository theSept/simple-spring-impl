package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.utils.ClassUtils;

/**
 * 最外层：最上层的门面（Facade）。
 * <p>
 * 职责：只管给用户暴露友好 API
 *
 * @date: 2025-08-22
 * @author: liuxu
 */
public class ProxyFactory extends ProxyCreatorSupport {

    public ProxyFactory() {
    }


    /**
     * 、
     * 创建一个新的 ProxyFactory。
     * 将代理给定目标实现的所有接口。
     *
     * @param targetObj 要代理的目标对象
     */
    public ProxyFactory(Object targetObj) {
        setTarget(targetObj);
        setInterfaces(ClassUtils.getAllInterfacesForClass(targetObj.getClass()));
    }


    /**
     * 获取代理对象
     *
     * @return 代理对象
     */
    public Object getProxy() {
        return createAopProxy().getProxy();
    }

    /**
     * 获取代理对象，指定类加载器
     *
     * @param classLoader 类加载器
     * @return 代理对象
     */
    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }

    /**
     * 获取代理类,指定类型加载器
     *
     * @param classLoader 类加载器
     * @return 代理类
     */
    public Class<?> getProxyClass(ClassLoader classLoader) {
        return createAopProxy().getProxyClass(classLoader);
    }


}
