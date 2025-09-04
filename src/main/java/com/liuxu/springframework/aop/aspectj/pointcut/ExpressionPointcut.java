package com.liuxu.springframework.aop.aspectj.pointcut;

import com.liuxu.springframework.aop.Pointcut;

/**
 * 切点表达式接口
 *
 * @date: 2025-08-15
 * @author: liuxu
 */
public interface ExpressionPointcut extends Pointcut {
    /**
     * 获取切入点的表达式
     */
    String getExpression();
}
