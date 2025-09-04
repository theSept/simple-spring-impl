package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.annotion.Component;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * 动态匹配
 *
 * @date: 2025-09-02
 * @author: liuxu
 */
@Aspect
@Component
public class AspectJ02 {

    // 最稳妥的方式,就是表达式 args() 写了参数,注解的 argNames 需要写参数名称,方法上也要保持一样的参数列表
    @Before(value = "execution(* com.liuxu.example.testaop.BeanAop1.foo2Sum(..)) and args(a,..)", argNames = "a")
    public void foo2(JoinPoint pjp, int a) {
        System.out.println("before foo2(): 目标方法:" + pjp.getSignature().getName() + " i1 = " + a + ",i2=" + "X");
    }


}
