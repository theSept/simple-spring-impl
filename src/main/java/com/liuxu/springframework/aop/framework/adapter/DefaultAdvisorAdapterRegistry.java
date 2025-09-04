package com.liuxu.springframework.aop.framework.adapter;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.aspectj.annotation.DefaultPointcutAdvisor;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认的Advisor适配注册器的实现
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry {

    List<AdvisorAdapter> advisorAdapters = new ArrayList<>(4);

    public DefaultAdvisorAdapterRegistry() {
        // 默认支持的Advisor适配器
        advisorAdapters.add(new AfterReturningAdvisorAdapter());
        advisorAdapters.add(new MethodBeforeAdvisorAdapter());
    }

    @Override
    public Advisor wrap(Object adviceObject) throws IllegalArgumentException {

        if (adviceObject instanceof Advisor advisor) {
            return advisor;
        }

        if (!(adviceObject instanceof Advice advice)) {
            throw new IllegalArgumentException("advice对象必须是 Advisor 或 Advice");
        }

        if (advice instanceof MethodInterceptor) {
            // 是拦截器，直接包装成对应的切面通知
            return new DefaultPointcutAdvisor(advice);
        }

        // 检查所有适配器是否支持此通知
        for (AdvisorAdapter advisorAdapter : this.advisorAdapters) {
            if (advisorAdapter.supportsAdvice(advice)) {
                return new DefaultPointcutAdvisor(advice);
            }
        }

        throw new IllegalArgumentException("通知对象: " + adviceObject + " 既不是 [com.liuxu.springframework.aop.Advice] 的受支持子接口，也不是 [com.liuxu.springframework.aop.Advisor] 的子接口。");
    }

    @Override
    public MethodInterceptor[] getInterceptor(Advisor advisor) {
        List<MethodInterceptor> methodInterceptors = new ArrayList<>(2);
        Advice advice = advisor.getAdvice();
        if (advice instanceof MethodInterceptor mi) {
            methodInterceptors.add(mi);
        }

        // 遍历适配器，将适配器转换的所有拦截器统计
        for (AdvisorAdapter advisorAdapter : this.advisorAdapters) {
            if (advisorAdapter.supportsAdvice(advice)) {
                methodInterceptors.add(advisorAdapter.getInterceptor(advisor));
            }
        }

        if (methodInterceptors.isEmpty()) {
            throw new IllegalArgumentException("通知对象: " + advice + " 既不是 [com.liuxu.springframework.aop.Advice] 的受支持子接口，也不是 [com.liuxu.springframework.aop.Advisor] 的子接口。");
        }

        return methodInterceptors.toArray(new MethodInterceptor[0]);
    }

    @Override
    public void registerAdvisorAdapter(AdvisorAdapter advisorAdapter) {
        this.advisorAdapters.add(advisorAdapter);
    }
}
