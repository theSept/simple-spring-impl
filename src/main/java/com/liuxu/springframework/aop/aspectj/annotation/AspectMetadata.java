package com.liuxu.springframework.aop.aspectj.annotation;

import com.liuxu.springframework.aop.Pointcut;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;

import java.io.Serializable;

/**
 * Aspect 类的元数据信息
 *
 * @date: 2025-08-15
 * @author: liuxu
 */
public class AspectMetadata implements Serializable {

    /**
     * aspect类在 Spring中定义的 beanName
     */
    private final String aspectName;

    /**
     * aspect 切面类的类型，单独存储，以便在反序列化时重新解析相应的 AjType
     */
    private final Class<?> aspectClass;

    /**
     * AspectJ 类型描述对象（能访问到 per-clause、切点等信息）。
     */
    private final AjType<?> ajType;

    /**
     * Spring AOP 切入对应于方面的 per 子句。如果是单例，则为 Pointcut.TRUE 规范实例，否则为 AspectJExpressionPointcutAdvisor。
     * 是 AspectJ 切面的 实例化模型，控制切面对象的生命周期
     */
    private final Pointcut perClausePointcut;


    public AspectMetadata(String aspectName, Class<?> aspectClass) {
        this.aspectName = aspectName;

        Class<?> currClass = aspectClass;
        // 子->父类 遍历确认是不是一个切面类
        AjType<?> ajType = null;
        while (currClass != Object.class) {
            AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
            if (ajTypeToCheck.isAspect()) {
                ajType = ajTypeToCheck;
                break;
            }
            currClass = currClass.getSuperclass();
        }

        if (ajType == null) {
            throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] 不是 @AspectJ 注解的 aspect 类");
        }


        // declare precedence 是 AspectJ 中控制切面执行优先级的语法（declare precedence: Aspect1, Aspect2;）
        if (ajType.getDeclarePrecedence().length > 0) {
            // Spring AOP 不支持这个 AspectJ 特性
            throw new IllegalArgumentException("Spring AOP 目前不支持 DeclarePrecedence");
        }

        // 原始 Java 类。
        this.aspectClass = ajType.getJavaClass();
        this.ajType = ajType;


        // 根据 per-clause 创建 Pointcut
        switch (this.ajType.getPerClause().getKind()) {
            // 单例 @Aspect("singleton") 不写singleton默认单例
            case SINGLETON -> {
                // 始终匹配的
                this.perClausePointcut = Pointcut.TRUE;
            }
            // PERTARGET：每一个匹配的目标对象都会创建一个新的切面实例 例：@Aspect("pertarget(execution(* com.example..*(..)))")
            // PERTHIS：每一个执行连接点的 this 对象都会有单独的切面实例 例如：@Aspect("perthis(execution(* com.example..*(..)))")
            case PERTARGET, PERTHIS -> {
                AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
                ajexp.setLocation(aspectClass.getName());
                ajexp.setExpression(findPerClause(aspectClass));
                ajexp.setPointcutDeclarationScope(aspectClass);
                this.perClausePointcut = ajexp;
            }
            default ->
                    throw new IllegalStateException("Unexpected advice type: " + this.ajType.getPerClause().getKind().toString());

        }

    }


    /**
     * 获取 per-clause
     */
    private String findPerClause(Class<?> aspectClass) {
        Aspect ann = aspectClass.getAnnotation(Aspect.class);
        if (ann == null) {
            return "";
        }
        String value = ann.value();
        int beginIndex = value.indexOf('(');
        if (beginIndex < 0) {
            return "";
        }
        return value.substring(beginIndex + 1, value.length() - 1);
    }


    public String getAspectName() {
        return aspectName;
    }

    public Class<?> getAspectClass() {
        return aspectClass;
    }

    public AjType<?> getAjType() {
        return ajType;
    }

    public Pointcut getPerClausePointcut() {
        return perClausePointcut;
    }
}
