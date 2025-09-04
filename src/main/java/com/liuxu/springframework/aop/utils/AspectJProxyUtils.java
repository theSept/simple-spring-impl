package com.liuxu.springframework.aop.utils;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.PointcutAdvisor;
import com.liuxu.springframework.aop.aspectj.advice.AbstractAspectJAdvice;
import com.liuxu.springframework.aop.aspectj.annotation.InstantiationModelAwarePointcutAdvisor;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import com.liuxu.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * AspectJ 代理工具类
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public abstract class AspectJProxyUtils {


    /**
     * 如果有必要，将添加 {@link ExposeInvocationInterceptor} 在切面列表头，因为某些切入点匹配需要 供当前的AspectJ JoinPoint。
     * 如果切面链中没有 AspectJ 切面，则调用将无效。
     *
     * @param advisors 切面链
     * @return true:如果将 ExposeInvocationInterceptor 添加到列表中，否则 false
     */
    public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
        if (!advisors.isEmpty()) {
            // 标记是否存在AspectJ的方法切面
            boolean foundAspectJAdvice = false;
            for (Advisor advisor : advisors) {
                if (isAspectJAdvice(advisor)) {
                    foundAspectJAdvice = true;
                    break;
                }
            }

            // 如果存在AspectJ切面，则在最前面添加一个暴露线程上下文的切面拦截
            if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
                advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否是 AspectJ 通知
     *
     * @param advisor
     * @return
     */
    private static boolean isAspectJAdvice(Advisor advisor) {
        return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
                advisor.getAdvice() instanceof AbstractAspectJAdvice ||
                (advisor instanceof PointcutAdvisor pointcutAdvisor &&
                        pointcutAdvisor.getPointcut() instanceof AspectJExpressionPointcut));
    }


    /**
     * 验证指定标识符是否符合Java变量规范
     *
     * @param name 标识符
     * @return true:符合Java变量规范，否则 false
     */
    public static boolean isVariableName(String name) {
        if(StringUtils.isBlank(name)){
            return false;
        }

        // 检查第一个字符是否允许作为Java标识符中的第一个字符
        if(!Character.isJavaIdentifierStart(name.charAt(0))){
            return false;
        }

        // 指定的字符是否可以是第一个字符以外的 Java 标识符的一部分
        for (char c : name.toCharArray()) {
            if(!Character.isJavaIdentifierPart(c)){
                return false;
            }
        }

        return true;
    }

}
