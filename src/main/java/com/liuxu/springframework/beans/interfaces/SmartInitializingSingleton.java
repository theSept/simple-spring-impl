package com.liuxu.springframework.beans.interfaces;

/**
 * 扩展接口 安全的执行
 * - "单例初始化完成协调者"
 * -
 *
 * @date: 2025-06-24
 * @author: liuxu
 */
public interface SmartInitializingSingleton {

    /**
     * 用于在预加载单例bean实例后，执行的回调方法。
     * <p>
     * - 所有的非懒加载的单列对象实例化完成后执行
     * - 注意只会在预加载的单例bean创建后执行此方法，请不要在懒加载或非单例类中依赖此方法
     */
    void afterSingletonsInstantiated();

}
