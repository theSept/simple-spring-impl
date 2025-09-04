package com.liuxu.springframework.aop.aspectj.instance;


import com.liuxu.springframework.aop.aspectj.annotation.AspectMetadata;

/**
 * 带有 AspectJ 元数据的实例工厂
 *
 * @date: 2025-08-14
 * @author: liuxu
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

    /**
     * 获取切面元数据
     *
     * @return
     */
    AspectMetadata getAspectMetadata();


    /**
     * 为这个工厂获取创建互斥锁。
     */
    Object getAspectCreationMutex();


}
