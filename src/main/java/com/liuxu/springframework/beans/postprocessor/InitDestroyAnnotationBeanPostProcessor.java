package com.liuxu.springframework.beans.postprocessor;

import com.liuxu.springframework.beans.annotion.PostConstruct;
import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;
import com.liuxu.springframework.beans.interfaces.DestructionAwareBeanPostProcessor;
import com.liuxu.springframework.beans.interfaces.MergedBeanDefinitionPostProcessor;
import com.liuxu.springframework.utils.AnnotationUtils;
import com.liuxu.springframework.utils.ClassUtils;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 初始化、销毁方法 后处理器
 *
 * @date: 2025-07-17
 * @author: liuxu
 */
public class InitDestroyAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor {
    // transient 关键词的作用：不参与序列化
    private transient final Logger log = LoggerFactory.getLogger(getClass());

    private final transient LifecycleMetadata emptyLifecycleMetadata =
            new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList());

    // 标注初始化方法的注解
    private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);
    // 标注销毁方法的注解
    private final Set<Class<? extends Annotation>> destroyAnnotationTypes = new LinkedHashSet<>(2);

    // 生命周期元数据缓存  beanClass -> 生命周期元数据
    private final Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);

    /**
     * 指定要检查的 标识初始化方法 的注解. 指示在配置 bean 后要调用的初始化方法。
     * 可以使用任何自定义注解.
     * 没有默认值，但典型的选择是 {@link PostConstruct} 注解。
     */
    public void setInitAnnotationTypes(Class<? extends Annotation> initAnnotationType) {
        this.initAnnotationTypes.clear();
        this.initAnnotationTypes.add(initAnnotationType);
    }

    /**
     * 添加标识初始化方法的注解。在配置 bean 后要调用的初始化方法
     */
    public void addInitAnnotationTypes(Class<? extends Annotation> initAnnotationType) {
        if (initAnnotationType != null) {
            this.initAnnotationTypes.add(initAnnotationType);
        }
    }

    /**
     * 指定要检查的 销毁方法 的注解。指示在销毁 bean 后要调用的销毁方法。
     * 可以使用任何自定义注解.
     * 没有默认值，但典型选择是 {@link com.liuxu.springframework.beans.annotion.PreDestroy} 注解。
     */
    public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
        this.destroyAnnotationTypes.clear();
        this.destroyAnnotationTypes.add(destroyAnnotationType);
    }

    /**
     * 添加标识销毁方法注解
     */
    public void addDestroyAnnotationTypes(Class<? extends Annotation> destroyAnnotationType) {
        if (destroyAnnotationType != null) {
            this.destroyAnnotationTypes.add(destroyAnnotationType);
        }
    }


    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        findLifecycleMetadata(beanDefinition, beanType);
    }

    /**
     * 查找指定 Bean 的生命周期元数据
     * - 查询 bean 的生命周期元数据并缓存
     * - 检查不是外部声明管理的初始化、销毁方法
     *
     * @param beanDefinition 合并后的 BeanDefinition
     * @param beanClass      Bean 类
     * @return 生命周期元数据
     */
    private LifecycleMetadata findLifecycleMetadata(RootBeanDefinition beanDefinition, Class<?> beanClass) {
        LifecycleMetadata lifecycleMetadata = findLifecycleMetadata(beanClass);
        lifecycleMetadata.checkInitDestroyMethods(beanDefinition);
        return lifecycleMetadata;
    }

    private LifecycleMetadata findLifecycleMetadata(Class<?> beanClass) {
        LifecycleMetadata metadata = lifecycleMetadataCache.get(beanClass);
        if (metadata == null) {
            synchronized (this.lifecycleMetadataCache) {
                metadata = lifecycleMetadataCache.get(beanClass);
                if (metadata == null) {
                    // 构建 bean 生命周期元数据核心方法
                    metadata = buildLifecycleMetadata(beanClass);
                    this.lifecycleMetadataCache.put(beanClass, metadata);
                }
                return metadata;
            }
        }
        return metadata;
    }

    /**
     * 构建bean生命周期元数据信息
     *
     * @param beanClass Bean 类
     * @return bean生命周期元数据信
     */
    private LifecycleMetadata buildLifecycleMetadata(Class<?> beanClass) {
        // 提前校验不符合条件的类
        if (!AnnotationUtils.isCandidateClass(beanClass, this.initAnnotationTypes) &&
                !AnnotationUtils.isCandidateClass(beanClass, this.destroyAnnotationTypes)) {
            return this.emptyLifecycleMetadata;
        }

        List<LifecycleMethod> initMethods = new ArrayList<>(1);
        List<LifecycleMethod> destroyMethods = new ArrayList<>(1);
        Class<?> currentClass = beanClass;

        /* 子类往上遍历父类 检查初始化和销毁方法 */
        do {
            final ArrayList<LifecycleMethod> currInitMethods = new ArrayList<>();
            final ArrayList<LifecycleMethod> currDestroyMethods = new ArrayList<>();

            ReflectionUtils.doWithLocalMethods(currentClass, method -> {
                for (Class<? extends Annotation> initAnnotationType : this.initAnnotationTypes) {
                    if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
                        currInitMethods.add(new LifecycleMethod(method, beanClass));
                    }
                }

                for (Class<? extends Annotation> destroyAnnotationType : this.destroyAnnotationTypes) {
                    if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
                        currDestroyMethods.add(new LifecycleMethod(method, beanClass));
                    }
                }
            });

            // 初始化方法存储的顺序[父类方法,子类方法]
            initMethods.addAll(0, currInitMethods);
            // 销毁方法存储的顺序[子类方法,父类方法]
            destroyMethods.addAll(currDestroyMethods);

            // 继续遍历处理父类
            currentClass = currentClass.getSuperclass();

        } while (currentClass != null && currentClass != Object.class);


        return (initMethods.isEmpty() && destroyMethods.isEmpty() ?
                this.emptyLifecycleMetadata : new LifecycleMetadata(beanClass, initMethods, destroyMethods));
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        log.info("invoke init before method");
        LifecycleMetadata lifecycleMetadata = findLifecycleMetadata(bean.getClass());
        try {
            lifecycleMetadata.invokeInitMethods(bean, beanName);
        } catch (Throwable e) {
            throw new RuntimeException(beanName + ": 调用初始化方法失败", e);
        }
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws Exception {
        log.info("{} : invoke destroy before...", beanName);
        LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
        try {
            metadata.invokeDestroyMethods(bean, beanName);
        } catch (Throwable e) {
            throw new RuntimeException(beanName + ": 调用销毁方法失败", e);
        }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        // 是否存在销毁方法
        return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
    }


    /**
     * 生命周期元数据
     */
    private static class LifecycleMetadata {
        private final Class<?> beanClass;

        private final Collection<LifecycleMethod> initMethods;

        private final Collection<LifecycleMethod> destroyMethods;

        // 检查后不属于外部声明管理的初始化方法，根据方法元数据中的限定名进行了去重
        private volatile Set<LifecycleMethod> checkedInitMethods;

        // 检查后不属于外部声明管理的销毁方法，根据方法元数据中的限定名进行了去重
        private volatile Set<LifecycleMethod> checkedDestroyMethods;


        public LifecycleMetadata(Class<?> beanClass, Collection<LifecycleMethod> initMethods, Collection<LifecycleMethod> destroyMethods) {
            this.beanClass = beanClass;
            this.initMethods = initMethods;
            this.destroyMethods = destroyMethods;
        }

        /**
         * 验证初始化、销毁方法 -整理出不是外部声明管理的方法
         *
         * @param beanDefinition BeanDefinition
         */
        public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
            Set<LifecycleMethod> checkedInitMethods = new HashSet<>(this.initMethods.size());
            for (LifecycleMethod lifecycleMethod : initMethods) {
                String identifier = lifecycleMethod.getIdentifier();
                if (!beanDefinition.isExternallyManagedInitMethods(identifier)) {
                    beanDefinition.registerExternallyManagedInitMethod(identifier);
                    checkedInitMethods.add(lifecycleMethod);
                }
            }

            Set<LifecycleMethod> checkedDestroyMethods = new HashSet<>(this.destroyMethods.size());
            for (LifecycleMethod lifecycleMethod : destroyMethods) {
                String identifier = lifecycleMethod.getIdentifier();
                if (!beanDefinition.isExternallyManagedDestroyMethods(identifier)) {
                    beanDefinition.registerExternallyManagedDestroyMethod(identifier);
                    checkedDestroyMethods.add(lifecycleMethod);
                }
            }

            this.checkedInitMethods = checkedInitMethods;
            this.checkedDestroyMethods = checkedDestroyMethods;
        }


        /**
         * 调用初始化方法
         *
         * @param target   bean实例
         * @param beanName beanName
         */
        public void invokeInitMethods(Object target, String beanName) throws Throwable {
            // 类似给成员变量进行线程快照，长调用链中的稳定取值，避免其他线程修改为null出现错误
            Collection<LifecycleMethod> checkedInitMethods = this.checkedInitMethods;

            Collection<LifecycleMethod> initMethodsToIterate = (checkedInitMethods != null ?
                    checkedInitMethods : this.initMethods);

            if (!initMethodsToIterate.isEmpty()) {
                for (LifecycleMethod initMethod : initMethodsToIterate) {
                    initMethod.invoke(target);
                }
            }

        }

        /**
         * 调用销毁方法
         *
         * @param target   bean实例
         * @param beanName beanName
         * @throws Throwable 反射调用方法可能出现异常
         */
        public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
            Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;

            Collection<LifecycleMethod> destroyMethodsToUse = (checkedDestroyMethods != null ?
                    checkedDestroyMethods : this.destroyMethods);

            if (!destroyMethodsToUse.isEmpty()) {
                for (LifecycleMethod destroyMethod : destroyMethodsToUse) {
                    destroyMethod.invoke(target);
                }
            }
        }

        /**
         * 是否有销毁方法
         *
         * @return true:有销毁方法 false:无销毁方法
         */
        public boolean hasDestroyMethods() {
            Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;

            Collection<LifecycleMethod> destroyMethods = (checkedDestroyMethods != null ?
                    checkedDestroyMethods : this.destroyMethods);

            return !destroyMethods.isEmpty();
        }

    }

    /**
     * 生命周期方法
     */
    private static class LifecycleMethod {
        // 方法
        private final Method method;

        // 方法的限定名称
        private final String identifier;

        public LifecycleMethod(Method method, Class<?> beanClass) {
            if (method.getParameterCount() != 0) {
                throw new IllegalStateException("生命周期注解需要一个无参数的方法：" + method);
            }
            this.method = method;
            // 方法限定名标记，如果是重写父类的方法，只会执行子类方法。
            // 如果父类和子类有同名的私有的销毁方法，标识符会不一样，分别执行[子类,父类]方法
            this.identifier = isPrivateOrNotVisible(method, beanClass) ?
                    ClassUtils.getQualifiedMethodName(method) : method.getName();
        }

        public Method getMethod() {
            return method;
        }

        public String getIdentifier() {
            return identifier;
        }

        /**
         * 检查方法是私用的 或 方法与 Bean 类位于不同包中的类中声明，并且该方法既不是公共的也不是受保护的？
         */
        private static boolean isPrivateOrNotVisible(Method method, Class<?> beanClass) {
            int modifiers = method.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                return true;
            }
            // Method is declared in a class that resides in a different package
            // than the bean class and the method is neither public nor protected?
            // 方法与 Bean 类位于不同包中的类中声明，并且该方法既不是公共的也不是受保护的？
            return (!method.getDeclaringClass().getPackageName().equals(beanClass.getPackageName()) &&
                    !(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)));
        }

        /**
         * 执行方法
         *
         * @param target bean实例
         */
        public void invoke(Object target) throws Throwable {
            ReflectionUtils.makeAccessible(this.method);
            method.invoke(target);
        }
    }

}
