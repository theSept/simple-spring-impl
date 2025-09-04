package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.annotion.Component;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * @date: 2025-08-28
 * @author: liuxu
 */
@Aspect
@Component
public class AspectJ01 {


    @Before("execution(* com.liuxu.example.testaop.BeanAop1.foo())")
    public void beforeFoo(JoinPoint joinPoint) {
        System.out.println("before 增强...");
    }

    @AfterReturning(value = "execution(* com.liuxu.example.testaop.BeanAop1.foo())")
    public void afterReturningFoo() {
        System.out.println("afterReturning afterReturningFoo 增强...");
    }

    @After("execution(* com.liuxu.example.testaop.BeanAop1.foo())")
    public void afterFoo() {
        System.out.println("after 最终增强...");
    }


    @AfterThrowing(value = "execution(* com.liuxu.example.testaop.BeanAop1.foo())", throwing = "exception", argNames = "exception")
    public void afterThrowingFoo(ArithmeticException exception) {
        System.out.println("异常通知 异常:" + exception.getMessage());
    }

    @Around("execution(* com.liuxu.example.testaop.BeanAop1.foo())")
    public Object around(ProceedingJoinPoint pjp) {
        System.out.println("=====开始环绕增强");

        Object result = null;
        try {
            System.out.println("环绕通知 -before");
            result = pjp.proceed(pjp.getArgs());
            System.out.println("环绕通知 -afterReturning  result=" + result);
        } catch (Throwable e) {
            System.out.println("环绕通知 -afterThrowing  e=" + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            System.out.println("环绕通知 -after");
        }
        System.out.println("=====结束环绕增强");
        return result;
    }


}
