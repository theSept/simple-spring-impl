package com.liuxu.springframework.aop;

/**
 * 代理的目标对象（接口）
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface TargetSource {

    /**
     * 获取目标对象的类型
     */
    Class<?> getTargetClass();

    /**
     * 是否是固定的的目标对象
     *
     * @return true 表示目标对象是固定的，可以缓存
     */
    boolean isStatic();

    /**
     * 获取目标对象
     *
     * @return
     * @throws Exception
     */
    Object getTarget() throws Exception;


    /**
     * 释放目标对象
     *
     * @param target 目标对象
     * @throws Exception
     */
    void releaseTarget(Object target) throws Exception;


}
