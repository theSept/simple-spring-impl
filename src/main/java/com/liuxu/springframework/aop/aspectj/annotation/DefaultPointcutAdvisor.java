package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.PointcutAdvisor;

/**
 * 默认切面
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class DefaultPointcutAdvisor implements PointcutAdvisor {

    // 切点
    private Pointcut pointcut = Pointcut.TRUE;

    // 通知
    private Advice advice = EMPTY_ADVICE;

    public DefaultPointcutAdvisor() {
    }

    public DefaultPointcutAdvisor(Advice advice) {
        this(Pointcut.TRUE, advice);
    }


    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        setAdvice(advice);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    /**
     * Specify the advice that this advisor should apply.
     */
    public void setAdvice(Advice advice) {
        this.advice = advice;
    }


}
