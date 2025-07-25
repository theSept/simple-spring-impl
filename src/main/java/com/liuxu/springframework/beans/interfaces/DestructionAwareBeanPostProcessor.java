package com.liuxu.springframework.beans.interfaces;

/**
 * 感知bean销毁的后处理器
 * - 在 bean 对象销毁之前执行
 *
 * @date: 2025-07-18
 * @author: liuxu
 */
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * 后处理器 -销毁前执行
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @throws Exception 抛出异常
     */
    void postProcessBeforeDestruction(Object bean, String beanName) throws Exception;


    /**
     * 判断是否需要销毁
     *
     * @param bean 对象
     * @return 是否需要销毁
     */
    default boolean requiresDestruction(Object bean) {
        return true;
    }
}
