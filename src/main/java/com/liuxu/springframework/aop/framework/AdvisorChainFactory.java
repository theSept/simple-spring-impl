package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.Advised;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 切面链工厂，获取切面类列表
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface AdvisorChainFactory {
    /**
     * 获取拦截通知 和 动态拦截通知 组成的链
     *
     * @param config      AOP运行时的配置信息
     * @param method      目标方法
     * @param targetClass 目标类
     * @return 拦截通知链
     */
    List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, Class<?> targetClass);

}
