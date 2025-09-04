package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.core.ParameterNameDiscoverer;
import com.liuxu.springframework.utils.AnnotationUtils;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @date: 2025-08-14
 * @author: liuxu
 */
public abstract class AbstractAspectJAdvisorFactory implements AspectJAdvisorFactory {

    // Aspectj 注解的类型
    private static final Class<?>[] ASPECTJ_ANNOTATION_CLASSES = {
            Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class
    };

    // 用来发现注解属性显示绑定方法的参数名称。 私有类实现接口，暴露的接口的抽象方法，供子类调用，实际的内部实现封装在私有的内部类。
    protected final ParameterNameDiscoverer parameterNameDiscoverer = new AspectJAnnotationParameterNameDiscoverer();

    @Override
    public boolean isAspect(Class<?> clazz) {
        return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null);
    }

    @Override
    public void validate(Class<?> aspectClass) throws Exception {
        // AjType 是 AspectJ 中用于表示切面（Aspect）和相关元数据的类型封装
        AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
        if (!ajType.isAspect()) {
            throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not an AspectJ aspect");
        }

        // percflow 和 percflowbelow 是一种切面实例化模型。目前Spring AOP中不支持。
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
            throw new IllegalArgumentException(aspectClass.getName() + " uses percflow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
            throw new IllegalArgumentException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
        // ....

    }

    /**
     * 查找 AspectJ 注解
     * 在给定方法上查找并返回第一个 AspectJ 注释（无论如何 应该 只有一个......
     */

    protected static AspectJAnnotation findAspectJAnnotationOnMethod(Method method) {
        for (Class<?> annotationClass : ASPECTJ_ANNOTATION_CLASSES) {
            Annotation annotation = AnnotationUtils.findAnnotation(method, (Class<Annotation>) annotationClass);
            if (annotation != null) {
                return new AspectJAnnotation(annotation);
            }
        }
        return null;
    }

    /**
     * AspectJ 注释类型的枚举.
     *
     * @see AspectJAnnotation#getAnnotationType()
     */
    protected enum AspectJAnnotationType {

        AtPointcut(1), AtAround(2), AtBefore(3), AtAfter(4), AtAfterReturning(5), AtAfterThrowing(6);

        AspectJAnnotationType(int order) {
            this.order = order;
        }

        private final int order;

        public int getOrder() {
            return order;
        }
    }

    /**
     * 对 AspectJ 注解进行类建模，公开其类型枚举和切入点 String。
     */
    protected static class AspectJAnnotation {

        // 切入点注解的属性
        private static final String[] EXPRESSION_ATTRIBUTES = {"pointcut", "value"};

        // 注解类型映射的枚举表
        private static final Map<Class<?>, AspectJAnnotationType> annotationTypeMap = Map.of(
                Pointcut.class, AspectJAnnotationType.AtPointcut, //
                Around.class, AspectJAnnotationType.AtAround, //
                Before.class, AspectJAnnotationType.AtBefore, //
                After.class, AspectJAnnotationType.AtAfter, //
                AfterReturning.class, AspectJAnnotationType.AtAfterReturning, //
                AfterThrowing.class, AspectJAnnotationType.AtAfterThrowing //
        );
        // AspectJ 切面类中方法（（如 @Before、@After、@Around 等）和切点（Pointcut）信息））上的注解
        private final Annotation annotation;

        // 注解类型枚举
        private final AspectJAnnotationType annotationType;

        // 切点表达式
        private final String pointcutExpression;

        // 指定的参数名称
        private final String argumentNames;

        public AspectJAnnotation(Annotation annotation) {
            this.annotation = annotation;
            this.annotationType = determineAnnotationType(annotation);
            try {
                this.pointcutExpression = resolvePointcutExpression(annotation);
                Object argNames = AnnotationUtils.getValue(annotation, "argNames");
                this.argumentNames = (argNames instanceof String name ? name : "");
            } catch (Exception ex) {
                throw new IllegalArgumentException("无法解析注释: " + annotation, ex);
            }
        }

        // 确定注释类型
        private AspectJAnnotationType determineAnnotationType(Annotation annotation) {
            AspectJAnnotationType type = annotationTypeMap.get(annotation.annotationType());
            if (type != null) {
                return type;
            }
            throw new IllegalArgumentException("不支持的注释类型: " + annotation.annotationType());
        }

        // 解析切点表达式
        private String resolvePointcutExpression(Annotation annotation) {
            for (String attribute : EXPRESSION_ATTRIBUTES) {
                Object value = AnnotationUtils.getValue(annotation, attribute);
                // 注意 @AfterReturning 和 @AfterThrowing 注解有 pointcut 属性默认是空字符串
                if (value instanceof String svl && !svl.isEmpty()) {
                    return svl;
                }
            }
            return null;
        }


        public String getPointcutExpression() {
            return pointcutExpression;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        public AspectJAnnotationType getAnnotationType() {
            return annotationType;
        }

        public String getArgumentNames() {
            return argumentNames;
        }

        @Override
        public String toString() {
            return this.annotation.toString();
        }
    }


    /**
     * Aspect注解中的参数名称发现者
     * Spring中是私有类，实现接口，秉承面向对象的封装思想，
     */
    private static class AspectJAnnotationParameterNameDiscoverer implements ParameterNameDiscoverer {

        private final String[] EMPTY_ARRAY = new String[0];

        /**
         * 获取通知注解中显示配置的参数名称数组
         *
         * @param method 通知方法
         * @return 参数名称数组
         */
        @Override
        public String[] getParameterNames(Method method) {
            if (method.getParameterCount() == 0) {
                return EMPTY_ARRAY;
            }

            AspectJAnnotation aspectJAnnotation = findAspectJAnnotationOnMethod(method);
            if (aspectJAnnotation == null) {
                return null;
            }

            StringTokenizer stringTokenizer = new StringTokenizer(aspectJAnnotation.getArgumentNames(), ",");
            int countTokens = stringTokenizer.countTokens();
            if (countTokens > 0) {
                String[] parameterNames = new String[countTokens];
                for (int i = 0; i < parameterNames.length; i++) {
                    parameterNames[i] = stringTokenizer.nextToken();
                }
                return parameterNames;
            } else {
                return null;
            }

        }

        @Override
        public String[] getParameterNames(Constructor<?> ctor) {
            throw new UnsupportedOperationException("不支持构造函数参数名称的获取");
        }
    }

}
