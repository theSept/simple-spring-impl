package com.liuxu.springframework.aop.aspectj;

/**
 * 切面优先级信息接口
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public interface AspectJPrecedenceInformation {

    /**
     * Return the name of the aspect (bean) in which the advice was declared.
     */
    String getAspectName();

    /**
     * Return the declaration order of the advice member within the aspect.
     */
    int getDeclarationOrder();

    /**
     * Return whether this is a before advice.
     */
    boolean isBeforeAdvice();

    /**
     * Return whether this is an after advice.
     */
    boolean isAfterAdvice();

}
