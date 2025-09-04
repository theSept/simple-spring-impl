package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.aspectj.instance.MetadataAwareAspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 这个类的核心任务是通过反射扫描和分析 @Aspect 注解的类，
 * 将切面类中的通知（如 @Before、@After、@Around 等）和切点（Pointcut）信息提取出来，
 * 并将它们封装为 Spring AOP 中使用的 Advisor 对象。
 * 然后，Spring 使用这些 Advisor 创建代理对象，以便为目标方法应用切面逻辑。
 *
 * @date: 2025-08-14
 * @author: liuxu
 */
public interface AspectJAdvisorFactory {

    /**
     * 判断给定的类是否是一个切面类
     *
     * @param clazz 需要判断的切面类
     * @return true 表示是切面类，false 表示不是切面类
     */
    boolean isAspect(Class<?> clazz);


    void validate(Class<?> aspectClass) throws Exception;

    /**
     * 根据Aspect切面类元数据实例工厂解析所有的 切面方法
     *
     * @param aspectInstanceFactory 切面类元数据实例工厂
     * @return Advisor切面方法
     */
    List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) throws Exception;

    /**
     * 为给定的 AspectJ 建议方法构建一个 Spring AOP Advisor。
     *
     * @param candidateAdviceMethod 候选的通知方法
     * @param aspectInstanceFactory AspectJ切面类实例工厂
     * @param declarationOrder      切面类内的声明顺序
     * @param aspectName            AspectJ切面类在Spring中的名称
     * @return null 如果该方法不是 AspectJ 通知方法，或者它是一个切入点，将被其他通知使用，但本身不会创建 Spring 通知
     */
    Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
                       int declarationOrder, String aspectName) throws Exception;

    /**
     * 创建一个通知
     *
     * @param candidateAdviceMethod 候选的通知方法
     * @param expressionPointcut    切点
     * @param aspectInstanceFactory 切面类实例工厂
     * @param declarationOrder      切面类内的声明顺序
     * @param aspectName            切面类名称
     * @return 通知
     */
    Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
                     MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) throws Exception;


}
