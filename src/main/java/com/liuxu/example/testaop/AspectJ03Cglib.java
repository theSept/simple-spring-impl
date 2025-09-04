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

import java.util.Arrays;

/**
 * @date: 2025-09-03
 * @author: liuxu
 */
@Component
@Aspect
public class AspectJ03Cglib {
    @Before("execution(* com.liuxu.example.testaop.BeanAop2.foo())")
    public void beforeFoo(JoinPoint joinPoint) {
        System.out.println("方法：" + joinPoint.getSignature().getName() + " beforeFoo..");
    }

    @After("execution(* com.liuxu.example.testaop.BeanAop2.foo())")
    public void afterFoo(JoinPoint joinPoint) {
        System.out.println("方法：" + joinPoint.getSignature().getName() + " afterFoo..");
    }

    @AfterReturning("execution(* com.liuxu.example.testaop.BeanAop2.foo())")
    public void afterReturningFoo(JoinPoint joinPoint) {
        System.out.println("方法：" + joinPoint.getSignature().getName() + " afterReturningFoo..");
    }

    @AfterThrowing(value = "execution(* com.liuxu.example.testaop.BeanAop2.foo())", throwing = "ex", argNames = "ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) { // JoinPoint类型的参数在 argNames属性可以不填写，在参数列表的第一个就行。
        System.out.println("方法：" + joinPoint.getSignature().getName() + " 抛出异常：" + ex.getMessage());
    }

    @Around("execution(* com.liuxu.example.testaop.BeanAop2.foo())")
    public Object aroundFoo(ProceedingJoinPoint pjp) {
        System.out.println("方法：" + pjp.getSignature().getName() + " aroundFoo..");
        Object proceed = null;
        try {
            proceed = pjp.proceed(pjp.getArgs());
            System.out.println("方法：" + pjp.getSignature().getName() + " aroundFoo.. 返回之后执行");
        } catch (Throwable e) {
            System.out.println("方法：" + pjp.getSignature().getName() + " 异常拦截..");
            throw new RuntimeException(e);
        }
        return proceed;
    }


    // =========================== foo2()
    @Before(value = "execution(* com.liuxu.example.testaop.BeanAop2.foo2(..)) && args(number,str)", argNames = "jp,number,str")
    public void beforeFoo2(JoinPoint jp, int number, String str) {
        System.out.println("方法：" + jp.getSignature().getName() + "(" + number + "," + str + ") before.. ：");
    }

    @AfterReturning(value = "execution(* com.liuxu.example.testaop.BeanAop2.foo2(..)) && args(..,paramStr)", returning = "restVal", argNames = "jp,paramStr,restVal")
    public void afterReturningFoo2(JoinPoint jp, String paramStr, String restVal) {
        System.out.println("方法：" + jp.getSignature().getName() + "() 第二个参数：" + paramStr + " 返回值：" + restVal);
    }

    @After(value = "execution(* com.liuxu.example.testaop.BeanAop2.foo2(..)) && args(..,str)", argNames = "jp,str")
    public void afterFoo2(JoinPoint jp, String str) {
        System.out.println("方法：" + jp.getSignature().getName() + "() after.. ：" + str);
    }

    @Around(value = "execution(* com.liuxu.example.testaop.BeanAop2.foo2(..)) && args(i,str)", argNames = "i,str")
    public Object aroundFoo2(ProceedingJoinPoint pjp, int i, String str) { // ProceedingJoinPoint参数可以不用卸载 argNames ，只要在第一个参数会自动解析
        System.out.println("方法：" + pjp.getSignature().getName() + "() aroundFoo2.. ：" + i + " " + str);
        System.out.println("方法：" + pjp.getSignature().getName() + "() aroundFoo2.. ：args: " + Arrays.toString(pjp.getArgs()));
        Object result;
        try {
            result = pjp.proceed(pjp.getArgs());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("方法：" + pjp.getSignature().getName() + "() aroundFoo2.. ：finally..");
        }
        return result;
    }


    // ==================fooEx()
    @AfterThrowing(value = "execution(* com.liuxu.example.testaop.BeanAop2.fooEx())", throwing = "ex", argNames = "ex")
    public void beforeFooEx(JoinPoint joinPoint, Exception ex) {
        System.out.println("方法：" + joinPoint.getSignature().getName() + "() beforeFooEx.. 错误信息：" + ex.getMessage());
    }


}
