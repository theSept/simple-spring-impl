package com.liuxu.springframework.aop.aspectj.advice;


import com.liuxu.springframework.aop.AfterReturningAdvice;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import com.liuxu.springframework.utils.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 方法返回后通知
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice implements AfterReturningAdvice {

    public AspectJAfterReturningAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }


    @Override
    public boolean isBeforeAdvice() {
        return false;
    }

    @Override
    public boolean isAfterAdvice() {
        return true;
    }

    @Override
    public void setReturningName(String returningName) {
        super.setReturningNameNoCheck(returningName);
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        // 如果有返回值，必须返回的结果和方法的返回类型匹配才允许调用方法
        if (shouldInvokeOnReturnValueOf(method, returnValue)) {
            invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
        }
    }


    /**
     * 在 AspectJ 语义之后，如果指定了返回子句，则仅当返回值是给定返回类型的实例并且泛型类型参数（如果有）与赋值规则匹配时，才会调用通知。
     * 如果返回类型是 Object，则*始终*调用通知.
     * 检查返回值的类型跟方法的返回类型是否匹配，匹配才会调用返回通知
     */
    private boolean shouldInvokeOnReturnValueOf(Method method, Object returnValue) {
        /*
            例：
            @AfterReturning(pointcut = "...", returning = "ret")
            public void afterReturning(List<String> ret) { ... }

            type  = List.class
            genericType = List<String>  // 参数的type包含反省
         */
        Class<?> type = getDiscoveredReturningType();
        Type genericType = getDiscoveredReturningGenericType(); // genericType检查：如果有泛型声明，再严格验证泛型一致性

        // 忽略泛型判断
        return matchesReturnValue(type, method, returnValue);
    }

    /**
     * 检查 after-returning 通知上声明的参数类型（type），和目标方法的返回值（真实值或签名类型）是否匹配。匹配才能调用通知
     * 如果通知方法声明的参数类是Object,并且目标方法声明为void,则仍允许调用通知
     *
     * @param type
     * @param method
     * @param returnValue
     * @return
     */
    private boolean matchesReturnValue(Class<?> type, Method method, Object returnValue) {
        if (returnValue != null) {
            // 返回了非空的值
            return ClassUtils.isAssignable(type, returnValue.getClass());
        } else if (type == Object.class && void.class == method.getReturnType()) {
            // 如果通知配置参数类型是 Object，而方法本身返回 void，直接判定不匹配
            return true;
        } else {
            // 方法实际返回null，通知配置的返回类型不是object 或方法语法写的类型不是 void。
            return ClassUtils.isAssignable(type, method.getReturnType());
        }

    }


}
