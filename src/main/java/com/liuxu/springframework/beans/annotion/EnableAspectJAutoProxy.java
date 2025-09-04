package com.liuxu.springframework.beans.annotion;

import com.liuxu.springframework.beans.framework.AspectJAutoProxyRegistrar;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启AOP切面,
 * 会将{@link AspectJAutoProxyRegistrar} 实例化并调用其注册方法,将AOP切面的后处理器注册容器中
 * <p>
 * 开启Cglib代理后需要添加JVM参数：--add-opens java.base/java.lang=ALL-UNNAMED
 *
 * @date: 2025-08-29
 * @author: liuxu
 * @see org.aspectj.lang.annotation.Aspect
 * @see com.liuxu.springframework.aop.autoproxy.AnnotationAwareAspectJAutoProxyCreator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

    /**
     * 是否使用CGLIB代理
     */
    boolean proxyTargetClass() default false;


}
