package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.ProxyMethodInvocation;
import com.liuxu.springframework.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反射方法调用器
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class ReflectiveMethodInvocation implements MethodInvocation, ProxyMethodInvocation, Cloneable {


    // 目标方法
    private Method method;
    // 方法参数
    private Object[] arguments;
    // 目标对象
    private Object target;
    // 代理对象
    private Object proxy;
    // 目标对象类型
    private Class<?> targetClass;

    /**
     * 用户属性 调用时延迟加载
     */
    private Map<String, Object> userAttributes;

    /** 拦截链(拦截器和方法匹配拦截器组合) */
    private final List<?> interceptorsAndDynamicMethodMatchers;

    /** 当前拦截器索引 */
    private int currentInterceptorIndex = -1;


    public ReflectiveMethodInvocation(Method method, Object[] arguments, Object target, Object proxy, Class<?> targetClass, List<?> interceptorsAndDynamicMethodMatchers) {
        this.method = method;
        this.arguments = (arguments != null ? arguments : new Object[0]);
        this.target = target;
        this.proxy = proxy;
        this.targetClass = targetClass;
        this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
    }


    @Override
    public Object proceed() throws Throwable {
        /* 进行链式递归调用 */

        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            // 拦截连执行完毕,调用目标方法
            return invokeJoinpointUsingReflection(this.method, this.target, this.arguments);
        }


        Object interceptor = this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
        if (interceptor instanceof InterceptorAndDynamicMethodMatcher iadmm) {
            // 动态匹配通知
            // 会将参数存入 JoinPointMatch 里,供通知方法参数绑定
            Class<?> targetClass = (this.targetClass != null ? this.targetClass : method.getDeclaringClass());
            if (iadmm.methodMatcher().matches(this.method, targetClass, this.arguments)) {
                return iadmm.methodInterceptor().invoke(this);
            } else {
                // 如果动态匹配不上该方法,则再次进入调用下一个拦截方法
                return proceed();
            }
        } else {
            // 不需要动态匹配,静态匹配通过的,直接拦截器调用
            // 拦截器直接调用
            return ((MethodInterceptor) interceptor).invoke(this);
        }

    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public Object[] getArguments() {
        return this.arguments;
    }

    @Override
    public Object getThis() {
        return this.target;
    }

    @Override
    public Object getProxy() {
        return this.proxy;
    }


    @Override
    public MethodInvocation invocableClone() {
        Object[] arguments = this.arguments.clone();
        if (this.arguments.length > 0) {
            // 对于参数，构建参数数组的独立副本，浅克隆无法对引用类型创建独立副本
            arguments = this.arguments.clone();
        }
        return invocableClone(arguments);
    }

    /**
     * 此实现返回此调用对象的浅层副本，使用克隆的给定参数数组。
     * 在这种情况下，我们想要一个浅拷贝：我们想要使用相同的拦截器链和其他对象引用，但我们想要当前拦截器索引的独立值
     *
     * @param arguments 克隆调用应该使用的参数，覆盖原始参数
     * @return
     */
    @Override
    public MethodInvocation invocableClone(Object... arguments) {
        if (this.userAttributes == null) {
            this.userAttributes = new HashMap<>();
        }
        try {
            // 浅拷贝，基本值类型会创建独立副本，但引用类型的属性引用地址还是不变的。
            ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
            clone.setArguments(arguments);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("CloneNotSupportedException", e);
        }
    }

    @Override
    public void setUserAttribute(String key, Object value) {
        if (value != null) {
            if (this.userAttributes == null) {
                this.userAttributes = new HashMap<>();
            }
            this.userAttributes.put(key, value);
        } else {
            if (this.userAttributes != null) {
                this.userAttributes.remove(key);
            }
        }
    }

    @Override
    public Object getUserAttribute(String key) {
        if (this.userAttributes == null) {
            return null;
        }
        return this.userAttributes.get(key);
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }


    /**
     * 使用反射调用连接点方法(目标方法)
     *
     * @param method 方法
     * @param target 目标对象
     * @param args   方法参数
     * @return 方法返回值
     * @throws Throwable 调用连接点时出现的异常
     */
    private Object invokeJoinpointUsingReflection(Method method, Object target, Object... args) throws Throwable {
        try {
            ReflectionUtils.makeAccessible(method);
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            // 反射调用目标方法引发了检查异常,需要抛出目标方法真实的异常,否则异常拦截器无法正常拦截
            throw ex.getTargetException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("调用目标方法出现非法的参数异常:", ex);
        }
    }

}
