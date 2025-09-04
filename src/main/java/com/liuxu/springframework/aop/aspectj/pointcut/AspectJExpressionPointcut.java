package com.liuxu.springframework.aop.aspectj.pointcut;

import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.ProxyMethodInvocation;
import com.liuxu.springframework.aop.interceptor.ExposeInvocationInterceptor;
import com.liuxu.springframework.aop.matches.ClassFilter;
import com.liuxu.springframework.aop.matches.MethodMatcher;
import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.utils.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.reflect.PointcutParameterImpl;
import org.aspectj.weaver.reflect.ReflectionWorld;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParameter;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.aspectj.weaver.tools.ShadowMatch;
import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect 匹配切入点表达式
 *
 * @date: 2025-08-15
 * @author: liuxu
 */
public class AspectJExpressionPointcut implements ExpressionPointcut, ClassFilter, MethodMatcher {

    private static final Logger log = LoggerFactory.getLogger(AspectJExpressionPointcut.class);

    // Aspect 编译后的标识
    private static final String AJC_MAGIC = "ajc$";

    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = Set.of(
            PointcutPrimitive.EXECUTION,
            PointcutPrimitive.ARGS,
            PointcutPrimitive.REFERENCE,
            PointcutPrimitive.THIS,
            PointcutPrimitive.TARGET,
            PointcutPrimitive.WITHIN,
            PointcutPrimitive.AT_ANNOTATION,
            PointcutPrimitive.AT_WITHIN,
            PointcutPrimitive.AT_ARGS,
            PointcutPrimitive.AT_TARGET);


    private String location;

    private String expression;


    // 切点所声明的类。如果是@Before("pc()") 这种用到了切点方法的语法，AspectJ会去该类中找名称匹配的方法并拿到方法上切点表达式。
    // 又因为AspectJExpressionPointcut类会为每个通知方法创建实例，传入声明的类只能是当前方法所在类，
    // 也就导致Spring不支持使用其他切面类中的切点方法，例如：@Before("A.pc()") 是不支持的。
    private Class<?> pointcutDeclarationScope;

    /** 切面的类 是否是 AspectJ 编译器 (ajc) 编译后的 */
    private boolean aspectCompiledByAjc;


    /** 切点（通知）方法配置的参数名称数组 */
    private String[] pointcutParameterNames = new String[0];

    /** 切点（通知）方法配置的参数名称对应的类型数组 */
    private Class<?>[] pointcutParameterTypes = new Class<?>[0];


    private transient ClassLoader pointcutClassLoader;

    /** 底层AspectJ的切点表达式类 */
    private transient PointcutExpression pointcutExpression;


    // 标记切点是否解析失败了
    private transient boolean pointcutParsingFailed = false;

    private BeanFactory beanFactory;

    /** 切点表达式匹配结果缓存  key:方法 --> value:切点匹配结果 */
    private final transient Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);


    public AspectJExpressionPointcut(Class<?> pointcutDeclarationScope, String[] paramNames, Class<?>[] paramTypes) {
        this.pointcutDeclarationScope = pointcutDeclarationScope;
        if (paramNames.length != paramTypes.length) {
            throw new IllegalArgumentException("参数名称和参数类型长度不一致");
        }
        this.pointcutParameterNames = paramNames;
        this.pointcutParameterTypes = paramTypes;
    }

    public AspectJExpressionPointcut() {
    }

    @Override
    public String getExpression() {
        return this.expression;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
        this.pointcutDeclarationScope = pointcutDeclarationScope;
        this.aspectCompiledByAjc = compiledByAjc(pointcutDeclarationScope);
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * 指定类是否是 AspectJ 编译器 (ajc) 编译后的
     *
     * @param clazz 需要判断的类
     * @return true: 是
     */
    private static boolean compiledByAjc(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().startsWith(AJC_MAGIC)) {
                return true;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        return (superclass != null && compiledByAjc(superclass));
    }


    public void setPointcutParameterNames(String... pointcutParameterNames) {
        this.pointcutParameterNames = pointcutParameterNames;
    }

    public void setPointcutParameterTypes(Class<?>... pointcutParameterTypes) {
        this.pointcutParameterTypes = pointcutParameterTypes;
    }

    @Override
    public ClassFilter getClassFilter() {
        checkExpression();
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        checkExpression();
        return this;
    }


    private void checkExpression() {
        if (getExpression() == null) {
            throw new IllegalStateException("Must set property 'expression' before attempting to match");
        }
    }


    /**
     * 懒加载底层 AspectJ 切入点表达式
     *
     * @return 底层 AspectJ 切点表达式
     */
    private PointcutExpression obtainPointcutExpression() {
        if (this.pointcutExpression == null) {
            this.pointcutClassLoader = determinePointcutClassLoader();
            this.pointcutExpression = buildPointcutExpression(this.pointcutClassLoader);
        }
        return this.pointcutExpression;
    }

    /**
     * 创建底层 AspectJ 切点表达式
     *
     * @param pointcutClassLoader 切点类加载器
     * @return 切点表达式
     */
    private PointcutExpression buildPointcutExpression(ClassLoader pointcutClassLoader) {
        // 初始化解析器
        PointcutParser parser = initializePointcutParser(pointcutClassLoader);

        // 创建切点参数
        PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
        for (int i = 0; i < pointcutParameters.length; i++) {
            pointcutParameters[i] = new PointcutParameterImpl(
                    this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
        }

        // 解析给定的切点表达式
        return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()),
                this.pointcutDeclarationScope, pointcutParameters);
    }


    /**
     * 替换布尔操作
     * 我们还允许在两个切入点子表达式之间使用 "and" 。
     * 此方法转换回 "&&"， AspectJ 切入点解析器支持的语法
     *
     * @param pcExpr 需要替换切点表达式
     * @return 替换后的切点表达式
     */
    private String replaceBooleanOperators(String pcExpr) {
        String repExpr = StringUtils.replace(pcExpr, " and ", " && ");
        repExpr = StringUtils.replace(repExpr, " or ", " || ");
        repExpr = StringUtils.replace(repExpr, " not ", " ! ");

        return repExpr;
    }

    /** 确定切点表达式 */
    private String resolveExpression() {
        String expression = getExpression();
        if (expression == null) {
            throw new IllegalStateException("Must set property 'expression' before attempting to match");
        }
        return expression;
    }

    /**
     * 初始化底层 AspectJ 切入点解析器
     */
    private PointcutParser initializePointcutParser(ClassLoader classLoader) {
        return PointcutParser
                .getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
                        SUPPORTED_PRIMITIVES, classLoader);
    }

    /**
     * 切点AspectJ的切点类所需的类加载器
     *
     * @return 切点类加载器
     */
    private ClassLoader determinePointcutClassLoader() {
        if (this.beanFactory instanceof DefaultListableBeanFactory dlbf) {
            return dlbf.getBeanClassLoader();
        }
        if (this.pointcutDeclarationScope != null) {
            return this.pointcutDeclarationScope.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    /**
     * 返回底层 AspectJ 切入点表达式
     */
    public PointcutExpression getPointcutExpression() {
        return obtainPointcutExpression();
    }

    /**
     * 类级别匹配
     *
     * @param targetClass 目标类
     * @return
     */
    @Override
    public boolean matches(Class<?> targetClass) {
        if (this.pointcutParsingFailed) {
            return false;
        }
        if (this.aspectCompiledByAjc && compiledByAjc(targetClass)) {
            // 切面类和目标类都是 AspectJ 编译器 (ajc) 编译过的类型 -> 表明已在编译期间将增强的代码编织进字节码。
            return false;
        }

        try {
            try {
                // 使用底层AspectJ切点表达式进行匹配：是否能够匹配指定类型中的连接点
                return obtainPointcutExpression().couldMatchJoinPointsInType(targetClass);
            } catch (ReflectionWorld.ReflectionWorldException ex) {
                log.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
                // 尝试使用目标的类的类加载器创建切点表达式再次匹配
                PointcutExpression fallbackPointcutExpression = getFallbackPointcutExpression(targetClass);
                if (fallbackPointcutExpression != null) {
                    return fallbackPointcutExpression.couldMatchJoinPointsInType(targetClass);
                }
            }
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedPointcutPrimitiveException ex) {
            this.pointcutParsingFailed = true; // 标记切点解析失败
            log.debug("[ERROR] 切点解析器拒绝了该表达式 [{}] 错误: {}", getExpression(), ex.getMessage());
        } catch (Throwable ex) {
            log.debug("[ERROR] 切点表达式匹配拒绝了目标类", ex);
        }

        return false;
    }


    /**
     * 方法级别匹配
     *
     * @param method      候选方法
     * @param targetClass 目标类
     * @return
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 方法匹配结果
        ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

        // 有三种匹配结果：
        // 1. always matches → 一定匹配
        // 2. never matches → 一定不匹配
        // 3. maybe matches → 可能匹配，需要 runtime 检查（Residue）
        if (shadowMatch.alwaysMatches()) {
            return true;
        } else if (shadowMatch.neverMatches()) {
            return false;
        }

        // 如果测试中涉及任何子类型敏感变量（this、target、at_this、at_target、at_annotation），
        // 那么我们说这不是匹配，因为在 Spring 中永远不会有不同的运行时子类型。
        RuntimeTestWalker testWalker = getRuntimeTestWalker(shadowMatch);

        return (!testWalker.testsSubtypeSensitiveVars()  // 如果不是子类敏感（切点表达式没有this() target() 这类残留判断）就当匹配通过
                || testWalker.testTargetInstanceOfResidue(targetClass));

    }

    @Override
    public boolean isRuntime() {
        // 是否需要在运行时做额外的判断（是否需要动态匹配）
        return obtainPointcutExpression().mayNeedDynamicTest();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, Object... args) {
        // 动态匹配
        ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);


        // 将 Spring AOP 的代理对象绑定给 AspectJ 的 thisObject，
        // 将 Spring AOP 的目标对象绑定给 AspectJ 的 targetObject
        Object targetObject = null;
        Object thisObject = null;
        ProxyMethodInvocation pmi = null;
        try {
            // 拿到当前线程关联的 AOP 方法调用器
            MethodInvocation curr = ExposeInvocationInterceptor.currentInvocation();

            targetObject = curr.getThis();
            if (!(curr instanceof ProxyMethodInvocation currPmi)) {
                throw new IllegalStateException("MethodInvocation 不是 Spring ProxyMethodInvocation: " + curr);
            }

            thisObject = currPmi.getProxy();
            pmi = currPmi;
        } catch (IllegalStateException e) {
            log.info("[ERROR] 无法获取当前线程上下文关联的 AOP 方法调用器(ProxyMethodInvocation)", e);
        }


        try {
            // 连接点匹配，并返回结果
            JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, args);


            // 能拿到这两个变量，说明是一次真正的方法调用
            if (pmi != null && thisObject != null) {

                // 既然是真正的方法调用，能拿到真正的代理对象类型（thisObject），需要再校验一次切点表达式中this() 类型是否匹配，避免仅靠静态匹配造成误判
                RuntimeTestWalker runtimeTestWalker = getRuntimeTestWalker(getShadowMatch(method, method));
                if (!runtimeTestWalker.testThisInstanceOfResidue(thisObject.getClass())) {
                    // 说明切点里对 this(Type) 的要求在当前代理类型上不成立
                    return false;
                }

                if (joinPointMatch.matches()) {
                    // 将连接点匹配存入当前线程暴露的方法调用器中，可用于后续通知方法参数绑定。
                    pmi.setUserAttribute(getExpression(), joinPointMatch);
                }
            }

            return joinPointMatch.matches();
        } catch (Exception e) {
            // 无法评估参数的连接点
            log.debug("[ERROR] 无法评估带参数的连接点，参数 {}, 回退到不匹配", Arrays.toString(args), e);
        }

        return false;

    }


    /**
     * 尝试获取新的切入点表达式
     * 根据目标类的加载器而不是默认值的类加载器.
     *
     * @param targetClass 目标类
     * @return 切点表达式
     */
    private PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
        try {
            ClassLoader classLoader = targetClass.getClassLoader();
            if (classLoader != null && classLoader != this.pointcutClassLoader) {
                return buildPointcutExpression(classLoader);
            }
        } catch (Throwable ex) {
            log.debug("[ERROR] 创建备用的 PointcutExpression 失败 Failed to create fallback PointcutExpression", ex);
        }
        return null;
    }


    private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
        // spring 中对 ShadowMatch 进行了扩展，支持备用的切点匹配。
        return new RuntimeTestWalker(shadowMatch);
    }

    /**
     * 获取目标方法对应的 ShadowMatch（AspectJ匹配结果）
     *
     * @param method      目标方法
     * @param targetClass 目标类
     * @return ShadowMatch（AspectJ匹配结果）
     */
    private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
        // 获取具体的方法
        Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        if (mostSpecificMethod.getDeclaringClass().isInterface() && // 接口声明的方法
                mostSpecificMethod.getDeclaringClass() != targetClass &&
                obtainPointcutExpression().getPointcutExpression().contains("." + mostSpecificMethod.getName() + "(") // 切点表达式包含该方法
        ) {
            // 所有的接口
            Set<Class<?>> ifs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
            if (ifs.size() > 1) {
                try {
                    // 创建一个组合接口，如果目标类走的是JDK代理，就会用组合接口定义一个代理
                    targetClass = ClassUtils.createCompositeInterface(ClassUtils.toClassArray(ifs), targetClass.getClassLoader());
                    // 再次尝试获取具体的方法
                    mostSpecificMethod = ClassUtils.getMostSpecificMethod(mostSpecificMethod, targetClass);
                } catch (Exception e) {
                    // ......
                }
            }
        }

        return getShadowMatch(mostSpecificMethod, method);
    }

    /**
     * 获取ShadowMatch 匹配结果
     *
     * @param targetMethod   目标方法
     * @param originalMethod 原始方法
     * @return ShadowMatch 匹配结果
     */
    private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
        ShadowMatch shadowMatch = this.shadowMatchCache.get(targetMethod);
        if (shadowMatch == null) {
            synchronized (this.shadowMatchCache) {
                shadowMatch = this.shadowMatchCache.get(targetMethod);
                if (shadowMatch == null) {
                    PointcutExpression fallbackPointcutExpression = null;
                    Method methodToMatch = targetMethod;


                    try {
                        // 首先尝试匹配 targetMethod 方法
                        try {
                            // 判断某个方法的执行点（method execution join point）是否匹配切点表达式。
                            shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
                        } catch (ReflectionWorld.ReflectionWorldException ex1) {
                            try {
                                fallbackPointcutExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
                                if (fallbackPointcutExpression != null) {
                                    shadowMatch = fallbackPointcutExpression.matchesMethodExecution(methodToMatch);
                                }
                            } catch (ReflectionWorld.ReflectionWorldException ex2) {
                                fallbackPointcutExpression = null;
                            }
                        }


                        // 匹配失败，再尝试匹配 originalMethod 方法
                        if (targetMethod != originalMethod &&
                                //  shadowMatch 是空   或者  shadowMatch永不匹配并且方法的声明类是代理类
                                (shadowMatch == null || (shadowMatch.neverMatches() && Proxy.isProxyClass(targetMethod.getDeclaringClass())))) {
                            methodToMatch = originalMethod;

                            try {
                                shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
                            } catch (ReflectionWorld.ReflectionWorldException ex1) {
                                try {
                                    fallbackPointcutExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
                                    if (fallbackPointcutExpression != null) {
                                        shadowMatch = fallbackPointcutExpression.matchesMethodExecution(methodToMatch);
                                    }
                                } catch (ReflectionWorld.ReflectionWorldException ex2) {
                                    fallbackPointcutExpression = null;
                                }
                            }

                        }
                    } catch (Exception e) {
                        log.debug("PointcutExpression匹配被拒绝的目标方法", e);
                        fallbackPointcutExpression = null;
                    }

                    // 依旧没匹配成功，创建一个"永不匹配"的匹配结果存入缓存
                    if (shadowMatch == null) {
                        shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
                    } else if (shadowMatch.neverMatches() && fallbackPointcutExpression != null) {
                        // 可能匹配 并且是 次选切点匹配的结果
                        // Spring中对此会创建一个带备选匹配的匹配结果.....
                    }

                    // 存入缓存
                    this.shadowMatchCache.put(methodToMatch, shadowMatch);

                }
            }
        }

        return shadowMatch;

    }

    //...............SpringAOP添加了切点表达式对 bean() 的支持，这就不实现扩展了..........................


}
