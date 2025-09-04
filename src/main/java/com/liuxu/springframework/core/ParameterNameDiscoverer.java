package com.liuxu.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 用于发现方法和构造函数的参数名称的接口。
 * 参数名称发现并不总是可能的，但可以尝试各种策略，例如查找可能在编译时发出的调试信息，以及查找可选地伴随 AspectJ 注释方法的参数名称注释值。
 *
 * @date: 2025-08-20
 * @author: liuxu
 */
public interface ParameterNameDiscoverer {

    /**
     * 获取方法参数名称
     *
     * @param method 查找参数名称的方法
     * @return 参数名称数组
     */
    String[] getParameterNames(Method method);


    /**
     * 获取构造函数参数名称
     *
     * @param ctor 查找参数名称的构造函数
     * @return 参数名称数组
     */
    String[] getParameterNames(Constructor<?> ctor);

}
