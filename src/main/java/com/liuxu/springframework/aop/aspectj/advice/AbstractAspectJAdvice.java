package com.liuxu.springframework.aop.aspectj.advice;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.ProxyMethodInvocation;
import com.liuxu.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer;
import com.liuxu.springframework.aop.aspectj.AspectJPrecedenceInformation;
import com.liuxu.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import com.liuxu.springframework.aop.interceptor.ExposeInvocationInterceptor;
import com.liuxu.springframework.aop.utils.AspectJProxyUtils;
import com.liuxu.springframework.core.ParameterNameDiscoverer;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * AspectJ 通知的实现模板
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation {
    // 日志
    private static final Logger log = LoggerFactory.getLogger(AbstractAspectJAdvice.class);

    private static final String JOIN_POINT_KEY = JoinPoint.class.getName();

    /**
     * 获取当前连接点信息
     * 它描述的：当前正在执行的目标点（目标方法）
     * 提供 上下文信息，让切面方法知道自己正在哪个方法上执行、参数是什么、目标对象是谁。
     * 返回的是 JoinPoint 类型，是没有 proceed() 方法，避免破坏责任链
     */
    public static JoinPoint currentJoinPoint() {

        MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
        if (!(mi instanceof ProxyMethodInvocation pmi)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
        if (jp == null) {
            jp = new MethodInvocationProceedingJoinPoint(pmi);
            pmi.setUserAttribute(JOIN_POINT_KEY, jp);
        }
        return jp;
    }

    /**
     * 这一个切面中，此通知的执行顺序.
     */
    private int declarationOrder;


    /**
     * 切面方法的声明类
     */
    private final Class<?> declaringClass;
    // 切面方法的名称
    private final String methodName;
    // 切面方法的参数类型数组
    private final Class<?>[] parameterTypes;
    // 切面的方法
    protected transient Method aspectJAdviceMethod;
    // AspectJ切点表达式
    private final AspectJExpressionPointcut pointcut;
    // AspectJ 获取切面类的实例工厂
    private final AspectInstanceFactory aspectInstanceFactory;

    // 标记参数是否已检查（只运行执行一次）
    private boolean argumentsIntrospected = false;

    private int joinPointArgumentIndex = -1; // 记录类型是joinPoint的参数在方法的参数数组中的索引位置
    private int joinPointStaticPartArgumentIndex = -1;// 记录类型是JoinPoint.StaticPart.class的参数在方法的参数数组中的索引位置


    /** aspectJ 切面类在Spring中的BeanName */
    private String aspectName = "";

    /** 参数绑定 key:显示绑定的参数名称 --> value:参数所在方法参数列表的索引 */
    private Map<String, Integer> argumentBindings;


    /**
     * 如果此通知对象的创建者知道参数名称并显式设置它们，则这将为非空。
     * 例如：{@code @Around(argNames="arg1,arg2,arg3")}
     */
    private String[] argumentNames;

    /** 指定抛出的异常名称，例如：{@code @AfterThrowing(throwing="ex")} */
    private String throwingName;

    /** 指定返回值名称，例如：{@code @AfterReturning(returning="retVal")} */
    private String returningName;

    // 确定的返回值类型，例如：List.class
    private Class<?> discoveredReturningType = Object.class;
    // 确定的异常类型
    private Class<?> discoveredThrowingType = Object.class;

    // 确定返回值的type及泛型（例如：List<String>）  null没有声明泛型
    private Type discoveredReturningGenericType;


    public AbstractAspectJAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
        this.methodName = aspectJAdviceMethod.getName();
        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
        this.aspectInstanceFactory = aspectInstanceFactory;
        this.aspectJAdviceMethod = aspectJAdviceMethod;
        this.pointcut = pointcut;
    }


    /**
     * 调用切面方法
     *
     * @param jpMatch     切面方法匹配 连接点信息（包含目标方法、参数等）
     * @param returnValue 切面方法返回值 (后置通知才有返回值)
     * @param ex          切面方法异常 (异常通知才有异常对象)
     * @return 切面方法返回值
     */
    protected Object invokeAdviceMethod(JoinPointMatch jpMatch, Object returnValue, Throwable ex) throws Throwable {
        return invokeAdviceMethodWithGivenArgs(argBinding(getJonPoint(), jpMatch, returnValue, ex));
    }


    /**
     * 环绕通知调用切面方法
     *
     * @param jp          环绕通知连接点信息
     * @param jpMatch     切面方法匹配
     * @param returnValue 环绕通知返回值
     * @param t           环绕通知异常
     * @return 环绕通知返回值
     * @throws Throwable 执行环绕通知时可能出现异常
     */
    protected Object invokeAdviceMethod(JoinPoint jp, JoinPointMatch jpMatch,
                                        Object returnValue, Throwable t) throws Throwable {

        return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
    }

    /**
     * 调用切面方法
     *
     * @param args 切面方法参数
     * @return 切面方法返回值
     * @throws Throwable 切面方法异常
     */
    protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
        Object[] actualArgs = args;
        if (this.aspectJAdviceMethod.getParameterCount() == 0) {
            actualArgs = null;
        }
        try {
            ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
            return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("与通知方法的参数不匹配", e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }


    /**
     * 参数绑定
     *
     * @param jp          当前连接点
     * @param jpMatch     切面方法匹配
     * @param returnValue 切面方法返回值
     * @param ex          切面抛出方法异常
     * @return 参数绑定后的参数数组
     */
    protected Object[] argBinding(JoinPoint jp, JoinPointMatch jpMatch,
                                  Object returnValue, Throwable ex) {

        // 推算并绑定参数的名称类型
        calculateArgumentBindings();

        Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
        int numBound = 0;

        if (this.joinPointArgumentIndex != -1) {
            adviceInvocationArgs[joinPointArgumentIndex] = jp;
            numBound++;
        } else if (this.joinPointStaticPartArgumentIndex != -1) {
            adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
            numBound++;
        }

        // 处理绑定的参数
        if (this.argumentBindings != null && !this.argumentBindings.isEmpty()) {

            if (jpMatch != null) {
                // jpMatch：AspectJ的API，存放的是切点表达式里绑定的参数名称，以及参数的值。
                PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
                for (PointcutParameter parameterBinding : parameterBindings) {
                    Integer index = this.argumentBindings.get(parameterBinding.getName());
                    adviceInvocationArgs[index] = parameterBinding.getBinding();
                    numBound++;
                }
            }

            // 绑定返回值
            if (this.returningName != null) {
                Integer index = this.argumentBindings.get(this.returningName);
                adviceInvocationArgs[index] = returnValue;
                numBound++;
            }
            // 绑定异常
            if (this.throwingName != null) {
                Integer index = this.argumentBindings.get(this.throwingName);
                adviceInvocationArgs[index] = ex;
                numBound++;
            }
        }

        if (numBound != this.parameterTypes.length) {
            // 实际需要绑定的参数数量和通知方法参数数量不一致
            throw new IllegalStateException("通知方法需要绑定 " + this.parameterTypes.length + " 个参数，但只绑定了 " + numBound + " 个参数. (JoinPointMatch " + (jpMatch != null ? "已参与" : "没有参与") + " 绑定)");
        }

        return adviceInvocationArgs;
    }


    /**
     * 推算并绑定参数的名称和类型
     */
    public final void calculateArgumentBindings() {
        if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
            return;
        }

        // 标记通知方法的参数数量
        int numUnboundArgs = this.parameterTypes.length;
        Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
        // 检查第一个参数是否是切点类型
        if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0])
                || maybeBindJoinPointStaticPart(parameterTypes[0])) {
            numUnboundArgs--;
        }

        // 还存在参数，继续按切入点表达式匹配的名称绑定参数
        if (numUnboundArgs > 0) {
            bindArgumentsByName(numUnboundArgs);
        }

        this.argumentsIntrospected = true; // 标记参数已检查过

    }

    private void bindArgumentsByName(int numArgumentsExpectingToBind) {
        if (this.argumentNames == null) {
            // 尝试从切点表达上解析配置的参数名称
            this.argumentNames = createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);
        }

        if (this.argumentNames != null) {
            // 绑定已明确名称的参数
            bindExplicitArguments(numArgumentsExpectingToBind);
        } else {
            throw new IllegalStateException("没有为通知方法指定参数名称");
        }
    }

    /**
     * 创建一个 ParameterNameDiscoverer 以用于参数绑定
     *
     * @return
     */
    private ParameterNameDiscoverer createParameterNameDiscoverer() {
        // 当推断参数名称失败的场景下会抛出异常
        AspectJAdviceParameterNameDiscoverer parameterNameDiscoverer = new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
        parameterNameDiscoverer.setThrowingName(this.throwingName);
        parameterNameDiscoverer.setReturningName(this.returningName);
        parameterNameDiscoverer.setRaiseExceptions(true);
        return parameterNameDiscoverer;
    }


    private void bindExplicitArguments(int bindExplicitArguments) {
        int parameterCount = this.aspectJAdviceMethod.getParameterCount();
        if (this.argumentNames.length != parameterCount) { // 注意连接点参数只会作为第一个参数，而且不能显示绑定名称，会默认在 argumentNames[0] 添加一个名称占位。
            throw new IllegalStateException(
                    "期望在通知方法中通过名称来绑定" + parameterCount +
                            "个参数，但实际发现有" + this.argumentNames.length + "个参数");
        }
        this.argumentBindings = new HashMap<>();

        // 前面可已处理过连接点参数，需确认要匹配参数的起始索引
        int argumentsIndexOffset = this.parameterTypes.length - bindExplicitArguments;
        // 将剩余的参数名称与类型做好映射，key:参数名称 --> value:参数类型所在的索引
        for (int i = argumentsIndexOffset; i < this.argumentNames.length; i++) {
            this.argumentBindings.put(this.argumentNames[i], i);
        }

        /*如果指定了 返回参数和异常参数的名称，尝试绑定参数的名称和类型*/
        if (this.returningName != null) {
            if (!this.argumentBindings.containsKey(this.returningName)) {
                // 抛出异常，配置的指定从参数名称不匹配
                throw new IllegalStateException(" @AfterReturning 配置的返回参数名称与通知方法的参数不匹配：" + this.returningName);
            } else {
                // 确定返回值的类型
                Integer index = this.argumentBindings.get(this.returningName);
                this.discoveredReturningType = this.aspectJAdviceMethod.getParameterTypes()[index];
                // 确定泛型
                this.discoveredReturningGenericType = this.aspectJAdviceMethod.getGenericReturnType();
            }
        }

        if (this.throwingName != null) {
            if (!this.argumentBindings.containsKey(this.throwingName)) {
                throw new IllegalStateException(" @AfterThrowing 配置的异常参数名称与通知方法的参数不匹配：" + this.throwingName);
            } else {
                // 确定异常的类型
                Integer index = this.argumentBindings.get(this.throwingName);
                this.discoveredThrowingType = this.aspectJAdviceMethod.getParameterTypes()[index];
            }
        }

        // 处理切点表达式绑定的参数
        configurePointcutParameters(this.argumentNames, argumentsIndexOffset);

    }

    /**
     * 处理切点表达式显示填写的参数名称，将其类型及名称和方法的参数位置找到
     * 例如: @Before("execution(* com.example..*(..)) && args(name, age)") 也就是 (name, age) 两个参数找到它在通知方法参数位置和类型
     *
     * @param argumentNames       显示配置的参数名称数组
     * @param argumentIndexOffset 参数名称数组开始处理的索引偏移量
     */
    private void configurePointcutParameters(String[] argumentNames, int argumentIndexOffset) {
        int numParametersToRemove = argumentIndexOffset;
        if (this.returningName != null) {
            numParametersToRemove++;
        }
        if (this.throwingName != null) {
            numParametersToRemove++;
        }

        // 计算剩余配置了名称的参数的数量，这些是切点表达式显示绑定的参数
        // 例如: @Before("execution(* com.example..*(..)) && args(name, age)")
        String[] pointcutParameterNames = new String[argumentNames.length - numParametersToRemove];
        Class<?>[] paramTypes = new Class<?>[pointcutParameterNames.length];
        Class<?>[] methodParameterTypes = this.aspectJAdviceMethod.getParameterTypes();

        int index = 0;
        for (int i = argumentIndexOffset; i < argumentNames.length; i++) {
            if (argumentNames[i].equals(this.returningName) ||
                    argumentNames[i].equals(this.throwingName)) {
                continue;
            }

            // 拿到参数名称、和类型
            pointcutParameterNames[index] = argumentNames[i];
            paramTypes[index] = methodParameterTypes[i];
            index++;

        }

        // 关联在连接点上
        this.pointcut.setPointcutParameterNames(pointcutParameterNames);
        this.pointcut.setPointcutParameterTypes(paramTypes);

    }

    /**
     * 如果参数名称已知，则由此通知对象的创建者设置。
     * 例如，这可能是因为它们已在 XML 或通知注解中显式指定。
     *
     * @param argumentNames 参数名称数组
     */
    public void setArgumentNamesFromStringArray(String... argumentNames) {
        this.argumentNames = new String[argumentNames.length];
        for (int i = 0; i < this.argumentNames.length; i++) {
            this.argumentNames[i] = argumentNames[i];
            if (!isVariableName(this.argumentNames[i])) {
                throw new IllegalArgumentException("参数名称不合法：" + this.argumentNames[i]);
            }
        }

        // 检查通知方法的参数中是否有切点匹配参数。
        if (this.aspectJAdviceMethod.getParameterCount() == this.argumentNames.length + 1) {
            Class<?> firstParameter = this.aspectJAdviceMethod.getParameterTypes()[0];

            // 需要添加隐式的参数名称: 比如第一个参数是 JoinPoint 或 ProceedingJoinPoint , 但 argNames 并没有添加 join 参数.
            // 例如:
            // @After(value = "execution(* com.liuxu.example.testaop.BeanAop1.foo())",argNames = "")
            // public void after(JoinPoint join){...}
            if (firstParameter == JoinPoint.class ||
                    firstParameter == ProceedingJoinPoint.class ||
                    firstParameter == JoinPoint.StaticPart.class) {
                String[] oldNames = this.argumentNames;
                this.argumentNames = new String[oldNames.length + 1];
                this.argumentNames[0] = "THIS_JOIN_POINT"; // 标记切点信息的参数名称，占位作用。连接点不会根据名称匹配，只匹配方法的第一个参数类型
                System.arraycopy(oldNames, 0, this.argumentNames, 1, oldNames.length);
            }
        }
    }


    public void setThrowingName(String throwingName) {
        throw new UnsupportedOperationException("只有 afterThrowing advice 才能绑定异常对象");
    }

    public void setReturningName(String returningName) {
        throw new UnsupportedOperationException("只有 afterReturning advice 才能绑定返回值");
    }

    public void setAspectName(String aspectName) {
        this.aspectName = aspectName;
    }

    public void setDeclarationOrder(int declarationOrder) {
        this.declarationOrder = declarationOrder;
    }


    /** 获取目标方法返回值类型 因为 如果@AfterReturning的通知方法的参数可能需要绑定目标方法的返回值 */
    public Class<?> getDiscoveredReturningType() {
        return discoveredReturningType;
    }

    /** 获取目标方法抛出的异常类型 因为 如果@AfterThrowing的通知方法的参数可能需要绑定目标方法抛出的异常 */
    public Class<?> getDiscoveredThrowingType() {
        return discoveredThrowingType;
    }

    /**
     * 设置并检查异常通知指定的异常名称或类型
     *
     * @param name 异常名称或类型
     */
    protected void setThrowingNameNoCheck(String name) {
        // 校验name是否符合Java标识符规范
        if (isVariableName(name)) {
            this.throwingName = name;
        } else {
            // 尝试获取类型
            try {
                this.discoveredThrowingType = Class.forName(name, false, this.aspectInstanceFactory.getAspectClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("抛出的错误信息为：" + name + " 名字，这个名称既不是有效的参数名称，也不是类路径中 Java 类的完整限定名称 ");
            }
        }
    }

    /**
     * 我们需要将返回名称保持在此级别以进行参数绑定计算，此方法允许 afterReturning 通知子类设置名称。
     *
     * @param name 返回名称或类型
     */
    protected void setReturningNameNoCheck(String name) {
        // 校验name是否符合Java标识符规范
        if (isVariableName(name)) {
            this.returningName = name;
        } else {
            // 尝试获取类型
            try {
                this.discoveredReturningType = Class.forName(name, false, this.aspectInstanceFactory.getAspectClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("返回值名称为：" + name + " 错误，这个名称既不是有效的参数名称，也不是类路径中 Java 类的完整限定名称 ");
            }
        }
    }


    // 验证是否是变量名称
    private static boolean isVariableName(String name) {
        return AspectJProxyUtils.isVariableName(name);
    }

    /**
     * 是否支持 ProceedingJoinPoint
     * 只有环绕通知支持
     *
     * @return true
     */
    protected boolean supportsProceedingJoinPoint() {
        return false;
    }


    /* 尝试绑定 JoinPoint.class*/
    private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
        if (JoinPoint.class == candidateParameterType) {
            this.joinPointArgumentIndex = 0;
            return true;
        } else {
            return false;
        }
    }

    /* 尝试绑定 ProceedingJoinPoint.class*/
    private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
        if (ProceedingJoinPoint.class == candidateParameterType) {
            // 只有环绕通知才支持 ProceedingJoinPoint 类型的参数
            if (!supportsProceedingJoinPoint()) {
                throw new IllegalArgumentException("ProceedingJoinPoint 参数只支持 @Around 环绕通知");
            }
            this.joinPointArgumentIndex = 0;
            return true;
        } else {
            return false;
        }
    }

    /* 尝试绑定 JoinPoint.StaticPart.class*/
    private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType) {
        if (JoinPoint.StaticPart.class == candidateParameterType) {
            this.joinPointStaticPartArgumentIndex = 0;
            return true;
        } else {
            return false;
        }
    }


    @Override
    public String getAspectName() {
        return this.aspectName;
    }

    @Override
    public int getDeclarationOrder() {
        return this.declarationOrder;
    }

    public Type getDiscoveredReturningGenericType() {
        return discoveredReturningGenericType;
    }


    /**
     * 获取当前连接点（当前在执行的目标方法）
     *
     * @return 当前连接点
     */
    private JoinPoint getJonPoint() {
        return currentJoinPoint();
    }


    /**
     * 获取连接点匹配匹配信息
     * 连接点匹配信息：记录连接点匹配过程的结果，如果AspectJ注解显示标记了参数名称，还要保存绑定的参数变量。
     */
    protected JoinPointMatch getJoinPointMatch() {
        MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
        if (!(mi instanceof ProxyMethodInvocation pmi)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        return getJoinPointMatch(pmi);
    }

    private JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi) {
        String expression = this.pointcut.getExpression();
        return (pmi != null ? (JoinPointMatch) pmi.getUserAttribute(expression) : null);
    }


}

