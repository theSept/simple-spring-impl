package com.liuxu.springframework.beans.annotion;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;

/**
 * 注入时的元数据类
 * 包含：需要注入的字段信息
 *
 * @date: 2025-07-02
 * @author: liuxu
 */
public class InjectionMetadata {

    /**
     * 元数据信息所属的类
     */
    private final Class<?> targetClass;

    /**
     * 需要注入的元素列表
     */
    private final Collection<InjectedElement> injectedElements;


    public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> injectedElements) {
        this.targetClass = targetClass;
        this.injectedElements = injectedElements;
    }

    /** 空对象 */
    public static final InjectionMetadata EMPTY = new InjectionMetadata(Object.class, Collections.emptyList());

    /** 创建注入元数据类实例 */
    public static InjectionMetadata forElements(Collection<InjectedElement> elements, Class<?> clazz) {
        return (elements.isEmpty() ? new InjectionMetadata(clazz, Collections.emptyList()) :
                new InjectionMetadata(clazz, elements));

    }


    /**
     * 检查是否需要刷新给定的注入元数据。
     * 当类型不匹配说明需要刷新注入元数据
     *
     * @param clazz 要检查的类
     * @return true 指示刷新， false 不检查
     */
    public boolean needsRefresh(Class<?> clazz) {
        return (this.targetClass != clazz);
    }

    /**
     * 检查是否需要刷新给定的注入元数据
     *
     * @param metadata 要检查的注入元数据
     * @param clazz    要检查的类
     * @return true 指示刷新， false 不检查
     */
    public static boolean needsRefresh(InjectionMetadata metadata, Class<?> clazz) {
        return (metadata == null || metadata.needsRefresh(clazz));
    }


    /**
     * 给目标对象按元数据进行依赖注入
     *
     * @param target   Bean 对象
     * @param beanName Bean 对象 Name
     */
    public void inject(Object target, String beanName) throws Throwable {

        for (InjectedElement element : this.injectedElements) {
            element.inject(target, beanName);
        }

    }


    /**
     * 每个注入的元素信息类
     */
    public static class InjectedElement {

        /** （类的）成员 */
        protected final Member member;

        /** 成员是否是字段 */
        protected final boolean isField;


        public InjectedElement(Member member, boolean isField) {
            this.member = member;
            this.isField = isField;
        }

        /**
         * 给目标 对象的成员注入依赖
         *
         * @param target   目标对象
         * @param beanName Bean Name
         */
        protected void inject(Object target, String beanName) throws Throwable {
            // 留给子类实现去，每个解析依赖注入的后处理器都应当扩展注入的字段及方法，进行注入时也应当自行扩展注入
        }
    }

}
