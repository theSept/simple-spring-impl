package com.liuxu.springframework.aop.aspectj;

import com.liuxu.springframework.aop.ProxyMethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 通知方法调用时传递的切点匹配类，包装一层方法调用器
 *
 * @date: 2025-08-18
 * @author: liuxu
 */
public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart, Cloneable {

    private final ProxyMethodInvocation methodInvocation;


    public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }

    /** 目标方法参数 */
    private Object[] args;

    /** 方法签名 */
    private Signature signature;


    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {
        throw new UnsupportedOperationException();
    }

    /**
     * 继续执行拦截链，clone后继续执行
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object proceed() throws Throwable {
        // 允许多次消费，需克隆执行，避免破坏原始拦截链索引，保证链路完整性
        return this.methodInvocation.invocableClone().proceed();
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
        return this.methodInvocation.invocableClone(args).proceed();
    }

    @Override
    public String toShortString() {
        return "";
    }

    @Override
    public String toLongString() {
        return "";
    }

    /**
     * 返回 Spring AOP 代理对象。不能是 null
     *
     * @return
     */
    @Override
    public Object getThis() {
        return this.methodInvocation.getProxy();
    }

    /**
     * 返回 Spring AOP 目标。如果没有目标，则可能为 {@code null}。
     *
     * @return
     */
    @Override
    public Object getTarget() {
        return this.methodInvocation.getThis();
    }

    /**
     * 方法的参数
     */
    @Override
    public Object[] getArgs() {
        if (this.args == null) {
            this.args = this.methodInvocation.getArguments().clone();
        }
        return this.args;
    }

    /**
     * 方法的签名，包装外部类 methodInvocation.getMethod() 的方法信息。
     */
    @Override
    public Signature getSignature() {
        if (this.signature == null) {
            this.signature = new MethodSignatureImpl();
        }
        return this.signature;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public String getKind() {
        return JoinPoint.METHOD_EXECUTION;
    }

    @Override
    public JoinPoint.StaticPart getStaticPart() {
        return this;
    }

    /**
     * 方法的签名，包装外部类 methodInvocation.getMethod() 的方法信息。
     */
    private class MethodSignatureImpl implements MethodSignature {

        private String[] parameterNames;

        @Override
        public String getName() {
            return methodInvocation.getMethod().getName();
        }

        @Override
        public int getModifiers() {
            return methodInvocation.getMethod().getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return methodInvocation.getMethod().getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return methodInvocation.getMethod().getDeclaringClass().getName();
        }

        @Override
        public Class<?> getReturnType() {
            return methodInvocation.getMethod().getReturnType();
        }

        @Override
        public Method getMethod() {
            return methodInvocation.getMethod();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return methodInvocation.getMethod().getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            String[] names = this.parameterNames;
            if (names == null) {
                names = getParameterNames(getMethod().getParameters());
                this.parameterNames = names;
            }
            return names;
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return methodInvocation.getMethod().getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return "";
        }

        @Override
        public String toLongString() {
            return "";
        }


        /**
         * 解析参数名称
         * Spring中使用的是: org.springframework.core.DefaultParameterNameDiscoverer
         *
         * @param parameters 参数列表
         * @return 参数名称列表
         */
        private String[] getParameterNames(Parameter[] parameters) {
            String[] parameterNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (!param.isNamePresent()) {
                    return null;
                }
                parameterNames[i] = param.getName();
            }
            return parameterNames;
        }
    }

}
