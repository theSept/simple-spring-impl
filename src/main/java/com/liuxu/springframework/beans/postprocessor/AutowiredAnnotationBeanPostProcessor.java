package com.liuxu.springframework.beans.postprocessor;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.annotion.Autowired;
import com.liuxu.springframework.beans.annotion.InjectionMetadata;
import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;
import com.liuxu.springframework.beans.config.DependencyDescriptor;
import com.liuxu.springframework.beans.interfaces.AutowireCapableBeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactoryAware;
import com.liuxu.springframework.beans.interfaces.InstantiationAwareBeanPostProcessor;
import com.liuxu.springframework.beans.interfaces.MergedBeanDefinitionPostProcessor;
import com.liuxu.springframework.utils.AnnotationUtils;
import com.liuxu.springframework.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现AutoWired注解依赖注入
 *
 * @date: 2025-07-02
 * @author: liuxu
 */
public class AutowiredAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, InstantiationAwareBeanPostProcessor, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(AutowiredAnnotationBeanPostProcessor.class);

    /** BeanFactory 用于进行依赖解析 */
    protected AutowireCapableBeanFactory beanFactory;

    /** 处理自动注入的注解类型 */
    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);


    /**
     * 创建 {@code AutowiredAnnotationBeanPostProcessor}
     * 进行处理 {@link Autowired} 注解
     */
    public AutowiredAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(Autowired.class);
    }

    /** 缓存  beanName\ClassName -> 需要注入的字段和参数 */
    private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


    /**
     * 后处理器 -Bean的合并Bean定义后处理
     * 创建实例对象之后、属性赋值之前
     * 用于缓存当前Bean需要注入的字段和方法
     *
     * @param beanDefinition Bean 的合并 Bean 定义
     * @param beanType       托管 bean 实例的实际类型
     * @param beanName       Bean的名称
     */
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        log.info("后处理器 执行 AutowiredAnnotationBeanPostProcessor.postProcessMergedBeanDefinition() 加载自动注入的字段");
        findInjectionMetadata(beanName, beanType, beanDefinition);
    }

    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> beanType, RootBeanDefinition beanDefinition) {
        return findAutowiringMetadata(beanName, beanType);
    }

    /**
     * 查询 Class 自动注入的元数据信息
     *
     * @param beanName Bean的名称
     * @param clazz    托管 bean 实际的类型
     * @return bean自动注入的元数据信息
     */
    private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz) {
        // 缓存key
        String cacheKey = (!beanName.isBlank() ? beanName : clazz.getName());

        // 检查缓存中是否存在，检查是否需要重新刷新元数据信息
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey); // double check
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    // 缓存不存在，构建自动注入的元数据核心
                    metadata = buildAutowiringMetadata(clazz);
                    this.injectionMetadataCache.put(cacheKey, metadata);
                }
            }
        }
        return metadata;

    }

    /**
     * 根据Class构建内部需要自动依赖注入的元数据信息（包含字段、方法参数）
     *
     * @param clazz 需要自动依赖注入的Class
     * @return 需要自动依赖注入的元数据信息
     */
    private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
            return InjectionMetadata.EMPTY;
        }

        Class<?> targetClass = clazz;
        Collection<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        do {

            /* 处理字段 */
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                for (Class<? extends Annotation> aao : this.autowiredAnnotationTypes) {
                    Optional.ofNullable(field.getAnnotation(aao))
                            .ifPresent(annotation -> {
                                // 处理 @Autowired 注解的字段
                                if (annotation instanceof Autowired aw) {
                                    boolean required = aw.required();
                                    elements.add(new AutowiredFieldElement(required, field, true));
                                }
                            });
                }
            });

            /* TODO 后续可兼容方法参数 */

            // 拿到父类，继续进行处理
            targetClass = targetClass.getSuperclass();

        } while (targetClass != null && targetClass != Object.class);

        return InjectionMetadata.forElements(elements, clazz);
    }


    /**
     * 后处理器 -属性注入
     *
     * @param bean     待处理的bean
     * @param beanName beanName
     */
    @Override
    public void postProcessProperties(Object bean, String beanName) {
        log.info("后处理器 执行 postProcessProperties 处理属性, beanName {} 进入注解的自动依赖注入, 实例 {}", beanName, bean);

        // 查询需要自动注入的属性
        InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass());

        // 进行依赖注入
        try {
            metadata.inject(bean, beanName);
        } catch (Throwable e) {
            log.error("beanName {} instance {} ,依赖注入出现异常: {}", beanName, bean, e.getMessage());
            throw new RuntimeException("beanName" + beanName + " 执行依赖注入出现异常", e);
        }
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof DefaultListableBeanFactory lbf) {
            this.beanFactory = lbf;
        }
        log.trace("[AutowiredAnnotationBeanPostProcessor] beanFactory 类型不是 DefaultListableBeanFactory ,错误的类型：{} ", beanFactory.getClass().getName());
    }


    /**
     * 解析指定的缓存方法参数或字段值。
     *
     * @param beanName       Bean Name
     * @param cachedArgument 缓存方法参数或字段值
     * @return 解析后的数据
     */
    private Object resolveCachedArgument(String beanName, Object cachedArgument) {
        if (cachedArgument instanceof DependencyDescriptor descriptor) {
            return this.beanFactory.resolveDependency(descriptor, beanName, null);
        } else {
            return null;
        }
    }


    /**
     * AutoWired 针对注入元数据的字段扩展
     */
    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        /** 当没有匹配的注入实例时，是否必须 */
        protected final boolean required;

        /** 是否缓存值 */
        private volatile boolean isCache;

        /** 缓存的字段值 */
        private volatile Object cachedFieldValue;

        public AutowiredFieldElement(boolean required, Member member, boolean isField) {
            super(member, isField);
            this.required = required;
        }

        /**
         * AutoWired 扩展自己的字段依赖注入细节
         *
         * @param target   目标对象
         * @param beanName Bean Name
         */
        @Override
        protected void inject(Object target, String beanName) throws Throwable {
            Field field = (Field) this.member;
            Object value;
            if (this.isCache) {
                // 解析缓存的字段或者方法参数
                value = resolveCachedArgument(beanName, this.cachedFieldValue);
            } else {
                // 缓存中不存在，进入解析字段值核心方法
                value = resolveFieldValue(field, target, beanName);
            }

            if (value != null) {
                ReflectionUtils.makeAccessible(field);
                field.set(target, value);
            }
        }

        /**
         * 解析字段需要的值，返回所需的依赖对象
         *
         * @param field    字段
         * @param bean     目标对象
         * @param beanName Bean Name
         * @return 所需的依赖对象
         */
        private Object resolveFieldValue(Field field, Object bean, String beanName) {
            DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
            // 记录自动注入的BeanName
            HashSet<String> autowiredBeanNames = new HashSet<>(2);
            Object value;
            try {
                value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames);
            } catch (Exception e) {
                log.error("BeanName {} field {} 依赖解析出现错误 {}", beanName, desc.getFieldName(), e.getMessage());
                throw new RuntimeException(e);
            }

            // 缓存处理
            synchronized (this) {
                if (!this.isCache) {
                    // 解析的结果不是空，字段值是必须的，就将其进行缓存
                    if (value != null || this.required) {
                        Object cachedFieldValue = desc;
                        if (value != null && autowiredBeanNames.size() == 1) {
                            String autowiredBeanName = autowiredBeanNames.iterator().next();
                            if (beanFactory.containsBean(autowiredBeanName)
                                    && beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                                // 创建快捷依赖描述类作为缓存，后续直接根据beanName获取实例
                                cachedFieldValue = new ShortcutDependencyDescriptor(desc, autowiredBeanName);
                            }
                        }
                        this.cachedFieldValue = cachedFieldValue;
                        this.isCache = true;
                    }
                }
            }

            return value;
        }
    }

    /**
     * 创建一个快捷依赖描述符，便于存储缓存
     */
    private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

        // BeanName （缓存的beanName，走捷径方法就直接在容器中获取该名称的bean实例）
        private final String shortcut;

        public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut) {
            super(original);
            this.shortcut = shortcut;
        }

        // 如果缓存的对象是ShortcutDependencyDescriptor，那将会走此方法
        @Override
        public Object resolveShortcut(BeanFactory beanFactory) {
            return beanFactory.getBean(this.shortcut, getDependencyType());
        }
    }

}
