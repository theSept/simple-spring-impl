package com.liuxu.springframework.beans.beandefinition;

import com.liuxu.springframework.beans.annotion.PostConstruct;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 最终的 bean 定义类，注册bean实例时会使用此类
 * - 由最初未注册的 bean 定义{@link GenericBeanDefinition} 合并后将返回此最终 bean 定义类
 * - 最终的bean定义，不允许随意修改
 *
 * @date: 2025-06-25
 * @author: liuxu
 */
public class RootBeanDefinition extends AbstractBeanDefinition {

    /** 外部明确管理的初始化方法. （通过API编程添加）避免重复执行 */
    private Set<String> externallyManagedInitMethods;

    /** 外部明确管理的生命周期 销毁方法，（通过API编程添加）避免重复执行 */
    private Set<String> externallyManagedDestroyMethods;

    /** 后处理lock */
    public final Object postProcessingLock = new Object();


    /** 已执行 mergedPostProcessor 后处理（false表示没有） */
    private boolean postProcessed = false;

    /** 用于缓存已解析的销毁方法名称（也缓存推断结果）：避免重复反射检查 */
    public volatile String resolvedDestroyMethodName;


    public RootBeanDefinition(Class<?> clazz) {
        super(clazz);
    }

    public RootBeanDefinition(AbstractBeanDefinition bd) {
        super(bd);
    }


    /**
     * 标记为已执行 mergedPostProcessor 后处理
     */
    public void markAsPostProcessed() {
        synchronized (this.postProcessingLock) {
            this.postProcessed = true;
        }
    }

    public boolean isPostProcessed() {
        return postProcessed;
    }

    /**
     * 注册外部管理的配置初始化方法
     * - 例如 {@link PostConstruct} 注解
     *
     * @param initMethod
     */
    public void registerExternallyManagedInitMethod(String initMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedInitMethods == null) {
                this.externallyManagedInitMethods = new LinkedHashSet<>(1);
            }
            this.externallyManagedInitMethods.add(initMethod);
        }
    }

    /**
     * 确定指定的方法名称是否为外部管理的初始化方法
     */
    public boolean isExternallyManagedInitMethods(String initMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedInitMethods == null) {
                this.externallyManagedInitMethods = new LinkedHashSet<>(1);
            }
            return this.externallyManagedInitMethods.contains(initMethod);
        }
    }


    /**
     * 注册外部管理的配置销毁方法
     * - 例如 {@link com.liuxu.springframework.beans.annotion.PreDestroy} 注解
     *
     * @param destroyMethod
     */
    public void registerExternallyManagedDestroyMethod(String destroyMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedDestroyMethods == null) {
                this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
            }
            this.externallyManagedDestroyMethods.add(destroyMethod);
        }
    }

    /**
     * 确定指定的方法名称是否为外部管理的销毁方法
     * -spring6 中 @PreDestroy 会存入该集合中
     */
    public boolean isExternallyManagedDestroyMethods(String destroyMethod) {
        synchronized (this.postProcessingLock) {
            if (this.externallyManagedDestroyMethods == null) {
                this.externallyManagedDestroyMethods = new LinkedHashSet<>(1);
            }
            return this.externallyManagedDestroyMethods.contains(destroyMethod);
        }
    }


    public RootBeanDefinition cloneBeanDefinition() {
        return new RootBeanDefinition(this);
    }
}
