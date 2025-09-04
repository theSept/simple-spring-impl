package com.liuxu.springframework.aop.interceptor;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.aspectj.annotation.DefaultPointcutAdvisor;

/**
 * 用于上下文暴露的拦截器，优先级最高
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public final class ExposeInvocationInterceptor implements MethodInterceptor {

    public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

    // 默认的切面
    public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
        @Override
        public String toString() {
            return ExposeInvocationInterceptor.class.getName() + ".ADVISOR";
        }
    };

    /**
     * 线程上下文，用于存储当前调用
     */
    private static final ThreadLocal<MethodInvocation> invocation = new ThreadLocal<>() {
        private final String name = "Current AOP method invocation";

        @Override
        public String toString() {
            return this.name;
        }
    };

    private ExposeInvocationInterceptor() {
    }

    /**
     * 返回与当前调用关联的 AOP Alliance MethodInvocation 对象
     *
     * @return MethodInvocation
     */
    public static MethodInvocation currentInvocation() {
        MethodInvocation mi = invocation.get();
        if (mi == null) {
            throw new IllegalStateException(
                    "未找到 MethodInvocation：检查 AOP 调用是否正在进行中，并且" +
                            "ExposeInvocationInterceptor 是否位于拦截器链中。具体来说，请注意，" +
                            "具有顺序HIGHEST_PRECEDENCE的通知将在 ExposeInvocationInterceptor 之前执行！" +
                            "此外，必须从同一线程调用 ExposeInvocationInterceptor 和 ExposeInvocationInterceptor.currentInvocation（） " +
                            "必须从同一线程调用");
        }
        return mi;
    }


    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        MethodInvocation oldInvocation = invocation.get();
        // 将当前方法拦截器暴露在线程上下文进行共享
        invocation.set(methodInvocation);
        try {
            return methodInvocation.proceed();
        } finally {
            // 恢复线程上下文
            invocation.set(oldInvocation);
        }
    }
}
