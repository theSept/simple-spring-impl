package com.liuxu.springframework.beans.beandefinition;

import com.liuxu.springframework.beans.annotion.Primary;
import com.liuxu.springframework.beans.interfaces.BeanDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BeanDefinition
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public abstract class AbstractBeanDefinition implements BeanDefinition {

    /** Bean 的 Class 类型 */
    private volatile Class<?> beanType;

    public static final String SCOPE_DEFAULT = "";

    /** 作用域 */
    private String scope = SCOPE_DEFAULT;

    /** 懒加载 */
    private boolean lazyInit = Boolean.FALSE;

    /** 主要的 {@link Primary} */
    private boolean primary = false;

    /** 当前bean依赖的属性 */
    private String[] dependsOn;

    /** 显示配置的初始化方法. 例: {@code @Bean(initMethod="init")}, 可配置多个例如：{@code <bean destroyMethod="init1,init2">} 或 */
    private String[] initMethodNames;

    /** 显示配置的销毁方法. 例: {@code @Bean(destroyMethod="close")}, 可配置多个例如：{@code <bean destroyMethod="close1,close2">} */
    private String[] destroyMethodNames;


    /** 记录加载时需要注入当前Bean的属性的和属性值 */
    private List<PropertyValue> propertyValues = new ArrayList<>(2);


    /**
     * 常量，指示容器应尝试推断 bean 的 destroy method name 而不是显式指定方法名称。
     * 值 "(inferred)" 专门设计用于在方法名称中包含非法字符，确保不会与具有相同名称的合法命名方法发生冲突。
     * 目前，在销毁方法推理期间检测到的方法名称是"close"和"shutdown"（如果存在于特定 bean 类上）
     */
    public static final String INFER_METHOD = "(inferred)";


    public AbstractBeanDefinition(Class<?> beanType) {
        this.beanType = beanType;
    }


    public AbstractBeanDefinition(AbstractBeanDefinition bd) {
        this.scope = bd.getScope();
        this.dependsOn = bd.getDependsOn();
        this.initMethodNames = bd.getInitMethodNames();
        this.destroyMethodNames = bd.getDestroyMethodNames();
        this.lazyInit = bd.isLazyInit();
        this.beanType = bd.getBeanType();
        this.primary = bd.primary;
        this.propertyValues = bd.propertyValues;
    }

    /**
     * 克隆自身方法
     *
     * @return BeanDefinition 子类
     */
    public abstract AbstractBeanDefinition cloneBeanDefinition();

    @Override
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    @Override
    public boolean isPrimary() {
        return primary;
    }

    @Override
    public Class<?> getBeanType() {
        return beanType;
    }

    @Override
    public void setBeanType(Class<?> beanType) {
        this.beanType = beanType;
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public void setDependsOn(String[] dependsOn) {
        this.dependsOn = dependsOn;
    }

    public void setInitMethodNames(String[] initMethodNames) {
        this.initMethodNames = initMethodNames;
    }

    public void setDestroyMethodNames(String[] destroyMethodNames) {
        this.destroyMethodNames = destroyMethodNames;
    }


    @Override
    public void setLazyInit(Boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    @Override
    public boolean isSingleton() {
        return scope.equals(SCOPE_DEFAULT) || scope.equals(SCOPE_SINGLETON);
    }


    public String[] getDependsOn() {
        return dependsOn;
    }

    public String[] getInitMethodNames() {
        return initMethodNames;
    }

    /**
     * 显示配置的销毁方法列表
     */
    public String[] getDestroyMethodNames() {
        return destroyMethodNames;
    }

    /**
     * 获取显示配置的销毁方法，取第一个
     */
    public String getDestroyMethod() {
        return this.destroyMethodNames != null ? this.destroyMethodNames[0] : null;
    }

    @Override
    public boolean isLazyInit() {
        return lazyInit;
    }

    /**
     * 拿到要注入的属性信息
     */
    @Override
    public List<PropertyValue> getPropertyValues() {
        return propertyValues;
    }

    /**
     * 判断当前BeanDefinition是否有属性信息
     *
     * @return
     */
    @Override
    public boolean hasPropertyValues() {
        return (this.propertyValues != null && !this.propertyValues.isEmpty());
    }
}
