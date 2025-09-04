package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.Advised;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.PointcutAdvisor;
import com.liuxu.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import com.liuxu.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import com.liuxu.springframework.aop.matches.MethodMatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 默认的AdvisorChainFactory
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class DefaultAdvisorChainFactory implements AdvisorChainFactory {

    /**
     * 单例对象
     */
    public static final DefaultAdvisorChainFactory INSTANCE = new DefaultAdvisorChainFactory();

    /**
     * Advisor适配器
     */
    private final AdvisorAdapterRegistry advisorAdapterRegistry = new DefaultAdvisorAdapterRegistry();

    @Override
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, Class<?> targetClass) {

        // 注意需要使用适配器将Advisor是配成拦截器
        Advisor[] advisors = config.getAdvisors();
        List<Object> interceptorList = new ArrayList<>(advisors.length);
        Class<?> actual = targetClass != null ? targetClass : method.getDeclaringClass();
        // 省略：不处理类型增强.....

        // 1. 遍历所有Advisor，检查切点是否匹配
        for (Advisor advisor : advisors) {
            // 条件一：如果是 PointcutAdvisor
            if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
                // 1.1 类匹配，可检查是否预过滤
                if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actual)) {

                    // 1.2 方法过滤器，过滤通过：
                    // 根据方法是否需要动态匹配，不需要直接存储拦截器，需要的话将方法匹配器和拦截器组合一并存储
                    MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
                    if (mm.matches(method, actual)) {
                        if (mm.isRuntime()) {
                            // 需要运行时动态匹配
                            MethodInterceptor[] interceptor = advisorAdapterRegistry.getInterceptor(advisor);
                            for (MethodInterceptor mi : interceptor) {
                                interceptorList.add(new InterceptorAndDynamicMethodMatcher(mi, mm));
                            }

                        } else {
                            MethodInterceptor[] interceptor = advisorAdapterRegistry.getInterceptor(advisor);
                            interceptorList.addAll(Arrays.asList(interceptor));
                        }

                    }

                }

            } else if (false) {

                // 条件二：如果是引用增强。
                // 2.1 进行类级别匹配，通过直接存入静态匹配集合中
            } else {
                // else 兜底：不匹配，直接存入拦截集合中
                MethodInterceptor[] interceptor = advisorAdapterRegistry.getInterceptor(advisor);
                interceptorList.addAll(Arrays.asList(interceptor));
            }


        }

        return interceptorList;
    }
}
