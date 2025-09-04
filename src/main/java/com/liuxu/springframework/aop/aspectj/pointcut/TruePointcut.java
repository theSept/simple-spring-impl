package com.liuxu.springframework.aop.aspectj.pointcut;

import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.matches.ClassFilter;
import com.liuxu.springframework.aop.matches.MethodMatcher;

/**
 * 表示始终匹配，所有的切入点匹配都成立
 * 始终匹配的规范切入点实例。
 *
 * @date: 2025-08-15
 * @author: liuxu
 */
public class TruePointcut implements Pointcut {

    public static final TruePointcut INSTANCE = new TruePointcut();


    @Override
    public ClassFilter getClassFilter() {
        return ClassFilter.TRUE;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return MethodMatcher.TRUE;
    }
}
