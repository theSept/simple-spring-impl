package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.aspectj.advice.AbstractAspectJAdvice;
import com.liuxu.springframework.aop.aspectj.advice.AspectJAfterAdvice;
import com.liuxu.springframework.aop.aspectj.advice.AspectJAfterReturningAdvice;
import com.liuxu.springframework.aop.aspectj.advice.AspectJAfterThrowingAdvice;
import com.liuxu.springframework.aop.aspectj.advice.AspectJAroundAdvice;
import com.liuxu.springframework.aop.aspectj.advice.AspectJMethodBeforeAdvice;
import com.liuxu.springframework.aop.aspectj.instance.MetadataAwareAspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.utils.AnnotationUtils;
import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Spring AOP 中处理基于 @Aspect 注解的切面的重要类。它的作用是：
 * 1.从 @Aspect 注解类中提取通知和切点信息。
 * 2.创建相应的 Advisor，这些 Advisor 包含了通知和切点的具体实现。
 * 3.将这些 Advisor 应用于 Spring AOP 代理中，从而实现对目标对象方法的拦截和增强。
 *
 * @date: 2025-08-14
 * @author: liuxu
 */
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory {

    // 日志
    private static final Logger log = LoggerFactory.getLogger(ReflectiveAspectJAdvisorFactory.class);

    private final BeanFactory beanFactory;

    public ReflectiveAspectJAdvisorFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    // AspectJ 切面方法排序
    private static final Comparator<Method> methodSort = (o1, o2) -> {
        /* 拿到方法上AspectJ注解，按注解顺序排序，*/
        AspectJAnnotation aspectJAnnotation1 = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(o1);
        AspectJAnnotation aspectJAnnotation2 = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(o2);

        Integer order1 = Optional.ofNullable(aspectJAnnotation1)
                .map(AspectJAnnotation::getAnnotationType)
                .map(AspectJAnnotationType::getOrder)
                .orElse(Integer.MAX_VALUE);
        Integer order2 = Optional.ofNullable(aspectJAnnotation2)
                .map(AspectJAnnotation::getAnnotationType)
                .map(AspectJAnnotationType::getOrder)
                .orElse(Integer.MAX_VALUE);

        return Integer.compare(order1, order2);

    };

    static {
        // 当注解相同时，按@Order注解排序
        methodSort.thenComparing((m1, m2) -> {
            Integer order1 = Optional.ofNullable(AnnotationUtils.findAnnotation(m1, Order.class))
                    .map(Order::value).orElse(Integer.MAX_VALUE);
            Integer order2 = Optional.of(AnnotationUtils.findAnnotation(m2, Order.class))
                    .map(Order::value).orElse(Integer.MAX_VALUE);
            return Integer.compare(order1, order2);
        });
    }


    @Override
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) throws Exception {
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();

        validate(aspectClass);

        List<Advisor> advisors = new ArrayList<>();
        for (Method method : getAdvisorMethods(aspectClass)) {
            if (method.equals(ClassUtils.getMostSpecificMethod(method, aspectClass))) {
                Advisor advisor = getAdvisor(method, aspectInstanceFactory, 0, aspectName);
                if (advisor != null) {
                    advisors.add(advisor);
                }
            }

        }
        // TODO 解析字段切面 @DeclareParents

        return advisors;
    }


    @Override
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) throws Exception {
        validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

        // 获取切点表达式
        AspectJExpressionPointcut expressionPointcut = getPointcut(candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        if (expressionPointcut == null) {
            return null;
        }

        try {
            // 创建一个方法切面实例，里面会再调用获取 Advice 方法。
            return new InstantiationModelAwarePointcutAdvisor(expressionPointcut, candidateAdviceMethod,
                    this, aspectInstanceFactory, declarationOrder, aspectName);
        } catch (Exception e) {
            // 忽略不兼容的建议方法
            log.error("[ERROR] 无法创建 AspectJExpressionPointcutAdvisor,忽略该通知方法：[{}], 错误信息{}", candidateAdviceMethod, e.getMessage());
            return null;
        }
    }


    /**
     * 获取表达式切入点
     */
    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        AspectJAnnotation annotationOnMethod = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (annotationOnMethod == null) {
            return null;
        }

        // Aspect切点表达式匹配
        AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class[0]);
        ajexp.setExpression(annotationOnMethod.getPointcutExpression());
        if (this.beanFactory != null) {
            ajexp.setBeanFactory(this.beanFactory);
        }

        return ajexp;
    }


    @Override
    public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
                            MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) throws Exception {
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        validate(aspectClass);

        AspectJAnnotation aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }

        if (!isAspect(aspectClass)) {
            // 不是切面类
            throw new IllegalStateException("通知必须在切面类型中声明：违规的方法：" + candidateAdviceMethod + "在 类[" + candidateAdviceMethod.getDeclaringClass() + "]中");
        }

        AbstractAspectJAdvice advice;
        switch (aspectJAnnotation.getAnnotationType()) {
            case AtPointcut -> {
                return new AspectJAroundAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            }
            case AtBefore -> advice = new AspectJMethodBeforeAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            case AtAfterReturning -> {
                advice = new AspectJAfterReturningAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterReturning annotation = (AfterReturning) aspectJAnnotation.getAnnotation();
                if (!StringUtils.isBlank(annotation.returning())) {
                    advice.setReturningName(annotation.returning());
                }
            }
            case AtAfter -> advice = new AspectJAfterAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            case AtAfterThrowing -> {
                advice = new AspectJAfterThrowingAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterThrowing annotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
                if (!StringUtils.isBlank(annotation.throwing())) {
                    advice.setThrowingName(annotation.throwing());
                }
            }
            case AtAround -> advice = new AspectJAroundAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);

            default -> throw new IllegalStateException("不支持的切面注解：" + aspectJAnnotation.getAnnotationType());
        }

        advice.setAspectName(aspectName);
        advice.setDeclarationOrder(declarationOrder);
        // 解析注解配置的参数
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
        if (parameterNames != null) {
            // 设置通知方法参数名称
            advice.setArgumentNamesFromStringArray(parameterNames);
        }
        // 计算并绑定参数
        advice.calculateArgumentBindings();

        return advice;
    }


    /**
     * 获取指定类中所有的切面方法
     *
     * @param aspectClass 切面类
     * @return 切面方法列表
     */
    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        List<Method> methods = new ArrayList<>();
        ReflectionUtils.doWithLocalMethods(aspectClass, method -> {
            if (method.isBridge() || method.isSynthetic() || (method.getDeclaringClass() == Object.class)) {
                return;
            }

            if (method.getAnnotation(Pointcut.class) != null) {
                return;
            }
            methods.add(method);
        });

        if (methods.size() > 1) {
            methods.sort(methodSort);
        }

        return methods;
    }


}
