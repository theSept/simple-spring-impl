package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.beandefinition.PropertyValue;

import java.util.List;
import java.util.Map;

/**
 * Bean定义
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface BeanDefinition {

    String SCOPE_SINGLETON = "singleton";

    Class<?> getBeanType();

    void setBeanType(Class<?> beanType);

    void setPrimary(boolean primary);

    boolean isPrimary();

    boolean isSingleton();


    void setScope(String scope);

    boolean isLazyInit();

    void setLazyInit(Boolean lazyInit);

    List<PropertyValue> getPropertyValues();

    boolean hasPropertyValues();
}
