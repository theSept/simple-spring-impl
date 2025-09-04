package com.liuxu.springframework.aop.autoproxy;

/**
 * 代理创建时的上下文
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class ProxyCreationContext {

    public ProxyCreationContext() {
    }

    private static final ThreadLocal<String> currentProxiedBeanName = new ThreadLocal<>() {
        final String name = "当前代理的 Bean 的名称";

        @Override
        public String toString() {
            return this.name;
        }
    };

    public static void setCurrentProxiedBeanName(String beanName) {
        currentProxiedBeanName.set(beanName);
    }

    public static String getCurrentProxiedBeanName() {
        return currentProxiedBeanName.get();
    }


}
