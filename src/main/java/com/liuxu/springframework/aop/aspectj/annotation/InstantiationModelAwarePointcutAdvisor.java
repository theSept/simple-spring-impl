package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.PointcutAdvisor;
import com.liuxu.springframework.aop.aspectj.instance.MetadataAwareAspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;

import java.lang.reflect.Method;

/**
 * 专门为 @Aspect 注解切面生成
 * 切面实例化模型感知
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class InstantiationModelAwarePointcutAdvisor implements PointcutAdvisor {

    private static final Advice EMPTY_ADVICE = Advisor.EMPTY_ADVICE;

    /** 声明的切点表达式切点 */
    private final AspectJExpressionPointcut declaredPointcut;

    /** AspectJ 通知方法的声明的类 */
    private final Class<?> declaringClass;

    /** AspectJ 通知方法名称 */
    private final String methodName;

    /** AspectJ 通知方法的参数类型 */
    private final Class<?>[] parameterTypes;

    /** AspectJ 通知方法 */
    private transient Method aspectJAdviceMethod;

    /** 解析获取 AspectJ 切面的工厂 */
    private final AspectJAdvisorFactory aspectJAdvisorFactory;

    /** AspectJ 切面的是实例工厂 */
    private final MetadataAwareAspectInstanceFactory aspectInstanceFactory;

    private final int declarationOrder;

    /** AspectJ 类在容器中的 beanName */
    private final String aspectName;

    /** 切点 */
    private final Pointcut pointcut;

    /** 通知 */
    private Advice instantiatedAdvice;

    public InstantiationModelAwarePointcutAdvisor(AspectJExpressionPointcut declaredPointcut,
                                                  Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
                                                  MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
        this.declaredPointcut = declaredPointcut;
        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
        this.methodName = aspectJAdviceMethod.getName();
        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
        this.aspectInstanceFactory = aspectInstanceFactory;
        this.declarationOrder = declarationOrder;
        this.aspectName = aspectName;
        this.aspectJAdviceMethod = aspectJAdviceMethod;
        this.pointcut = this.declaredPointcut;
        this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
    }


    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }


    @Override
    public Advice getAdvice() {
        if (this.instantiatedAdvice == null) {
            try {
                this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this.instantiatedAdvice;
    }


    // 实例化一个通知
    private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
        Advice advice = null;
        try {
            advice = this.aspectJAdvisorFactory.getAdvice(
                    this.aspectJAdviceMethod, pointcut, this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (advice == null ? EMPTY_ADVICE : advice);
    }

}
