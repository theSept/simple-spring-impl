package com.liuxu.springframework.aop.utils;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.PointcutAdvisor;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import com.liuxu.springframework.aop.autoproxy.AnnotationAwareAspectJAutoProxyCreator;
import com.liuxu.springframework.aop.framework.AdvisedSupport;
import com.liuxu.springframework.aop.matches.MethodMatcher;
import com.liuxu.springframework.beans.beandefinition.PropertyValue;
import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;
import com.liuxu.springframework.beans.interfaces.BeanDefinition;
import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistry;
import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @date: 2025-08-17
 * @author: liuxu
 */
public abstract class AopUtils {

    /**
     * 作为AOP代理bean的BaneName
     */
    public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
            "org.springframework.aop.config.internalAutoProxyCreator";


    /**
     * 查找候选的切面中，匹配指定 class 的切面（Advisor）
     *
     * @param candidateAdvisors 候选的切面
     * @param clazz             指定class
     * @return 匹配的切面
     */
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }

        List<Advisor> eligibleAdvisors = new ArrayList<>();

        for (Advisor advisor : candidateAdvisors) {
            if (canApply(advisor, clazz)) {
                eligibleAdvisors.add(advisor);
            }
        }

        return eligibleAdvisors;
    }

    /**
     * 判断 advisor是否适用于 clazz
     *
     * @param advisor     Advisor
     * @param targetClass Class
     * @return boolean
     */
    public static boolean canApply(Advisor advisor, Class<?> targetClass) {
        if (advisor instanceof PointcutAdvisor pa) { // 切点的切面
            return canApply(pa.getPointcut(), targetClass);
        } else { // TODO Spring中还有接口增强，暂时忽略不实现
            return false;
        }
    }

    /**
     * 判断切点是否适用指定类
     * <p>
     * 会将目标类实现的接口也加入扫描的范围，
     * 与 {@link AspectJExpressionPointcut#getTargetShadowMatch(Method, Class)} 匹配的范围一致。
     *
     * @param pointcut
     * @param targetClass
     * @return
     * @see AspectJExpressionPointcut#getTargetShadowMatch(Method, Class)
     */
    private static boolean canApply(Pointcut pointcut, Class<?> targetClass) {
        // 首先进行类型匹配
        if (!pointcut.getClassFilter().matches(targetClass)) {
            return false;
        }

        // 开始处理方法匹配
        MethodMatcher methodMatcher = pointcut.getMethodMatcher();

        List<Class<?>> classes = new ArrayList<>();
        // 目标的实际类型，过滤掉JDK代理/CGLIB代理创建的类
        if (!Proxy.isProxyClass(targetClass)) {
            classes.add(ClassUtils.getUserClass(targetClass));
        }

        // 把目标类实现的接口以及父类实现的接口纳入匹配扫描范围
        classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

        for (Class<?> clazz : classes) {

            // 当前类及其超类（接口和超接口）上所有声明的方法
            Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method method : allDeclaredMethods) {
                // 方法的静态匹配，类中只要有个方法匹配成功，就算适用。
                if (methodMatcher.matches(method, targetClass)) {
                    return true;
                }
            }

        }

        return false;
    }


    /**
     * 使用反射调用连接点方法
     *
     * @param method 方法
     * @param target 目标对象
     * @param args   方法参数
     * @return 方法返回值
     * @throws Throwable 反射调用方法可能出现异常
     */
    public static Object invokeJoinpointUsingReflection(Method method, Object target, Object[] args) throws Throwable {
        try {
            ReflectionUtils.makeAccessible(method);
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            // 非法的访问
            throw new RuntimeException("[调用目标方法]非法的访问目标方法", e);
        } catch (InvocationTargetException e) {
            // 调用目标方法出现异常
            throw e.getTargetException();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("[调用目标方法]非法的参数", e);
        }
    }


    /**
     * 在容器中注册AnnotationAwareAspectJAutoProxyCreator
     *
     * @param registry BeanDefinitionRegistry
     * @return BeanDefinition
     */
    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
        return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
    }

    private static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object o) {
        // 将 AnnotationAwareAspectJAutoProxyCreator 注册,支持AOP代理
        return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, null);
    }


    private static BeanDefinition registerOrEscalateApcAsRequired(
            Class<?> cls, BeanDefinitionRegistry registry, Object source) {

        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            return null;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
        beanDefinition.setLazyInit(false);
        registry.registryBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);

        return beanDefinition;
    }


    /**
     * 给{@link AnnotationAwareAspectJAutoProxyCreator} 的 beanDefinition 设置优先使用CGLIB代理的配置
     *
     * @param registry BeanDefinitionRegistry
     */
    public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
            beanDefinition.getPropertyValues().add(new PropertyValue("proxyTargetClass", Boolean.TRUE));
        }
    }


    /**
     * 拿到完整的代理接口
     *
     * @param advised advisedSupport
     * @return 完整的代理接口
     */
    public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
        Class<?>[] proxiedInterfaces = advised.getProxiedInterfaces();
        if (proxiedInterfaces.length == 0) {
            Class<?> targetClass = advised.getTargetSource().getTargetClass();
            if (targetClass != null) {
                if (targetClass.isInterface()) {
                    advised.setInterfaces(targetClass);
                } else if (Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
                    advised.setInterfaces(targetClass.getInterfaces());
                }
                proxiedInterfaces = advised.getProxiedInterfaces();
            }
        }

        List<Class<?>> interfaceList = new ArrayList<>(proxiedInterfaces.length);
        for (Class<?> anInterface : proxiedInterfaces) {
            // 从 JDK 17 开始支持 sealed class/interface（密封类/接口），即某个接口只能被指定的子类/子接口实现。
            if (!anInterface.isSealed()) {
                // 不是密封类/接口
                interfaceList.add(anInterface);
            }
        }

        return ClassUtils.toClassArray(interfaceList);

    }


    /**
     * 判断是否是equals方法
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean isEqualsMethod(Method method) {
        return method != null && method.getName().equals("equals") &&
                method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class;
    }

    /**
     * 判断是否是hashCode方法
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean isHashCodeMethod(Method method) {
        return method != null && method.getName().equals("hashCode") &&
                method.getParameterCount() == 0;
    }

    /**
     * 判断是否是toString方法
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean isToStringMethod(Method method) {
        return method != null && method.getName().equals("toString") &&
                method.getParameterCount() == 0;
    }

    /**
     * 判断是否是finalize方法
     *
     * @param method 方法
     * @return boolean
     */
    public static boolean isFinalizeMethod(Method method) {
        return (method != null && method.getName().equals("finalize") &&
                method.getParameterCount() == 0);
    }


    /**
     * 检查给定 AdvisedSupport 对象后面的代理是否相等。与 AdvisedSupport 对象的相等不同：接口、顾问和目标源的相等。
     */
    public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
        return (a == b ||
                (equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
    }

    /**
     * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
     */
    public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
        return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
    }

    /**
     * Check equality of the advisors behind the given AdvisedSupport objects.
     */
    public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
        return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
    }


}
