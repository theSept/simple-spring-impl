package com.liuxu.springframework.aop;

import com.liuxu.springframework.aop.aspectj.pointcut.TruePointcut;
import com.liuxu.springframework.aop.matches.ClassFilter;
import com.liuxu.springframework.aop.matches.MethodMatcher;

/**
 * 切点 顶级接口，包含在切面中
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface Pointcut {

    /**
     * 返回此切点的类过滤器
     */
    ClassFilter getClassFilter();

    /**
     * 返回此切点的方法匹配器
     */
    MethodMatcher getMethodMatcher();


    // 表示：所有切点都匹配
    Pointcut TRUE = TruePointcut.INSTANCE;

}
