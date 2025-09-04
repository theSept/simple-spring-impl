package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.Advised;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.TargetSource;
import com.liuxu.springframework.aop.target.SingletonTargetSource;
import com.liuxu.springframework.utils.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 继承第二层：负责管理 AOP 的核心元数据。
 * 职责：只管保存元数据（Advisor/Target）
 *
 * @date: 2025-08-12
 * @author: liuxu
 */

public class AdvisedSupport extends ProxyConfig implements Advised {

    /**
     * 切面名单。如果添加了通知，则在添加到此列表中之前，它将被包装在切面中。
     */
    private List<Advisor> advisors = new ArrayList<>();

    /**
     * 代理要实现的接口。在列表中保持注册顺序，以创建具有指定接口顺序的 JDK 代理。
     */
    private List<Class<?>> interfaces = new ArrayList<>();

    /**
     * 切面拦截通知链工厂，获取拦截链
     */
    private AdvisorChainFactory advisorChainFactory;

    /**
     * 目标对象源
     */
    private TargetSource targetSource;

    /**
     * 是否已预先过滤（预先进行了类匹配并且匹配过通过）
     */
    private boolean preFiltered = false;

    /**
     * 缓存，将 Method 作为键，将 advisor 链 List 作为值。
     */
    private transient Map<MethodCacheKey, List<Object>> methodCache;


    public AdvisedSupport() {
        this.methodCache = new ConcurrentHashMap<>(32);
        this.advisorChainFactory = DefaultAdvisorChainFactory.INSTANCE;
    }


    /**
     * 在现有的接口配置上再添加接口
     *
     * @param interfaces 新的接口
     */
    public void addInterfaces(Class<?> interfaces) {
        if (!interfaces.isInterface()) {
            throw new IllegalArgumentException("指定的类必须是接口");
        }
        if (!this.interfaces.contains(interfaces)) {
            this.interfaces.add(interfaces);
            adviceChanged();// 清除缓存
        }
    }

    /**
     * 清空之前的接口信息，设置代理目标对象实现的接口
     *
     * @param interfaces 目标对象实现的接口
     */
    public void setInterfaces(Class<?>... interfaces) {
        this.interfaces.clear();

        for (Class<?> anInterface : interfaces) {
            addInterfaces(anInterface);
        }
    }

    public Class<?>[] getProxiedInterfaces() {
        return ClassUtils.toClassArray(this.interfaces);
    }


    public void setTarget(Object target) {
        this.targetSource = new SingletonTargetSource(target);
    }

    /**
     * 获取指定方法的拦截链
     *
     * @param method      方法
     * @param targetClass 目标类
     * @return
     */
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
        MethodCacheKey cacheKey = new MethodCacheKey(method);
        List<Object> cached = methodCache.get(cacheKey);
        if (cached == null) {
            // 获取拦截链
            cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(this, method, targetClass);
            this.methodCache.put(cacheKey, cached);
        }

        return cached;
    }


    @Override
    public void setTargetSource(TargetSource targetSource) {
        this.targetSource = targetSource;
    }

    @Override
    public TargetSource getTargetSource() {
        return this.targetSource;
    }

    @Override
    public void setPreFiltered(boolean preFiltered) {
        this.preFiltered = preFiltered;
    }

    @Override
    public boolean isPreFiltered() {
        return this.preFiltered;
    }

    @Override
    public Advisor[] getAdvisors() {
        return this.advisors.toArray(new Advisor[0]);
    }

    public void setAdvisors(Advisor... advisors) {
        addAdvisors(Arrays.asList(advisors));
    }


    public void addAdvisors(Collection<Advisor> advisors) {
        if (isFrozen()) {
            throw new IllegalStateException("一旦冻结，就无法将新的顾问添加到 proxy-config");
        }

        if (advisors != null && !advisors.isEmpty()) {
            this.advisors.addAll(advisors);
            adviceChanged();
        }
    }


    /**
     * 在通知发生更改时调用
     */
    protected void adviceChanged() {
        this.methodCache.clear();
    }

    public int getAdvisorCount() {
        return this.advisors.size();
    }

    /**
     * 方法缓存key
     */
    private static final class MethodCacheKey implements Comparable<MethodCacheKey> {
        private final Method method;

        private final int hashCode;

        public MethodCacheKey(Method method) {
            this.method = method;
            this.hashCode = method.hashCode();
        }

        public boolean equals(Object other) {
            return (this == other || (other instanceof MethodCacheKey that && this.method == that.method));
        }


        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        public String toString() {
            return this.method.toString();
        }

        @Override
        public int compareTo(MethodCacheKey other) {
            int result = this.method.getName().compareTo(other.method.getName());
            if (result == 0) {
                result = this.method.toString().compareTo(other.method.toString());
            }
            return result;
        }
    }


}
