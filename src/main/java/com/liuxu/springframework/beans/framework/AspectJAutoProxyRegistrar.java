package com.liuxu.springframework.beans.framework;

import com.liuxu.springframework.aop.autoproxy.AnnotationAwareAspectJAutoProxyCreator;
import com.liuxu.springframework.aop.utils.AopUtils;
import com.liuxu.springframework.beans.annotion.AnnotationMateData;
import com.liuxu.springframework.beans.annotion.EnableAspectJAutoProxy;
import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistry;
import com.liuxu.springframework.beans.interfaces.ImportBeanDefinitionRegistrar;

/**
 * 对 AspectJ 代理的后处理器进行注册
 * <p>
 * 会注册{@link AnnotationAwareAspectJAutoProxyCreator}
 *
 * @date: 2025-08-29
 * @author: liuxu
 * @see EnableAspectJAutoProxy
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMateData importAnnotationMateData, BeanDefinitionRegistry registry) {

        // 注册 AspectJAnnotationAutoProxyCreator
        AopUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

        // 获取 proxyTargetClass 属性的值
        Boolean proxyTargetClass = importAnnotationMateData.getAttributeValue("proxyTargetClass", Boolean.FALSE);
        if (proxyTargetClass) {
            // 设置优先使用CGLIB代理
            AopUtils.forceAutoProxyCreatorToUseClassProxying(registry);
        }


    }
}
